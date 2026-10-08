package com.eva.ai.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeTrialPolicyTest {
    @Test fun firstTrialShowsNoticeAndAllowsMessages() {
        assertTrue(shouldShowTrialNotice(false, false, 10))
        assertFalse(freeTrialExhausted(false, 10))
        assertFalse(freeTrialExhausted(false, 1))
    }
    @Test fun acknowledgedNoticeDoesNotRepeat() {
        assertFalse(shouldShowTrialNotice(true, false, 9))
    }
    @Test fun exhaustedTrialBlocksOnlyTheNextSend() {
        assertTrue(freeTrialExhausted(false, 0))
        assertFalse(shouldShowTrialNotice(false, false, 0))
    }
    @Test fun premiumAndDisabledLimitsNeverShowTrialPaywall() {
        assertFalse(freeTrialExhausted(true, 0))
        assertFalse(shouldShowTrialNotice(false, true, 10))
        assertFalse(freeTrialExhausted(false, null))
        assertFalse(shouldShowTrialNotice(false, false, null))
    }
}
