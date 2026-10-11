package com.eva.ai.presentation

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

internal fun birthdayIso(year: Int, month: Int, day: Int, today: Calendar = Calendar.getInstance()): String? {
    if (year < 1900 || month !in 1..12 || day !in 1..31) return null
    val birthday = GregorianCalendar(year, month - 1, day).apply { isLenient = false }
    if (runCatching { birthday.timeInMillis }.isFailure) return null
    var age = today.get(Calendar.YEAR) - year
    if (today.get(Calendar.MONTH) + 1 < month || (today.get(Calendar.MONTH) + 1 == month && today.get(Calendar.DAY_OF_MONTH) < day)) age--
    if (age < 18) return null
    return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)
}

internal fun birthdayDays(year: Int, month: Int): Int = GregorianCalendar(year, month - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH)
