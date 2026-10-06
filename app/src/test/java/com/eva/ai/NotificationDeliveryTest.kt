package com.eva.ai

import com.eva.ai.domain.logic.shouldSuppressChatNotification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeliveryTest {
    @Test fun backgroundChatDoesNotSuppressNotification() {
        assertFalse(shouldSuppressChatNotification(false, "chat", "chat", true))
    }
    @Test fun foregroundVisibleChatRefreshesInstead() {
        assertTrue(shouldSuppressChatNotification(true, "chat", "chat", true))
    }
    @Test fun noSubscriberDoesNotDropNotification() {
        assertFalse(shouldSuppressChatNotification(true, "chat", "chat", false))
    }
    @Test fun anotherConversationStillNotifies() {
        assertFalse(shouldSuppressChatNotification(true, "other", "chat", true))
    }
}
