package com.eva.ai.presentation

import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.SubscriptionCheckout
import com.eva.ai.domain.logic.cleanMessage
import kotlinx.coroutines.CancellationException

private inline fun <T> billingResult(block: () -> T): Result<T> = runCatching(block).onFailure {
    if (it is CancellationException) throw it
}

private fun EvaAppController.billingUserId(): String? = (authState as? AuthState.SignedIn)?.user?.id

suspend fun EvaAppController.userChoiceBillingEnabled(): Boolean =
    billingResult { api.userChoiceBillingEnabled() }.getOrDefault(false)

suspend fun EvaAppController.refreshSubscription(silent: Boolean = false): Boolean {
    val owner = billingUserId() ?: return false
    if (!silent && subscriptionBusy) return false
    val ticket = if (!silent) beginBillingOperation(BillingOperation.Refresh) else null
    try {
        return billingResult { api.subscriptionStatus(sync = !silent) }.onSuccess { state ->
            if (billingUserId() != owner) return@onSuccess
            subscriptionState = state
            freeMessagesRemaining = if (state.active) null else state.freeRemaining
        }.onFailure { error ->
            if (!silent) notice = error.cleanMessage("Could not refresh subscription.")
        }.isSuccess && billingUserId() == owner
    } finally { ticket?.let(::endBillingOperation) }
}

suspend fun EvaAppController.verifyGooglePlayPurchase(purchaseToken: String, productId: String): Boolean {
    val owner = billingUserId() ?: return false
    return withBillingOperation(BillingOperation.Verify) {
        var verified = false
        billingResult { api.verifyGooglePlay(purchaseToken, productId) }.onSuccess { state ->
            if (billingUserId() != owner) return@onSuccess
            subscriptionState = state
            freeMessagesRemaining = if (state.active) null else state.freeRemaining
            verified = state.active
            notice = if (state.active) "Premium is active." else "Google Play purchase is not active yet (${state.status})."
        }.onFailure { error -> notice = error.cleanMessage("Google Play purchase could not be verified.") }
        verified
    }
}

suspend fun EvaAppController.cancelPremiumRenewal() {
    val owner = billingUserId() ?: return
    if (subscriptionBusy) return
    withBillingOperation(BillingOperation.Cancel) {
        billingResult { api.cancelSubscription() }.onSuccess { state ->
            if (billingUserId() != owner) return@onSuccess
            subscriptionState = state
            notice = "Future renewals are cancelled. Your current paid period remains available."
        }.onFailure { error -> notice = error.cleanMessage("Could not cancel the renewal. Please try again.") }
    }
}

suspend fun EvaAppController.startPremiumSubscription(
    externalTransactionToken: String? = null, billingCountryCode: String? = null,
    billingAdministrativeArea: String? = null
): SubscriptionCheckout? {
    if (subscriptionBusy) return null
    val owner = billingUserId()
    if (owner == null) {
        notice = "Sign in before starting premium."
        return null
    }
    return withBillingOperation(BillingOperation.Checkout) {
        var result: SubscriptionCheckout? = null
        billingResult { api.startSubscription(externalTransactionToken, billingCountryCode, billingAdministrativeArea) }.onSuccess { checkout ->
            if (billingUserId() != owner) return@onSuccess
            subscriptionState = checkout.subscription
            if (checkout.subscription.active) notice = "Premium is already active." else result = checkout
        }.onFailure { error -> notice = error.cleanMessage("Could not start subscription.") }
        result
    }
}

suspend fun EvaAppController.verifyRazorpayPurchase(paymentId: String, subscriptionId: String, signature: String) {
    val owner = billingUserId() ?: return
    withBillingOperation(BillingOperation.Verify) {
        billingResult { api.verifyRazorpay(paymentId, subscriptionId, signature) }.onSuccess { state ->
            if (billingUserId() != owner) return@onSuccess
            subscriptionState = state
            freeMessagesRemaining = if (state.active) null else state.freeRemaining
            notice = if (state.active) "Premium is active." else "Payment verification is pending. Refresh payment status shortly."
        }.onFailure {
            notice = "Payment could not be confirmed yet. Refresh payment status before trying another payment."
        }
    }
}
