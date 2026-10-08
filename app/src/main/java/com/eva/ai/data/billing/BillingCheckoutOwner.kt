package com.eva.ai.data.billing

/** Keeps the checkout owner across Play and alternative-billing callbacks. */
internal class BillingCheckoutOwner {
    private var ownerId: String? = null
    private var awaitingAlternativeChoice = false

    @Synchronized fun begin(userId: String, userChoice: Boolean) {
        ownerId = userId
        awaitingAlternativeChoice = userChoice
    }

    @Synchronized fun onPlayUpdate() {
        // A Play update can arrive before the alternative choice callback.
        // Only that callback (or a new checkout) consumes a choice's owner.
        if (!awaitingAlternativeChoice) ownerId = null
    }

    @Synchronized fun consumeAlternative(currentUserId: String?): Boolean {
        val matches = awaitingAlternativeChoice && ownerId != null && ownerId == currentUserId
        clear()
        return matches
    }

    @Synchronized fun clear() {
        ownerId = null
        awaitingAlternativeChoice = false
    }
}
