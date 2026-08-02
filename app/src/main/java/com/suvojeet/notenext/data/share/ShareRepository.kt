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
        maxReads: Int = 1,
        sharedBy: String? = null
    ): Result<ShareResult> = runCatching {
        val enc = ShareCrypto.encrypt(title, content)
        val response = api.shareNote(
            ShareNoteRequest(
                ciphertext = enc.ciphertext,
                iv = enc.iv,
                sharedBy = sharedBy,
                expiresIn = expiry.apiValue,
                burnAfterRead = burnAfterRead,
                maxReads = maxReads
            )
        )
        val rawUrl = response.shareUrl?.takeIf { it.isNotBlank() }
            ?: ShareConstants.shareUrl(response.shareId)
        val base = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else {
            rawUrl
        }
        val finalUrl = if (base.contains("#")) base else "$base#${enc.keyFragment}"
        val token = response.noteToken ?: response.deleteToken
        ShareResult(
            shareId = response.shareId,
            url = finalUrl,
            key = enc.keyFragment,
            deleteToken = token,
            noteToken = token,
            expiresAt = response.expiresAt
        )
    }

    /**
     * Updates an existing note on the backend using its token.
     */
    suspend fun updateNote(
        shareId: String,
        token: String,
        title: String,
        content: String,
        expiry: ShareExpiry = ShareExpiry.DEFAULT,
        burnAfterRead: Boolean = false,
        maxReads: Int = 1,
        sharedBy: String? = null
    ): Result<ShareResult> = runCatching {
        val enc = ShareCrypto.encrypt(title, content)
        val response = api.updateNote(
            shareId = shareId,
            noteToken = token,
            deleteToken = token,
            body = ShareNoteRequest(
                ciphertext = enc.ciphertext,
                iv = enc.iv,
                sharedBy = sharedBy,
                expiresIn = expiry.apiValue,
                burnAfterRead = burnAfterRead,
                maxReads = maxReads
            )
        )
        val rawUrl = response.shareUrl?.takeIf { it.isNotBlank() }
            ?: ShareConstants.shareUrl(response.shareId)
        val base = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else {
            rawUrl
        }
        val finalUrl = if (base.contains("#")) base else "$base#${enc.keyFragment}"
        val returnedToken = response.noteToken ?: response.deleteToken ?: token
        ShareResult(
            shareId = response.shareId,
            url = finalUrl,
            key = enc.keyFragment,
            deleteToken = returnedToken,
            noteToken = returnedToken,
            expiresAt = response.expiresAt
        )
    }

    /**
     * Non-consuming check of whether a previously-created share still exists (not
     * expired / not fully burned). Used to decide whether a re-share can reuse the
     * existing link. Fails with an HttpException(404/410) when the share is gone.
     */
    suspend fun checkStatus(shareId: String): Result<ShareStatusDto> = runCatching {
        api.getStatus(shareId)
    }

    /**
     * Deletes (unshares) a note from the backend. Requires the secret noteToken/deleteToken.
     */
    suspend fun deleteNote(shareId: String, deleteToken: String): Result<Unit> = runCatching {
        api.deleteNote(shareId = shareId, noteToken = deleteToken, deleteToken = deleteToken)
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
