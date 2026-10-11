package com.eva.ai.presentation.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRequestGateTest {
    @Test fun latestPlaybackRequestWins() {
        val gate = PlaybackRequestGate()
        val first = gate.next()
        val second = gate.next()
        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(second))
    }

    @Test fun stoppingInvalidatesPendingDownload() {
        val gate = PlaybackRequestGate()
        val download = gate.next()
        gate.next()
        assertFalse(gate.isCurrent(download))
    }
}
