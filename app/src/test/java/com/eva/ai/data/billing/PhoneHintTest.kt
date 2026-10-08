package com.eva.ai.data.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneHintTest {
    @Test fun formatsAnIndianNumberAndPreservesInternationalCountryCodes() {
        assertEquals("+919876543210", normalizeBillingPhone("98765 43210"))
        assertEquals("+919876543210", normalizeBillingPhone("+91 (98765) 43210"))
        assertEquals("+14155552671", normalizeBillingPhone("+1 415-555-2671"))
    }

    @Test fun absentAndInvalidHintsUseManualCheckoutEntry() {
        for (value in listOf(null, "", "0000000000", "123", "not-a-phone", "+0000000000")) {
            assertNull(normalizeBillingPhone(value))
        }
    }
}
