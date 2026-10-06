package com.eva.ai.domain.logic

fun shouldSuppressChatNotification(
    foreground: Boolean,
    activeConversationId: String?,
    incomingConversationId: String,
    hasObservers: Boolean
): Boolean = foreground && hasObservers && activeConversationId == incomingConversationId
