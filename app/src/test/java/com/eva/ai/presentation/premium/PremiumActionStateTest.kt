package com.eva.ai.presentation.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PremiumActionStateTest {
    @Test fun checkoutLabelsDependOnPaymentChoiceNotOtherOperations() {
        assertEquals("Choose payment method", premiumCheckoutLabel(true))
        assertEquals("Subscribe with Google Play", premiumCheckoutLabel(false))
    }

    @Test fun idleHasNoProgressMessage() {
        assertNull(premiumOperationStatus(false, false, false, false, false))
    }

    @Test fun restoreKeepsItsOwnProgressWhenVerificationAlsoRuns() {
        assertEquals("Restoring your purchases...", premiumOperationStatus(false, false, true, true, false))
    }

    @Test fun progressIdentifiesTheActionWithoutRelabellingCheckout() {
        assertEquals("Checking your membership...", premiumOperationStatus(false, true, false, false, false))
        assertEquals("Confirming your payment...", premiumOperationStatus(false, false, false, true, false))
        assertEquals("Opening secure checkout...", premiumOperationStatus(true, false, false, false, false))
        assertEquals("Cancelling future renewals...", premiumOperationStatus(false, false, false, false, true))
    }
}
