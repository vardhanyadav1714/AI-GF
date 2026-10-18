package com.eva.ai.data.remote

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64

internal object OAuthPkce {
    private val urlEncoder = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun newVerifier(): String = urlEncoder.encode(ByteArray(32).also { SecureRandom().nextBytes(it) })

    fun challenge(verifier: String): String = urlEncoder
        .encode(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
}
