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

    @Test fun challengesRemainUnpaddedAndUrlSafe() {
        val verifiers = listOf("a".repeat(43), "-".repeat(128), "_".repeat(64))
        verifiers.forEach { verifier ->
            val expected = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
            assertEquals(expected, OAuthPkce.challenge(verifier))
            assertTrue(OAuthPkce.challenge(verifier).matches(Regex("[A-Za-z0-9_-]{43}")))
        }
    }
}
