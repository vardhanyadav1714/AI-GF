package com.eva.ai.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthPkceTest {
    @Test fun matchesRfc7636Example() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            OAuthPkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }

    @Test fun generatesUniqueUrlSafeVerifiers() {
        val values = List(50) { OAuthPkce.newVerifier() }
        assertEquals(50, values.toSet().size)
        assertTrue(values.all { it.matches(Regex("[A-Za-z0-9_-]{43}")) })
    }
}
