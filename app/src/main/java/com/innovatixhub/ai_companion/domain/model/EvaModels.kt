package com.eva.ai.domain.model

import java.util.UUID

enum class EvaTab { Home, Chat, Profile }

enum class AuthMode { Login, Signup }

enum class MessageKind { Text, Voice }

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: EvaUser) : AuthState
}

data class EvaUser(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val preferredName: String? = null,
    val occupation: String? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromUser: Boolean,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val kind: MessageKind = MessageKind.Text,
    val streaming: Boolean = false,
    val voiceSeconds: Int = 0,
    val audioBase64: String = "",
    val audioMimeType: String = ""
)

data class ConversationPreview(
    val id: String,
    val title: String,
    val companionId: String,
    val lastMessageAt: String
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val user: EvaUser
)

data class SendMessageResult(
    val conversationId: String,
    val assistantText: String
)

data class VoiceSendResult(
    val conversationId: String,
    val transcript: String,
    val assistantText: String,
    val audioBase64: String,
    val audioMimeType: String
)

data class SubscriptionPlan(
    val planId: String,
    val name: String,
    val amount: Int,
    val formattedAmount: String,
    val currency: String,
    val interval: String
)

data class SubscriptionState(
    val active: Boolean,
    val status: String,
    val providerSubscriptionId: String,
    val checkoutUrl: String,
    val plan: SubscriptionPlan,
    val currentEnd: String?,
    val freeUsed: Int = 0,
    val freeLimit: Int = 10,
    val freeRemaining: Int? = null
)

data class SubscriptionCheckout(
    val subscription: SubscriptionState,
    val checkoutUrl: String
)

data class VoiceRecordingPreview(
    val audioBytes: ByteArray,
    val mimeType: String,
    val durationSeconds: Int
) {
    override fun equals(other: Any?): Boolean =
        other is VoiceRecordingPreview &&
            audioBytes.contentEquals(other.audioBytes) &&
            mimeType == other.mimeType &&
            durationSeconds == other.durationSeconds

    override fun hashCode(): Int {
        var result = audioBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + durationSeconds
        return result
    }
}

data class CompanionProfile(
    val id: String,
    val name: String,
    val subtitle: String,
    val bornIn: String,
    val age: String,
    val personality: String,
    val voiceStyle: String,
    val interests: String,
    val homeLine: String,
    val openingLine: String,
    val about: String,
    val tags: List<String>,
    val imageRes: Int
)

data class ReplyStyle(
    val id: String,
    val label: String,
    val subtitle: String,
    val instruction: String
)

