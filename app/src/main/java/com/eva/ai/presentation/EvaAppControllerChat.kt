package com.eva.ai.presentation

import android.util.Base64
import com.eva.ai.data.remote.ApiException
import com.eva.ai.domain.logic.cleanMessage
import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.ChatMessage
import com.eva.ai.domain.model.CompanionProfile
import com.eva.ai.domain.model.EvaTab
import com.eva.ai.domain.model.MessageKind
import com.eva.ai.domain.model.VoiceSendResult
import com.eva.ai.domain.model.companionById
import kotlin.math.max
import kotlinx.coroutines.CancellationException

// ── Conversation loading ─────────────────────────────────────────────────────

suspend fun EvaAppController.loadChats() {
    chatsLoading = true
    runCatching {
        runCatching { api.customCompanions() }.onSuccess { records ->
            customCompanions.clear()
            customCompanions.addAll(records)
        }
        val previews = api.conversations()
        conversations.clear()
        conversations.addAll(previews)
        val first = previews.firstOrNull()
        selectedConversationId = first?.id
        if (first != null) {
            selectedCompanion = companionProfile(first.companionId)
            persistSelectedCompanion()
            val loadedMessages = api.messages(first.id)
            messages.clear()
            messages.addAll(loadedMessages)
        }
        backendLive = true
        if (messages.isEmpty()) seedLocalMessages()
    }.onFailure {
        backendLive = false
        if (messages.isEmpty()) seedLocalMessages()
    }
    chatsLoading = false
}

suspend fun EvaAppController.openConversationFromNotification(conversationId: String) {
    pendingConversationId = conversationId
    if (authState is AuthState.Loading) return
    pendingConversationId = null
    openConversation(conversationId)
}

suspend fun EvaAppController.openConversation(conversationId: String) {
    activeTab = EvaTab.Chat
    premiumOpen = false
    conversations.firstOrNull { it.id == conversationId }?.let { preview ->
        selectedCompanion = runCatching { companionProfile(preview.companionId) }.getOrElse {
            notice = "Could not load this companion. Please reload your chats."
            return
        }
        persistSelectedCompanion()
    }
    selectedConversationId = conversationId
    chatsLoading = true
    runCatching {
        val loadedMessages = api.messages(conversationId)
        messages.clear()
        messages.addAll(loadedMessages)
        backendLive = true
        if (messages.isEmpty()) seedLocalMessages()
    }.onFailure {
        backendLive = false
        if (messages.isEmpty()) seedLocalMessages()
    }
    chatsLoading = false
}

suspend fun EvaAppController.reloadCurrentConversation() {
    if (sending) return
    val conversationId = selectedConversationId ?: return loadChats()
    runCatching {
        val loadedMessages = api.messages(conversationId)
        messages.clear()
        messages.addAll(loadedMessages)
        backendLive = true
    }
}

// ── Companion selection ──────────────────────────────────────────────────────

fun EvaAppController.selectCompanion(profile: CompanionProfile) {
    if (selectedCompanion.id == profile.id) {
        selectedCompanion = profile
        persistSelectedCompanion()
        return
    }
    selectedCompanion = profile
    persistSelectedCompanion()
    selectedConversationId = null
    messages.clear()
    seedLocalMessages()
    notice = "Now chatting with ${profile.name}."
}

// ── Sending ──────────────────────────────────────────────────────────────────

suspend fun EvaAppController.sendMessage(quickText: String? = null) {
    val cleanText = (quickText ?: draft).trim()
    if (cleanText.isBlank() || sending) return
    if (shouldOpenPaywallBeforeSend()) return

    activeTab = EvaTab.Chat
    premiumOpen = false
    draft = ""
    sending = true
    messages.add(ChatMessage(text = cleanText, fromUser = true))
    messages.add(ChatMessage(text = "", fromUser = false, streaming = true))

    val placeholderIndex = messages.lastIndex
    val liveResult = if (api.savedAccessToken().isNullOrBlank()) {
        Result.failure(IllegalStateException("No signed-in backend session."))
    } else {
        runCatching {
            api.sendMessage(
                conversationId = selectedConversationId,
                content = cleanText,
                companion = selectedCompanion,
                replyStyle = selectedReplyStyle
            )
        }
    }

    liveResult.onSuccess { result ->
        backendLive = true
        selectedConversationId = result.conversationId
        applyUsage(result.freeRemaining)
        messages[placeholderIndex] = messages[placeholderIndex].copy(
            text = result.assistantText,
            streaming = false
        )
    }.onFailure { error ->
        if (error is ApiException && error.statusCode == 402) {
            if (draft.isBlank()) draft = cleanText
            handleMessageLimitReached(placeholderIndex - 1, placeholderIndex)
        } else {
            backendLive = false
            if (placeholderIndex in messages.indices) messages.removeAt(placeholderIndex)
            if (placeholderIndex - 1 in messages.indices) messages.removeAt(placeholderIndex - 1)
            if (draft.isBlank()) draft = cleanText
            notice = error.cleanMessage("Could not get a reply. Please try again.")
        }
    }
    sending = false
}

