package com.eva.ai.presentation.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceMessagePlayerTest {
    @Test fun formatsPlaybackTime() {
        assertEquals("0:00", audioTime(0))
        assertEquals("0:09", audioTime(9))
        assertEquals("1:05", audioTime(65))
        assertEquals("60:00", audioTime(3600))
    }

    @Test fun clampsInvalidDuration() {
        assertEquals("0:00", audioTime(-1))
    }
}
