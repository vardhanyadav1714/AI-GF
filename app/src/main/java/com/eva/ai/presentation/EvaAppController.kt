package com.eva.ai.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eva.ai.data.remote.ApiException
import com.eva.ai.data.remote.MeriGfApi
import com.eva.ai.data.settings.EvaSettingsStore
import com.eva.ai.domain.logic.cleanMessage
import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.ChatMessage
import com.eva.ai.domain.model.CompanionProfile
import com.eva.ai.domain.model.ConversationPreview
import com.eva.ai.domain.model.EvaTab
import com.eva.ai.domain.model.SubscriptionState
import java.net.HttpURLConnection
import javax.inject.Inject

/**
 * Single source of truth for app-wide UI state. Feature logic lives in the
 * per-feature extension files next to this class (Chat, Auth, Billing,
 * Profile) so each concern stays in its own file without splitting state.
 */
class EvaAppController @Inject constructor(
    internal val api: MeriGfApi,
    internal val settingsStore: EvaSettingsStore
) {
    // ── Global UI state ──
    var authState by mutableStateOf<AuthState>(AuthState.Loading)
    var authBusy by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)
    var activeTab by mutableStateOf(EvaTab.Home)
    var premiumOpen by mutableStateOf(false)

    /** null = follow the system theme; true/false = explicit user override. */
    var lightMode by mutableStateOf(settingsStore.lightModePref())

    // ── Chat state ──
    var draft by mutableStateOf("")
    var sending by mutableStateOf(false)
    var backendLive by mutableStateOf(false)
    var selectedConversationId by mutableStateOf<String?>(null)
    var chatsLoading by mutableStateOf(false)

    // ── Billing state ──
    var subscriptionState by mutableStateOf<SubscriptionState?>(null)
    var billingAdministrativeArea by mutableStateOf<String?>(null)
    var razorpayBillingStateRequired by mutableStateOf(false)
    private val billingOperations = BillingOperationTracker()
    val subscriptionBusy: Boolean get() = billingOperations.busy
    fun billingBusy(operation: BillingOperation): Boolean = billingOperations.busy(operation)
    internal fun beginBillingOperation(operation: BillingOperation): Long = billingOperations.begin(operation)
    internal fun endBillingOperation(ticket: Long) { billingOperations.end(ticket) }

    /** null = unknown or unlimited (premium). */
    var freeMessagesRemaining by mutableStateOf<Int?>(null)

    // ── Onboarding / profile state ──
    var needsDateOfBirth by mutableStateOf(false)
    var selectedCompanion by mutableStateOf(settingsStore.selectedCompanion())
    var selectedReplyStyle by mutableStateOf(settingsStore.selectedReplyStyle())
    var pendingConversationId by mutableStateOf<String?>(null)

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
                needsDateOfBirth = user.dateOfBirth == null
                loadChats()
                refreshSubscription(silent = true)
                syncDeviceToken()
                pendingConversationId?.let { requested ->
                    pendingConversationId = null
                    openConversation(requested)
                }
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

    internal fun applyUsage(remaining: Int?) {
        freeMessagesRemaining = remaining
        subscriptionState = subscriptionState?.let { state ->
            state.copy(freeRemaining = remaining, freeUsed = remaining?.let { state.freeLimit - it } ?: state.freeUsed)
        }
        if (remaining == 0 && subscriptionState?.active != true) {
            notice = "That was your last free message. Your reply is ready; choose Premium to keep chatting."
        }
    }

    internal fun welcomeNotice(name: String): String {
        val premium = subscriptionState?.active == true
        val remaining = freeMessagesRemaining
        return when {
            premium -> "Signed in as $name. Premium is active."
            remaining != null && remaining > 0 ->
                "Signed in as $name. $remaining free message${if (remaining == 1) "" else "s"} left."
            remaining != null && remaining <= 0 ->
                "Signed in as $name. Free messages are over — upgrade to Premium."
            else -> "Signed in as $name."
        }
    }

    internal fun handleMessageLimitReached(userIndex: Int, placeholderIndex: Int) {
        if (placeholderIndex in messages.indices) messages.removeAt(placeholderIndex)
        if (userIndex in messages.indices) messages.removeAt(userIndex)
        backendLive = true
        freeMessagesRemaining = 0
        notice = "Your 10 free messages are over. Upgrade to Eva Premium for unlimited chats."
        premiumOpen = true
    }

    internal fun shouldOpenPaywallBeforeSend(): Boolean {
        val premium = subscriptionState?.active == true
        if (!freeTrialExhausted(premium, freeMessagesRemaining)) return false
        notice = "Your 10 free messages are over. Upgrade to Eva Premium for unlimited chats."
        premiumOpen = true
        activeTab = EvaTab.Chat
        return true
    }

    internal fun seedLocalMessages() {
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

    internal fun persistSelectedCompanion() {
        settingsStore.saveSelectedCompanion(selectedCompanion)
    }

    fun applyLightMode(enabled: Boolean) {
        lightMode = enabled
        settingsStore.saveLightMode(enabled)
    }

    fun clearNotice() {
        notice = null
    }
}
