package com.suvojeet.notenext.data.share

import android.util.Base64
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Client-side end-to-end encryption for shared notes.
 *
 * A shared note's title + content are encrypted on-device with a fresh random
 * AES-256-GCM key. Only the ciphertext + IV are uploaded to the backend — the key
 * NEVER leaves the device except as the fragment (`#<key>`) of the share link,
 * which browsers/OSes never transmit to any server. The backend is therefore
 * zero-knowledge: it stores an opaque blob it cannot read.
 *
 * Wire format:
 *  - ciphertext: standard Base64 of `AES-GCM(cipher || 128-bit tag)` (Java appends
 *    the tag, which is exactly what the Web Crypto API expects on decrypt).
 *  - iv:         standard Base64 of the 12-byte GCM nonce.
 *  - key:        URL-safe Base64 (no padding) of the 32-byte key — goes in the URL
 *    fragment. URL-safe + unpadded keeps it clean in links and intent extras.
 */
object ShareCrypto {

    private const val KEY_BYTES = 32       // AES-256
    private const val IV_BYTES = 12        // standard GCM nonce length
    private const val GCM_TAG_BITS = 128
    private const val KEY_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    private val json = Json { ignoreUnknownKeys = true }

    /** Encrypts a note into an [EncryptedShare] carrying the base64 blob + the fragment key. */
    fun encrypt(title: String, content: String): EncryptedShare {
        val keyBytes = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }

        val plaintext = json
            .encodeToString(NotePayload.serializer(), NotePayload(title = title, content = content))
            .toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plaintext) // ciphertext || tag

        return EncryptedShare(
            ciphertext = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            keyFragment = Base64.encodeToString(keyBytes, KEY_FLAGS)
        )
    }

    /**
     * Decrypts a fetched share back into its [NotePayload].
     * @throws Exception if the key/iv/ciphertext don't match (wrong or truncated link).
     */
    fun decrypt(ciphertextB64: String, ivB64: String, keyFragment: String): NotePayload {
        val keyBytes = Base64.decode(keyFragment, Base64.URL_SAFE or Base64.NO_PADDING)
        val iv = Base64.decode(ivB64, Base64.DEFAULT)
        val ct = Base64.decode(ciphertextB64, Base64.DEFAULT)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val plaintext = cipher.doFinal(ct)

        return json.decodeFromString(NotePayload.serializer(), String(plaintext, Charsets.UTF_8))
    }
}
