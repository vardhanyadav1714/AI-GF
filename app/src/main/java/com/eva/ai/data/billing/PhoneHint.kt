package com.eva.ai.data.billing

internal fun normalizeBillingPhone(value: String?): String? {
    val phone = value?.replace(Regex("[\\s()-]"), "") ?: return null
    return when {
        phone.matches(Regex("\\+[1-9][0-9]{7,14}")) -> phone
        phone.matches(Regex("[6-9][0-9]{9}")) -> "+91$phone"
        else -> null
    }
}
