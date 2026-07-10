package com.suvojeet.notenext.data.share

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads end-to-end encrypted notes to the sharing backend and fetches them back.
 * Encryption/decryption happen here on-device via [ShareCrypto]; the backend only
 * ever sees ciphertext. Network calls are wrapped in [Result] so callers can
 * surface friendly errors without try/catch noise.
 */
@Singleton
class ShareRepository @Inject constructor(
    private val api: NoteNextApiService
) {
    /**
     * Encrypts [title]/[content] on-device, uploads only the ciphertext, and returns
     * the full share link (with the decryption key in its "#<key>" fragment) plus the
     * one-time delete-token and computed expiry.
     */
    suspend fun shareNote(
        title: String,
        content: String,
        expiry: ShareExpiry = ShareExpiry.DEFAULT,
        burnAfterRead: Boolean = false,
        sharedBy: String? = null
    ): Result<ShareResult> = runCatching {
        val enc = ShareCrypto.encrypt(title, content)
        val response = api.shareNote(
            ShareNoteRequest(
                ciphertext = enc.ciphertext,
                iv = enc.iv,
                sharedBy = sharedBy,
                expiresIn = expiry.apiValue,
                burnAfterRead = burnAfterRead
            )
        )
        val base = response.shareUrl ?: ShareConstants.shareUrl(response.shareId)
        ShareResult(
            shareId = response.shareId,
            url = "$base#${enc.keyFragment}",
            deleteToken = response.deleteToken,
            expiresAt = response.expiresAt
        )
    }

    /**
     * Deletes (unshares) a note from the backend. Requires the secret delete-token
     * issued at share time; only the creator holds it.
     */
    suspend fun deleteNote(shareId: String, deleteToken: String): Result<Unit> = runCatching {
        api.deleteNote(shareId, deleteToken)
        Unit
    }

    /**
     * Fetches a shared note and decrypts it with [keyFragment] (from the link's "#..").
     * Fails if the note is missing/expired/burned (HTTP 404/410) or the key is wrong.
     */
    suspend fun getNote(shareId: String, keyFragment: String): Result<DecryptedNote> = runCatching {
        val dto = api.getNote(shareId)
        val payload = ShareCrypto.decrypt(dto.ciphertext, dto.iv, keyFragment)
        DecryptedNote(
            title = payload.title,
            content = payload.content,
            sharedBy = dto.sharedBy?.takeIf { it.isNotBlank() } ?: "NoteNext user",
            expiresAt = dto.expiresAt,
            burnAfterRead = dto.burnAfterRead,
            burned = dto.burned,
            createdAt = dto.createdAt
        )
    }
}
