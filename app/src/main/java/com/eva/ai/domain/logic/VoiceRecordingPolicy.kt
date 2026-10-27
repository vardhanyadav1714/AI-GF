package com.eva.ai.domain.logic

object VoiceRecordingPolicy {
    const val DEFAULT_MAX_SECONDS = 60

    fun maxSeconds(serverLimit: Int?): Int =
        serverLimit?.takeIf { it > 0 }?.coerceAtMost(120) ?: DEFAULT_MAX_SECONDS

    fun shouldStop(elapsedMillis: Long, maxSeconds: Int): Boolean =
        elapsedMillis >= maxSeconds * 1000L

    fun canSend(durationSeconds: Int, maxSeconds: Int): Boolean =
        durationSeconds in 1..maxSeconds
}
