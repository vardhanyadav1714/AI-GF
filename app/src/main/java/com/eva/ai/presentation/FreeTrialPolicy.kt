package com.eva.ai.presentation

internal fun shouldShowTrialNotice(seen: Boolean, premium: Boolean, remaining: Int?): Boolean =
    !seen && !premium && remaining != null && remaining > 0

internal fun freeTrialExhausted(premium: Boolean, remaining: Int?): Boolean =
    !premium && remaining != null && remaining <= 0
