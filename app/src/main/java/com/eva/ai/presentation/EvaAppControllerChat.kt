package com.eva.ai.presentation

import android.util.Base64
import com.eva.ai.data.remote.ApiException
import com.eva.ai.domain.logic.cleanMessage
import com.eva.ai.domain.logic.estimateSpeechSeconds
import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.ChatMessage
import com.eva.ai.domain.model.CompanionProfile
import com.eva.ai.domain.model.EvaTab
import com.eva.ai.domain.model.MessageKind
import com.eva.ai.domain.model.VoiceSendResult
import com.eva.ai.domain.model.companionById
import kotlin.math.max

// ── Conversation loading ─────────────────────────────────────────────────────

suspend fun EvaAppController.loadChats() {
    chatsLoading = true
    runCatching {
        val previews = api.conversations()
        conversations.clear()
        conversations.addAll(previews)
        val first = previews.firstOrNull()
        selectedConversationId = first?.id
        if (first != null) {
            selectedCompanion = companionById(first.companionId)
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
        selectedCompanion = companionById(preview.companionId)
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
    if (selectedCompanion.id == profile.id) return
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

    runCatching {
        api.reportAiContent(
            conversationId = selectedConversationId,
            messageId = message.id.takeIf { it.isMongoObjectId() },
            companionId = selectedCompanion.id,
            details = message.text.take(400)
        )
    }.onSuccess {
        notice = "Thanks. This reply has been reported for review."
    }.onFailure { error ->
        notice = error.cleanMessage("Could not report that reply.")
    }
}

suspend fun EvaAppController.sendVoiceNote(
    audioBytes: ByteArray,
    mimeType: String = "audio/mp4",
    voiceSeconds: Int = max(1, audioBytes.size / 8000)
): VoiceSendResult? {
    if (audioBytes.isEmpty()) {
        notice = "I could not hear anything. Try again."
        return null
    }
    if (shouldOpenPaywallBeforeSend()) return null
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
    val liveResult = if (api.savedAccessToken().isNullOrBlank()) {
        Result.failure(IllegalStateException("No signed-in backend session."))
    } else {
        runCatching {
            api.sendVoiceMessage(
                conversationId = selectedConversationId,
                audioBytes = audioBytes,
                mimeType = mimeType,
                companion = selectedCompanion,
                replyStyle = selectedReplyStyle
            )
        }
    }

    var playback: VoiceSendResult? = null
    liveResult.onSuccess { result ->
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
                    voiceSeconds = estimateSpeechSeconds(assistantText),
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
    }.onFailure { error ->
        if (error is ApiException && error.statusCode == 402) {
            handleMessageLimitReached(userIndex, placeholderIndex)
        } else {
            backendLive = false
            notice = error.cleanMessage("Voice message could not be sent.")
            if (placeholderIndex in messages.indices) messages.removeAt(placeholderIndex)
        }
    }
    sending = false
    return playback
}

private fun String.isMongoObjectId(): Boolean =
    length == 24 && all { character ->
        character.isDigit() || character.lowercaseChar() in 'a'..'f'
    }
