package com.eva.ai.presentation

import androidx.compose.runtime.mutableStateMapOf

enum class BillingOperation { Checkout, Refresh, Restore, Verify, Cancel }

internal class BillingOperationTracker {
    private val operations = mutableStateMapOf<Long, BillingOperation>()
    private var nextTicket = 0L
    val busy: Boolean get() = operations.isNotEmpty()
    fun busy(operation: BillingOperation): Boolean = operations.containsValue(operation)
    fun begin(operation: BillingOperation): Long {
        val ticket = ++nextTicket
        operations[ticket] = operation
        return ticket
    }
    fun end(ticket: Long) { operations.remove(ticket) }
}

suspend fun <T> EvaAppController.withBillingOperation(operation: BillingOperation, block: suspend () -> T): T {
    val ticket = beginBillingOperation(operation)
    try {
        return block()
    } finally {
        endBillingOperation(ticket)
    }
}
