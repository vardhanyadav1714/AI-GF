package com.eva.ai.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingOperationTrackerTest {
    @Test fun refreshDoesNotSetCheckoutOrRestoreLoading() {
        val operations = BillingOperationTracker()
        val refresh = operations.begin(BillingOperation.Refresh)
        assertTrue(operations.busy)
        assertTrue(operations.busy(BillingOperation.Refresh))
        assertFalse(operations.busy(BillingOperation.Checkout))
        assertFalse(operations.busy(BillingOperation.Restore))
        operations.end(refresh)
        assertFalse(operations.busy)
    }

    @Test fun finishingVerificationDoesNotEndRestore() {
        val operations = BillingOperationTracker()
        val restore = operations.begin(BillingOperation.Restore)
        val verify = operations.begin(BillingOperation.Verify)
        operations.end(verify)
        assertTrue(operations.busy(BillingOperation.Restore))
        assertFalse(operations.busy(BillingOperation.Verify))
        operations.end(restore)
        assertFalse(operations.busy)
    }

    @Test fun overlappingVerificationRemainsBusyUntilBothFinish() {
        val operations = BillingOperationTracker()
        val first = operations.begin(BillingOperation.Verify)
        val second = operations.begin(BillingOperation.Verify)
        operations.end(first)
        assertTrue(operations.busy(BillingOperation.Verify))
        operations.end(second)
        assertFalse(operations.busy)
    }

    @Test fun lateOrDuplicateCompletionCannotClearANewAction() {
        val operations = BillingOperationTracker()
        val old = operations.begin(BillingOperation.Refresh)
        operations.end(old)
        val checkout = operations.begin(BillingOperation.Checkout)
        operations.end(old)
        assertTrue(operations.busy(BillingOperation.Checkout))
        operations.end(checkout)
        assertFalse(operations.busy)
    }
}
