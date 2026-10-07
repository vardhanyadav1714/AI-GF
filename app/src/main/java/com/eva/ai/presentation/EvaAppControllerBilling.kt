package com.eva.ai.presentation

import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.SubscriptionCheckout
import com.eva.ai.domain.logic.cleanMessage

suspend fun EvaAppController.userChoiceBillingEnabled(): Boolean = runCatching { api.userChoiceBillingEnabled() }.getOrDefault(false)

suspend fun EvaAppController.refreshSubscription(silent: Boolean = false): Boolean {
    if (authState !is AuthState.SignedIn) return false
    if (!silent) subscriptionBusy = true
    val refreshed = runCatching {
        api.subscriptionStatus(sync = !silent)
    }.onSuccess { state ->
        subscriptionState = state
        freeMessagesRemaining = if (state.active) null else state.freeRemaining
    }.onFailure { error ->
        if (!silent) notice = error.cleanMessage("Could not refresh subscription.")
    }.isSuccess
    if (!silent) subscriptionBusy = false
    return refreshed
}

suspend fun EvaAppController.verifyGooglePlayPurchase(purchaseToken: String, productId: String): Boolean {
    if (authState !is AuthState.SignedIn) return false
    subscriptionBusy = true
    var verified = false
    runCatching {
        api.verifyGooglePlay(purchaseToken, productId)
    }.onSuccess { state ->
        subscriptionState = state
        freeMessagesRemaining = if (state.active) null else state.freeRemaining
        verified = state.active
        notice = if (state.active) {
            "Premium is active. Enjoy unlimited chats!"
        } else {
            "Google Play purchase is not active yet (${state.status})."
        }
    }.onFailure { error ->
        notice = error.cleanMessage("Google Play purchase could not be verified.")
    }
    subscriptionBusy = false
    return verified
}

suspend fun EvaAppController.cancelPremiumRenewal() {
    if (subscriptionBusy || authState !is AuthState.SignedIn) return
    subscriptionBusy = true
    runCatching { api.cancelSubscription() }.onSuccess { state ->
        subscriptionState = state
        notice = "Future renewals are cancelled. Your current paid period remains available."
    }.onFailure { error -> notice = error.cleanMessage("Could not cancel the renewal. Please try again.") }
    subscriptionBusy = false
}

suspend fun EvaAppController.startPremiumSubscription(externalTransactionToken: String? = null, billingCountryCode: String? = null, billingAdministrativeArea: String? = null): SubscriptionCheckout? {
    if (subscriptionBusy) return null
    if (authState !is AuthState.SignedIn) {
        notice = "Sign in before starting premium."
        return null
    }
    subscriptionBusy = true
    var result: SubscriptionCheckout? = null
    runCatching {
        api.startSubscription(externalTransactionToken, billingCountryCode, billingAdministrativeArea)
    }.onSuccess { checkout ->
        subscriptionState = checkout.subscription
        if (checkout.subscription.active) {
            notice = "Premium is already active."
        } else {
            result = checkout
        }
    }.onFailure { error ->
        notice = error.cleanMessage("Could not start subscription.")
    }
    subscriptionBusy = false
    return result
}

suspend fun EvaAppController.verifyRazorpayPurchase(paymentId: String, subscriptionId: String, signature: String) {
    subscriptionBusy = true
    runCatching { api.verifyRazorpay(paymentId, subscriptionId, signature) }.onSuccess { state ->
        subscriptionState = state
        freeMessagesRemaining = if (state.active) null else state.freeRemaining
        notice = if (state.active) "Premium is active." else "Payment verification is pending. Refresh payment status shortly."
    }.onFailure {
        notice = "Payment could not be confirmed yet. Refresh payment status before trying another payment."
    }
    subscriptionBusy = false
}
