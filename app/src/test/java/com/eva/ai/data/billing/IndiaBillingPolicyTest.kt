package com.eva.ai.data.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class IndiaBillingPolicyTest {
    @Test fun onlyIndiaCanUseAlternativeBilling() {
        assertTrue(IndiaBillingPolicy.offerUserChoice(true, "IN"))
        for (country in listOf("US", "GB", "AU", "DE", "JP", "", null)) {
            assertFalse(IndiaBillingPolicy.offerUserChoice(true, country))
        }
        assertFalse(IndiaBillingPolicy.offerUserChoice(false, "IN"))
    }

    @Test fun statesAreUniqueAndIncludeAllCurrentSubdivisions() {
        assertEquals(36, IndiaBillingPolicy.administrativeAreas.size)
        assertEquals(36, IndiaBillingPolicy.administrativeAreas.distinct().size)
        assertTrue(IndiaBillingPolicy.administrativeAreas.contains("UTTAR PRADESH"))
        assertTrue(IndiaBillingPolicy.administrativeAreas.contains("LADAKH"))
    }
}
