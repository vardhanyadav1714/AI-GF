package com.eva.ai

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

internal fun notificationReplyText(value: CharSequence?): String? {
    val text = value?.toString()?.trim() ?: return null
    if (text.isBlank() || text.length > 4000) return null
    val encoded = ByteArrayOutputStream().also { output -> DataOutputStream(output).use { it.writeUTF(text) } }.size()
    return text.takeIf { encoded <= 6144 }
}
