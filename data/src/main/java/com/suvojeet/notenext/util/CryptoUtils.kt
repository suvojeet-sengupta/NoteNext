package com.suvojeet.notenext.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteSummary
import com.suvojeet.notenext.data.NoteVersion
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.text.Charsets

object CryptoUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "NoteNextSecretKey"
    private const val AUTH_KEY_ALIAS_V1 = "NoteNextAuthSecretKey"
    private const val AUTH_KEY_ALIAS_V2 = "NoteNextAuthKeyV2"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getSecretKey(alias: String = KEY_ALIAS, requireAuth: Boolean = false): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(alias, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setInvalidatedByBiometricEnrollment(false)
            
        if (requireAuth) {
            builder.setUserAuthenticationRequired(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(60, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
            } else {
                builder.setUserAuthenticationValidityDurationSeconds(60)
            }
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    fun getEncryptionCipher(isLocked: Boolean): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val alias = if (isLocked) AUTH_KEY_ALIAS_V2 else KEY_ALIAS
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(alias, isLocked))
        return cipher
    }

    fun getDecryptionCipher(iv: ByteArray, isLocked: Boolean, useV1: Boolean = false): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val alias = if (isLocked) {
            if (useV1) AUTH_KEY_ALIAS_V1 else AUTH_KEY_ALIAS_V2
        } else {
            KEY_ALIAS
        }
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(alias, isLocked), GCMParameterSpec(128, iv))
        return cipher
    }

    fun encryptNote(note: Note): Note {
        if (note.isEncrypted) return note

        val isLocked = note.isLocked

        // Title is no longer encrypted as per requirement.
        val unencryptedTitle = note.title

        // Encrypt content. Always use UTF-8 explicitly — relying on the platform default
        // charset corrupts non-Latin content on devices/locales where it isn't UTF-8.
        val cipherContent = getEncryptionCipher(isLocked)
        val ivContent = cipherContent.iv
        val encryptedContent = Base64.encodeToString(cipherContent.doFinal(note.content.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)

        // Combine IVs: v3:ivContent (v3 prefix: title unencrypted, content encrypted)
        val combinedIv = if (isLocked) {
            "v3:" + Base64.encodeToString(ivContent, Base64.DEFAULT)
        } else {
            "v3-plain:" + Base64.encodeToString(ivContent, Base64.DEFAULT)
        }

        return note.copy(
            title = unencryptedTitle,
            content = encryptedContent,
            iv = combinedIv,
            isEncrypted = true
        )
    }

    /**
     * Decrypts a note.
     * If isLocked is true, this WILL FAIL unless called after biometric authentication
     * or if the key doesn't require auth (legacy notes).
     *
     * On failure the ORIGINAL note is returned unchanged (still `isEncrypted=true` with
     * its `iv`). Callers should check `result.isEncrypted` and treat it as "still
     * locked / failed to decrypt" rather than substituting placeholder content. This
     * prevents the previous bug where a placeholder string ("Unable to decrypt…") could
     * be persisted back to the DB on round-trip updates, destroying the ciphertext.
     */
    fun decryptNote(note: Note, authenticatedCipherTitle: Cipher? = null, authenticatedCipherContent: Cipher? = null): Note {
        return try {
            val decrypted = decryptNoteInternal(
                note.id, note.title, note.content, note.iv, note.isEncrypted, note.isLocked,
                authenticatedCipherTitle, authenticatedCipherContent
            )
            note.copy(
                title = decrypted.first,
                content = decrypted.second,
                iv = null,
                isEncrypted = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            note
        }
    }

    fun decryptNote(summary: NoteSummary, authenticatedCipherTitle: Cipher? = null, authenticatedCipherContent: Cipher? = null): NoteSummary {
        return try {
            val decrypted = decryptNoteInternal(
                summary.id, summary.title, summary.content, summary.iv, summary.isEncrypted, summary.isLocked,
                authenticatedCipherTitle, authenticatedCipherContent
            )
            summary.copy(
                title = decrypted.first,
                content = decrypted.second,
                iv = null,
                isEncrypted = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            summary
        }
    }

    /**
     * Throws on failure. The two `decryptNote(...)` wrappers above catch and return the
     * original encrypted note, so the placeholder-string-as-content data-loss bug can
     * no longer happen on a round-trip update.
     */
    private fun decryptNoteInternal(
        id: Int, title: String, content: String, iv: String?, isEncrypted: Boolean, isLocked: Boolean,
        authenticatedCipherTitle: Cipher? = null, authenticatedCipherContent: Cipher? = null
    ): Pair<String, String> {
        if (!isEncrypted || iv == null) return Pair(title, content)

        val rawIv = iv
        val isV3 = rawIv.startsWith("v3:") || rawIv.startsWith("v3-plain:")
        val isV2 = rawIv.startsWith("v2:")

        val cleanIv = if (isV3) {
            if (rawIv.startsWith("v3:")) rawIv.substring(3) else rawIv.substring(9)
        } else if (isV2) {
            rawIv.substring(3)
        } else {
            rawIv
        }

        val ivs = cleanIv.split(":")
        val effectiveIsLocked = if (isV3) rawIv.startsWith("v3:") else (isLocked && cleanIv.contains(":"))

        return if (isV3) {
            // v3 format: title is already unencrypted
            val ivContent = Base64.decode(cleanIv, Base64.DEFAULT)
            val decryptedContent = if (authenticatedCipherContent != null) {
                String(authenticatedCipherContent.doFinal(Base64.decode(content, Base64.DEFAULT)), Charsets.UTF_8)
            } else {
                val cipherContent = getDecryptionCipher(ivContent, effectiveIsLocked, useV1 = false)
                String(cipherContent.doFinal(Base64.decode(content, Base64.DEFAULT)), Charsets.UTF_8)
            }
            Pair(title, decryptedContent)
        } else if (ivs.size == 2) {
            // v2 or v1 format with two IVs
            val ivTitle = Base64.decode(ivs[0], Base64.DEFAULT)
            val ivContent = Base64.decode(ivs[1], Base64.DEFAULT)

            val decryptedTitle = if (authenticatedCipherTitle != null) {
                String(authenticatedCipherTitle.doFinal(Base64.decode(title, Base64.DEFAULT)), Charsets.UTF_8)
            } else {
                val cipherTitle = getDecryptionCipher(ivTitle, effectiveIsLocked, useV1 = !isV2)
                String(cipherTitle.doFinal(Base64.decode(title, Base64.DEFAULT)), Charsets.UTF_8)
            }

            val decryptedContent = if (authenticatedCipherContent != null) {
                String(authenticatedCipherContent.doFinal(Base64.decode(content, Base64.DEFAULT)), Charsets.UTF_8)
            } else {
                val cipherContent = getDecryptionCipher(ivContent, effectiveIsLocked, useV1 = !isV2)
                String(cipherContent.doFinal(Base64.decode(content, Base64.DEFAULT)), Charsets.UTF_8)
            }

            Pair(decryptedTitle, decryptedContent)
        } else {
            // Legacy format with single IV for both
            val ivBytes = Base64.decode(cleanIv, Base64.DEFAULT)
            val cipher = getDecryptionCipher(ivBytes, effectiveIsLocked, useV1 = !isV2)
            val decryptedTitle = String(cipher.doFinal(Base64.decode(title, Base64.DEFAULT)), Charsets.UTF_8)

            // Re-init for content
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(if (effectiveIsLocked) (if (isV2) AUTH_KEY_ALIAS_V2 else AUTH_KEY_ALIAS_V1) else KEY_ALIAS, effectiveIsLocked), GCMParameterSpec(128, ivBytes))
            val decryptedContent = String(cipher.doFinal(Base64.decode(content, Base64.DEFAULT)), Charsets.UTF_8)
            Pair(decryptedTitle, decryptedContent)
        }
    }

    fun encryptChecklistItem(item: com.suvojeet.notenext.data.ChecklistItem, isLocked: Boolean): com.suvojeet.notenext.data.ChecklistItem {
        if (item.isEncrypted) return item

        val cipher = getEncryptionCipher(isLocked)
        val iv = cipher.iv
        val encryptedText = Base64.encodeToString(cipher.doFinal(item.text.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)

        val combinedIv = if (isLocked) "v2:" + Base64.encodeToString(iv, Base64.DEFAULT) else Base64.encodeToString(iv, Base64.DEFAULT)

        return item.copy(
            text = encryptedText,
            iv = combinedIv,
            isEncrypted = true
        )
    }

    fun decryptChecklistItem(item: com.suvojeet.notenext.data.ChecklistItem, isLocked: Boolean): com.suvojeet.notenext.data.ChecklistItem {
        if (!item.isEncrypted || item.iv == null) return item

        return try {
            val rawIv = item.iv
            val isV2 = rawIv.startsWith("v2:")
            val cleanIv = if (isV2) rawIv.substring(3) else rawIv
            val iv = Base64.decode(cleanIv, Base64.DEFAULT)
            
            val cipher = getDecryptionCipher(iv, isLocked, useV1 = !isV2)
            val decryptedText = String(cipher.doFinal(Base64.decode(item.text, Base64.DEFAULT)), Charsets.UTF_8)

            item.copy(
                text = decryptedText,
                iv = null,
                isEncrypted = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Preserve original ciphertext + iv. Caller checks `isEncrypted` and shows
            // a "locked" UI rather than persisting a placeholder.
            item
        }
    }

    fun encryptNoteVersion(version: NoteVersion, isLocked: Boolean): NoteVersion {
        if (version.isEncrypted) return version

        // Encrypt title
        val cipherTitle = getEncryptionCipher(isLocked)
        val ivTitle = cipherTitle.iv
        val encryptedTitle = Base64.encodeToString(cipherTitle.doFinal(version.title.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)

        // Encrypt content
        val cipherContent = getEncryptionCipher(isLocked)
        val ivContent = cipherContent.iv
        val encryptedContent = Base64.encodeToString(cipherContent.doFinal(version.content.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)

        val combinedIv = if (isLocked) {
            "v2:" + Base64.encodeToString(ivTitle, Base64.DEFAULT) + ":" + Base64.encodeToString(ivContent, Base64.DEFAULT)
        } else {
            Base64.encodeToString(ivTitle, Base64.DEFAULT) + ":" + Base64.encodeToString(ivContent, Base64.DEFAULT)
        }

        return version.copy(
            title = encryptedTitle,
            content = encryptedContent,
            iv = combinedIv,
            isEncrypted = true
        )
    }

    fun decryptNoteVersion(version: NoteVersion, isLocked: Boolean): NoteVersion {
        if (!version.isEncrypted || version.iv == null) return version

        return try {
            val rawIv = version.iv
            val isV2 = rawIv.startsWith("v2:")
            val cleanIv = if (isV2) rawIv.substring(3) else rawIv
            val ivs = cleanIv.split(":")
            
            val (decryptedTitle, decryptedContent) = if (ivs.size == 2) {
                val ivTitle = Base64.decode(ivs[0], Base64.DEFAULT)
                val ivContent = Base64.decode(ivs[1], Base64.DEFAULT)
                
                val cipherTitle = getDecryptionCipher(ivTitle, isLocked, useV1 = !isV2)
                val title = String(cipherTitle.doFinal(Base64.decode(version.title, Base64.DEFAULT)), Charsets.UTF_8)

                val cipherContent = getDecryptionCipher(ivContent, isLocked, useV1 = !isV2)
                val content = String(cipherContent.doFinal(Base64.decode(version.content, Base64.DEFAULT)), Charsets.UTF_8)
                
                Pair(title, content)
            } else {
                val iv = Base64.decode(cleanIv, Base64.DEFAULT)
                val cipher = getDecryptionCipher(iv, isLocked, useV1 = !isV2)
                val title = String(cipher.doFinal(Base64.decode(version.title, Base64.DEFAULT)), Charsets.UTF_8)

                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(if (isLocked) (if (isV2) AUTH_KEY_ALIAS_V2 else AUTH_KEY_ALIAS_V1) else KEY_ALIAS, isLocked), GCMParameterSpec(128, iv))
                val content = String(cipher.doFinal(Base64.decode(version.content, Base64.DEFAULT)), Charsets.UTF_8)
                Pair(title, content)
            }

            version.copy(
                title = decryptedTitle,
                content = decryptedContent,
                iv = null,
                isEncrypted = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Preserve the original encrypted version unchanged. Caller should treat
            // `isEncrypted=true` as the failure signal — never persist a placeholder.
            version
        }
    }
}
