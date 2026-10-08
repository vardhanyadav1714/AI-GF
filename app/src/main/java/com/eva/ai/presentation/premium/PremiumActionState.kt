package com.eva.ai.presentation.premium

internal fun premiumCheckoutLabel(userChoice: Boolean): String = when {
    userChoice -> "Choose payment method"
    else -> "Subscribe with Google Play"
}

internal fun premiumOperationStatus(
    checkout: Boolean, refresh: Boolean, restore: Boolean, verify: Boolean, cancel: Boolean
): String? = when {
    restore -> "Restoring your purchases..."
    cancel -> "Cancelling future renewals..."
    verify -> "Confirming your payment..."
    checkout -> "Opening secure checkout..."
    refresh -> "Checking your membership..."
    else -> null
}
