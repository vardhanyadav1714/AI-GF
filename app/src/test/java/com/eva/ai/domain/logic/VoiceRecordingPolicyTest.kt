package com.eva.ai.domain.logic

import org.junit.Assert.*
import org.junit.Test

class VoiceRecordingPolicyTest {
    @Test fun absentOrInvalidPlanUsesTheServerDefault() {
        assertEquals(60, VoiceRecordingPolicy.maxSeconds(null))
        assertEquals(60, VoiceRecordingPolicy.maxSeconds(0))
        assertEquals(60, VoiceRecordingPolicy.maxSeconds(-1))
    }

    @Test fun recordingStopsAtTheExactLimitAndAfterDelayedUiUpdates() {
        assertFalse(VoiceRecordingPolicy.shouldStop(59_999, 60))
        assertTrue(VoiceRecordingPolicy.shouldStop(60_000, 60))
        assertTrue(VoiceRecordingPolicy.shouldStop(121_000, 60))
    }

    @Test fun emptyAndOversizedPreviewsCannotBeSent() {
        assertFalse(VoiceRecordingPolicy.canSend(0, 60))
        assertFalse(VoiceRecordingPolicy.canSend(-1, 60))
        assertTrue(VoiceRecordingPolicy.canSend(1, 60))
        assertTrue(VoiceRecordingPolicy.canSend(60, 60))
        assertFalse(VoiceRecordingPolicy.canSend(61, 60))
        assertFalse(VoiceRecordingPolicy.canSend(121, 60))
    }

    @Test fun aShorterPlanIsEnforced() {
        assertEquals(30, VoiceRecordingPolicy.maxSeconds(30))
        assertTrue(VoiceRecordingPolicy.shouldStop(30_000, 30))
        assertFalse(VoiceRecordingPolicy.canSend(31, 30))
        assertEquals(120, VoiceRecordingPolicy.maxSeconds(Int.MAX_VALUE))
    }
}
