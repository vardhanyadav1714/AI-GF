package com.eva.ai.data.remote

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class SecureSessionTokens(private val prefs: SharedPreferences) {
    fun read(name: String): String? = synchronized(lock) {
        val stored = prefs.getString(name, null) ?: return@synchronized null
        try {
            if (!stored.startsWith(PREFIX)) {
                // Migrate existing installations without retaining plaintext credentials.
                writeValue(name, stored)
                stored
            } else {
                val bytes = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
                require(bytes.size > 28)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
                String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            prefs.edit().remove("access_token").remove("refresh_token").commit()
            null
        }
    }

    fun save(accessToken: String, refreshToken: String?) = synchronized(lock) {
        val editor = prefs.edit().putString("access_token", encrypt("access_token", accessToken))
        if (refreshToken == null) editor.remove("refresh_token")
        else editor.putString("refresh_token", encrypt("refresh_token", refreshToken))
        check(editor.commit()) { "Could not securely save your session. Please try signing in again." }
    }

    fun saveOAuthVerifier(verifier: String) = synchronized(lock) {
        writeValue("oauth_verifier", verifier)
    }

    private fun writeValue(name: String, value: String) {
        check(prefs.edit().putString(name, encrypt(name, value)).commit())
    }

    private fun encrypt(name: String, value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    companion object {
        private const val PREFIX = "enc-v1:"
        private const val ALIAS = "eva.session.tokens.v1"
        private val lock = Any()
    }
}
