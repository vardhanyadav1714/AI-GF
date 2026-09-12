package com.eva.ai.presentation

import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eva.ai.data.remote.ApiException
import com.eva.ai.data.remote.MeriGfApi
import com.eva.ai.data.settings.EvaSettingsStore
import com.eva.ai.domain.logic.cleanMessage
import com.eva.ai.domain.logic.estimateSpeechSeconds
import com.eva.ai.domain.logic.localEvaReply
import com.eva.ai.domain.model.AuthSession
import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.ChatMessage
import com.eva.ai.domain.model.CompanionProfile
import com.eva.ai.domain.model.ConversationPreview
import com.eva.ai.domain.model.EvaTab
import com.eva.ai.domain.model.MessageKind
import com.eva.ai.domain.model.ReplyStyle
import com.eva.ai.domain.model.SubscriptionState
import com.eva.ai.domain.model.VoiceSendResult
import com.eva.ai.domain.model.companionById
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.util.Locale
import kotlin.math.max
import javax.inject.Inject

class EvaAppController @Inject constructor(
    private val api: MeriGfApi,
    private val settingsStore: EvaSettingsStore
) {
    var authState by mutableStateOf<AuthState>(AuthState.Loading)
    var authBusy by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)
    var activeTab by mutableStateOf(EvaTab.Home)
    var premiumOpen by mutableStateOf(false)
    var callOpen by mutableStateOf(false)
    var lightMode by mutableStateOf(false)
    var relationship by mutableStateOf("Sweetheart")
    var draft by mutableStateOf("")
    var sending by mutableStateOf(false)
    var backendLive by mutableStateOf(false)
    var selectedConversationId by mutableStateOf<String?>(null)
    var chatsLoading by mutableStateOf(false)
    var subscriptionState by mutableStateOf<SubscriptionState?>(null)
    var subscriptionBusy by mutableStateOf(false)
    var selectedCompanion by mutableStateOf(settingsStore.selectedCompanion())
    var selectedReplyStyle by mutableStateOf(settingsStore.selectedReplyStyle())

    val messages = mutableStateListOf<ChatMessage>()
    val conversations = mutableStateListOf<ConversationPreview>()

    suspend fun bootstrap() {
        authState = AuthState.Loading
        if (!api.hasSavedSession()) {
            authState = AuthState.SignedOut
            return
        }

        authBusy = true
        runCatching { api.me() }
            .onSuccess { user ->
                authState = AuthState.SignedIn(user)
                loadChats()
                refreshSubscription(silent = true)
            }
            .onFailure { error ->
                val cachedUser = api.savedUser()
                if (error is ApiException && error.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    api.clearSession()
                    authState = AuthState.SignedOut
                } else if (cachedUser != null) {
                    authState = AuthState.SignedIn(cachedUser)
                    backendLive = false
                    notice = "Could not reach the backend. Keeping you signed in."
                    if (messages.isEmpty()) seedLocalMessages()
                } else {
                    authState = AuthState.SignedOut
                }
            }
        authBusy = false
    }

    suspend fun refreshSubscription(silent: Boolean = false): Boolean {
        if (authState !is AuthState.SignedIn) return false
        if (!silent) subscriptionBusy = true
        val refreshed = runCatching {
            api.subscriptionStatus(sync = !silent)
        }.onSuccess { state ->
            subscriptionState = state
        }.onFailure { error ->
            if (!silent) notice = error.cleanMessage("Could not refresh subscription.")
        }.isSuccess
        if (!silent) subscriptionBusy = false
        return refreshed
    }

    suspend fun startPremiumSubscription(): String? {
        if (subscriptionBusy) return null
        if (authState !is AuthState.SignedIn) {
            notice = "Sign in before starting premium."
            return null
        }
        subscriptionBusy = true
        var checkoutUrl: String? = null
        runCatching {
            api.startSubscription()
        }.onSuccess { checkout ->
            subscriptionState = checkout.subscription
            checkoutUrl = checkout.checkoutUrl.ifBlank { checkout.subscription.checkoutUrl }
            if (checkout.subscription.active) {
                notice = "Premium is already active."
            } else if (checkoutUrl.isNullOrBlank()) {
                notice = "Razorpay checkout link was not returned."
            }
        }.onFailure { error ->
            notice = error.cleanMessage("Could not start subscription.")
        }
        subscriptionBusy = false
        return checkoutUrl
    }

    suspend fun requestEmailCode(name: String, email: String): Boolean {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            notice = "Enter a valid email first."
            return false
        }

        authBusy = true
        val sent = runCatching {
            api.requestEmailCode(name = name.trim(), email = cleanEmail)
        }.onSuccess {
            notice = "Confirmation code sent to $cleanEmail."
        }.onFailure { error ->
            notice = error.cleanMessage("Could not send the email code.")
        }.isSuccess
        authBusy = false
        return sent
    }

    suspend fun verifyEmailCode(email: String, code: String) {
        val cleanCode = code.trim()
        if (cleanCode.length < 4) {
            notice = "Enter the confirmation code."
            return
        }

        authBusy = true
        runCatching {
            api.verifyEmailCode(email = email.trim(), code = cleanCode)
        }.onSuccess { session ->
            authState = AuthState.SignedIn(session.user)
            notice = "Welcome back, ${session.user.name}."
            loadChats()
            refreshSubscription(silent = true)
        }.onFailure { error ->
            notice = error.cleanMessage("That code could not be verified.")
        }
        authBusy = false
    }

    suspend fun signInWithGoogle(idToken: String) {
        runCatching {
            api.signInWithGoogle(idToken)
        }.onSuccess { session ->
            authState = AuthState.SignedIn(session.user)
            notice = "Signed in as ${session.user.name}."
            loadChats()
            refreshSubscription(silent = true)
        }.onFailure { error ->
            notice = error.cleanMessage("Google sign-in failed.")
        }
    }

    fun googleSignInUri(): Uri = api.googleSignInUri()

    suspend fun acceptAuthRedirect(uri: Uri) {
        authBusy = true
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            notice = error
            authBusy = false
            return
        }

        runCatching {
            val accessToken = uri.getQueryParameter("accessToken")
            val refreshToken = uri.getQueryParameter("refreshToken")
            val code = uri.getQueryParameter("code")
            when {
                !accessToken.isNullOrBlank() -> api.saveRedirectSession(accessToken, refreshToken)
                !code.isNullOrBlank() -> api.exchangeGoogleCode(code)
                else -> throw ApiException("Google sign-in did not return a session.")
            }
        }.onSuccess { session ->
            authState = AuthState.SignedIn(session.user)
            notice = "Signed in as ${session.user.name}."
            loadChats()
            refreshSubscription(silent = true)
        }.onFailure { redirectError ->
            notice = redirectError.cleanMessage("Google sign-in could not be completed.")
        }
        authBusy = false
    }

    fun signOut() {
        api.clearSession()
        authState = AuthState.SignedOut
        messages.clear()
        conversations.clear()
        subscriptionState = null
        selectedConversationId = null
        activeTab = EvaTab.Home
        premiumOpen = false
        callOpen = false
        backendLive = false
    }

    suspend fun loadChats() {
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

    fun selectCompanion(profile: CompanionProfile) {
        if (selectedCompanion.id == profile.id) return
        selectedCompanion = profile
        persistSelectedCompanion()
        selectedConversationId = null
        messages.clear()
        seedLocalMessages()
        notice = "Now chatting with ${profile.name}."
    }

    fun selectReplyStyle(style: ReplyStyle) {
        if (selectedReplyStyle.id == style.id) return
        selectedReplyStyle = style
        settingsStore.saveSelectedReplyStyle(style)
        notice = "${selectedCompanion.name} will keep replies ${style.label.lowercase(Locale.getDefault())}."
    }

    suspend fun sendMessage(quickText: String? = null) {
        val cleanText = (quickText ?: draft).trim()
        if (cleanText.isBlank() || sending) return

        activeTab = EvaTab.Chat
        premiumOpen = false
        callOpen = false
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
            messages[placeholderIndex] = messages[placeholderIndex].copy(
                text = result.assistantText.ifBlank {
                    "I am here with you. Tell me a little more?"
                },
                streaming = false
            )
        }.onFailure {
            backendLive = false
            streamLocalReply(cleanText, placeholderIndex)
        }
        sending = false
    }

    suspend fun reportLastAssistantReply() {
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

    suspend fun sendVoiceNote(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        voiceSeconds: Int = max(1, audioBytes.size / 8000),
        stayInCall: Boolean = false
    ): VoiceSendResult? {
        if (audioBytes.isEmpty()) {
            notice = "I could not hear anything. Try again."
            return null
        }
        if (sending) {
            notice = "Wait for ${selectedCompanion.name} to finish replying first."
            return null
        }

        activeTab = EvaTab.Chat
        premiumOpen = false
        if (!stayInCall) callOpen = false
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
                val assistantText = result.assistantText.ifBlank {
                    "I heard you. Tell me a little more?"
                }
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
            backendLive = false
            notice = error.cleanMessage("Voice message could not be sent.")
            streamLocalReply("voice", placeholderIndex)
        }
        sending = false
        return playback
    }

    fun clearNotice() {
        notice = null
    }

    private fun seedLocalMessages() {
        val companion = selectedCompanion
        messages.clear()
        messages.addAll(
            listOf(
                ChatMessage(text = companion.openingLine, fromUser = false),
                ChatMessage(text = "Tell me about your day, ${companion.name}.", fromUser = true),
                ChatMessage(text = companion.homeLine, fromUser = false)
            )
        )
    }

    private suspend fun streamLocalReply(message: String, index: Int) {
        val reply = localEvaReply(message, selectedCompanion, selectedReplyStyle)
        val buffer = StringBuilder()
        reply.split(" ").forEach { word ->
            delay(36)
            buffer.append(word).append(" ")
            if (index in messages.indices) {
                messages[index] = messages[index].copy(text = buffer.toString())
            }
        }
        if (index in messages.indices) {
            messages[index] = messages[index].copy(
                text = buffer.toString().trim(),
                streaming = false
            )
        }
    }

    private fun persistSelectedCompanion() {
        settingsStore.saveSelectedCompanion(selectedCompanion)
    }
}

private fun String.isMongoObjectId(): Boolean =
    length == 24 && all { character ->
        character.isDigit() || character.lowercaseChar() in 'a'..'f'
    }


