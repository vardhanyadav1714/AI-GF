package com.eva.ai.presentation.chat

internal class PlaybackRequestGate {
    private var current = 0L
    fun next(): Long = ++current
    fun isCurrent(request: Long): Boolean = request == current
}
