package com.eva.ai.presentation

import org.junit.Assert.*
import org.junit.Test
import java.util.GregorianCalendar

class BirthdayPolicyTest {
    private val today = GregorianCalendar(2026, 9, 9)
    @Test fun allowsExactlyEighteenButNotTomorrowBirthday() {
        assertEquals("2008-10-09", birthdayIso(2008, 10, 9, today))
        assertNull(birthdayIso(2008, 10, 10, today))
    }
    @Test fun validatesLeapYearsAndMonthLengths() {
        assertEquals("2000-02-29", birthdayIso(2000, 2, 29, today))
        assertNull(birthdayIso(2001, 2, 29, today))
        assertNull(birthdayIso(2000, 4, 31, today))
        assertEquals(29, birthdayDays(2000, 2))
        assertEquals(28, birthdayDays(1900, 2))
    }
    @Test fun rejectsOutOfRangeAndKeepsPaddedIsoFormat() {
        assertNull(birthdayIso(1899, 1, 1, today))
        assertNull(birthdayIso(2000, 13, 1, today))
        assertEquals("2000-01-05", birthdayIso(2000, 1, 5, today))
    }
}
