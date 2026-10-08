package com.eva.ai.data.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCheckoutOwnerTest {
    @Test fun playCallbackCannotEraseTheAlternativeChoiceOwner() {
        val checkout = BillingCheckoutOwner()
        checkout.begin("eva-user", true)
        checkout.onPlayUpdate()
        assertTrue(checkout.consumeAlternative("eva-user"))
    }

    @Test fun alternativeCallbackCanArriveBeforePlayCallback() {
        val checkout = BillingCheckoutOwner()
        checkout.begin("eva-user", true)
        assertTrue(checkout.consumeAlternative("eva-user"))
        checkout.onPlayUpdate()
        assertFalse(checkout.consumeAlternative("eva-user"))
    }

    @Test fun genuineAccountChangesAndSignOutRemainBlocked() {
        for (current in listOf("different-user", null)) {
            val checkout = BillingCheckoutOwner()
            checkout.begin("eva-user", true)
            checkout.onPlayUpdate()
            assertFalse(checkout.consumeAlternative(current))
        }
    }

    @Test fun duplicateCallbacksAreRejected() {
        val checkout = BillingCheckoutOwner()
        checkout.begin("eva-user", true)
        assertTrue(checkout.consumeAlternative("eva-user"))
        assertFalse(checkout.consumeAlternative("eva-user"))
    }

    @Test fun standardCheckoutCannotStartAlternativeBilling() {
        val checkout = BillingCheckoutOwner()
        checkout.begin("eva-user", false)
        checkout.onPlayUpdate()
        assertFalse(checkout.consumeAlternative("eva-user"))
    }

    @Test fun aNewCheckoutReplacesTheOldOwner() {
        val checkout = BillingCheckoutOwner()
        checkout.begin("old-user", true)
        checkout.onPlayUpdate()
        checkout.begin("new-user", true)
        assertTrue(checkout.consumeAlternative("new-user"))
    }

    @Test fun closingOrFailingCheckoutClearsTheOwner() {
        val checkout = BillingCheckoutOwner()
        checkout.begin("eva-user", true)
        checkout.clear()
        assertFalse(checkout.consumeAlternative("eva-user"))
    }
}