suspend fun EvaAppController.reportLastAssistantReply() {
    if (authState !is AuthState.SignedIn) {
        notice = "Sign in before reporting a reply."
        return
    }

    val message = messages.lastOrNull { !it.fromUser && !it.streaming && it.text.isNotBlank() }
    if (message == null) {
        notice = "There is no reply to report yet."
        return
    }

    reportAssistantMessage(message, "offensive_ai_content", "")
}

suspend fun EvaAppController.reportAssistantMessage(message: ChatMessage, reason: String, details: String): Boolean {
    if (authState !is AuthState.SignedIn || message.fromUser || message.streaming || !message.id.isMongoObjectId()) {
        notice = "This reply is not ready to report."
        return false
    }
    return try {
        api.reportAiContent(null, message.id, selectedCompanion.id, reason, details)
        notice = "Thanks. This reply has been reported for review."
        true
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        notice = error.cleanMessage("Could not report that reply.")
        false
    }
}

fun EvaAppController.voiceTrialAllowsRecording(): Boolean {
    if (subscriptionState?.active != true && subscriptionState?.freeVoiceRemaining == 0) {
        notice = "Your free voice note is used. Premium includes 30 minutes of voice input per billing cycle."
        premiumOpen = true
        return false
    }
    return true
}

suspend fun EvaAppController.sendVoiceNote(
    audioBytes: ByteArray,
    mimeType: String = "audio/mp4",
    voiceSeconds: Int = max(1, audioBytes.size / 8000),
    requestId: String = java.util.UUID.randomUUID().toString()
): VoiceSendResult? {
    if (audioBytes.isEmpty()) {
        notice = "I could not hear anything. Try again."
        return null
    }
    if (shouldOpenPaywallBeforeSend()) return null
    if (!voiceTrialAllowsRecording()) return null
    if (sending) {
        notice = "Wait for ${selectedCompanion.name} to finish replying first."
        return null
    }

    activeTab = EvaTab.Chat
    premiumOpen = false
    sending = true
    messages.add(
        ChatMessage(
            text = "Voice note",
            fromUser = true,
            kind = MessageKind.Voice,
            voiceSeconds = voiceSeconds,
            audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP),
            audioMimeType = mimeType
        )
    )
    messages.add(
        ChatMessage(
            text = "Preparing voice reply",
            fromUser = false,
            streaming = true
        )
    )
    val userIndex = messages.lastIndex - 1
    val placeholderIndex = messages.lastIndex
    val voiceUserId = messages[userIndex].id
    val voicePlaceholderId = messages[placeholderIndex].id
    val liveResult = if (api.savedAccessToken().isNullOrBlank()) {
        Result.failure(IllegalStateException("No signed-in backend session."))
    } else {
        runCatching {
            api.sendVoiceMessage(
                conversationId = selectedConversationId,
                audioBytes = audioBytes,
                mimeType = mimeType,
                companion = selectedCompanion,
                replyStyle = selectedReplyStyle,
                requestId = requestId
            )
        }
    }

    var playback: VoiceSendResult? = null
    liveResult.onSuccess { result ->
        val userIndex = messages.indexOfFirst { it.id == voiceUserId }
        val placeholderIndex = messages.indexOfFirst { it.id == voicePlaceholderId }
        if (userIndex < 0 || placeholderIndex < 0) return@onSuccess
        backendLive = true
        selectedConversationId = result.conversationId
        applyUsage(result.freeRemaining)
        if (userIndex in messages.indices) {
            messages[userIndex] = messages[userIndex].copy(
                text = result.transcript.ifBlank { "Voice note" },
                kind = MessageKind.Voice,
                voiceSeconds = voiceSeconds,
                audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP),
                audioMimeType = mimeType
            )
        }
        if (placeholderIndex in messages.indices) {
            val assistantText = result.assistantText
            messages[placeholderIndex] = if (result.audioBase64.isNotBlank()) {
                messages[placeholderIndex].copy(
                    text = assistantText,
                    kind = MessageKind.Voice,
                    voiceSeconds = 0,
                    audioBase64 = result.audioBase64,
                    audioMimeType = result.audioMimeType,
                    streaming = false
                )
            } else {
                messages[placeholderIndex].copy(
                    text = assistantText,
                    streaming = false
                )
            }
        }
        playback = result
        if (result.freeVoiceRemaining != null) subscriptionState = subscriptionState?.copy(freeVoiceRemaining = result.freeVoiceRemaining)
        if (result.speechLimitReached) notice = "Today's spoken-reply allowance is used up. Replies continue as text until midnight UTC (5:30 AM IST)."
    }.onFailure { error ->
        val userIndex = messages.indexOfFirst { it.id == voiceUserId }
        val placeholderIndex = messages.indexOfFirst { it.id == voicePlaceholderId }
        if (userIndex < 0 || placeholderIndex < 0) return@onFailure
        if (error is ApiException && error.statusCode == 402) {
            handleMessageLimitReached(userIndex, placeholderIndex)
        } else {
            backendLive = false
            notice = error.cleanMessage("Voice message could not be sent.")
            if (placeholderIndex in messages.indices) messages.removeAt(placeholderIndex)
            if (userIndex in messages.indices) messages.removeAt(userIndex)
        }
    }
    sending = false
    return playback
}

private fun String.isMongoObjectId(): Boolean =
    length == 24 && all { character ->
        character.isDigit() || character.lowercaseChar() in 'a'..'f'
    }
