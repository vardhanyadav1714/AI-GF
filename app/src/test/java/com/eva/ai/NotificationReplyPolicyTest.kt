package com.eva.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationReplyPolicyTest {
    @Test fun trimsReply() { assertEquals("hello", notificationReplyText("  hello  ")) }
    @Test fun rejectsEmptyReply() { assertNull(notificationReplyText(null)); assertNull(notificationReplyText(" \n ")) }
    @Test fun rejectsTooLongReply() { assertNull(notificationReplyText("a".repeat(4001))) }
    @Test fun enforcesWorkDataByteBudget() {
        assertNull(notificationReplyText("\uD83D\uDE0A".repeat(1100)))
        assertNull(notificationReplyText("\u0905".repeat(3000)))
        assertEquals("\u0905".repeat(1000), notificationReplyText("\u0905".repeat(1000)))
    }
}
