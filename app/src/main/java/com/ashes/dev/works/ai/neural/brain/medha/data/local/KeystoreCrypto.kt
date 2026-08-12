package com.ashes.dev.works.ai.neural.brain.medha.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256/GCM encryption backed by the hardware-isolated Android Keystore.
 *
 * Why this exists: Gemini API keys are user credentials that bill the user's account.
 * They were being written to the settings DataStore in plaintext, so anything that can
 * read the app's files directory (a rooted device, a backup image, `run-as` on a
 * debuggable build) got the keys verbatim. The key material here never leaves the
 * Keystore — only the ciphertext is persisted.
 *
 * Format: base64( iv[12] || ciphertext||tag ), prefixed with [PREFIX] so a stored value
 * written by an older build (plaintext) is still readable and gets re-encrypted on the
 * next save.
 */
object KeystoreCrypto {

    private const val TAG = "KeystoreCrypto"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "medha_settings_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    /** Marks a value as produced by [encrypt]; anything without it is legacy plaintext. */
    private const val PREFIX = "enc1:"

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // No setUserAuthenticationRequired: the app must be able to read keys
                // headlessly (foreground service, cold start) without a lock-screen prompt.
                .build()
        )
        return generator.generateKey()
    }

    /**
     * Encrypt [plain]. Returns the original string unchanged if the Keystore is
     * unavailable — losing the user's saved key would be worse than storing it as-is,
     * and [isEncrypted] keeps the round-trip honest either way.
     */
    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return plain
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
            val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val payload = cipher.iv + encrypted
            PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "Keystore encrypt failed, storing as-is: ${e.message}")
            plain
        }
    }

    /** Decrypt a value produced by [encrypt]; passes legacy plaintext straight through. */
    fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored
        return try {
            val payload = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            if (payload.size <= IV_LENGTH) return ""
            val iv = payload.copyOfRange(0, IV_LENGTH)
            val body = payload.copyOfRange(IV_LENGTH, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            }
            String(cipher.doFinal(body), Charsets.UTF_8)
        } catch (e: Exception) {
            // Keystore key was invalidated (app data cleared, device restored) — the
            // ciphertext is unrecoverable. Return empty so the UI shows "add a key"
            // rather than sending garbage to the API.
            Log.w(TAG, "Keystore decrypt failed: ${e.message}")
            ""
        }
    }
}
