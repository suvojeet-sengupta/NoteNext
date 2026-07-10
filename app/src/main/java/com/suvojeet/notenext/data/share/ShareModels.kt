package com.suvojeet.notenext.data.share

import kotlinx.serialization.Serializable

/**
 * The plaintext payload that gets encrypted before it ever leaves the device.
 * Both fields live inside the ciphertext, so the server sees neither.
 */
@Serializable
data class NotePayload(
    val title: String,
    val content: String
)

/** Output of [ShareCrypto.encrypt]: the base64 blob to upload + the key for the link fragment. */
data class EncryptedShare(
    val ciphertext: String,
    val iv: String,
    /** URL-safe base64 (no padding) of the AES key. Belongs ONLY in the link fragment. */
    val keyFragment: String
)

/** How long a shared secret should live on the server before auto-deletion. */
enum class ShareExpiry(val apiValue: String) {
    TEN_MINUTES("10m"),
    ONE_HOUR("1h"),
    ONE_DAY("1d"),
    SEVEN_DAYS("7d");

    companion object {
        val DEFAULT = ONE_DAY
    }
}

/** Request body for creating an encrypted shared note. */
@Serializable
data class ShareNoteRequest(
    val ciphertext: String,
    val iv: String,
    val sharedBy: String? = null,
    /** Expiry preset key ("10m" | "1h" | "1d" | "7d"). Server defaults to 1d if null/unknown. */
    val expiresIn: String? = null,
    /** When true, the note is deleted on its first read. */
    val burnAfterRead: Boolean = false
)

/**
 * An encrypted shared note as returned by the backend. `ciphertext`/`iv` are
 * decrypted on-device with the key from the link fragment. Unknown keys are
 * ignored by the JSON config.
 */
@Serializable
data class SharedNoteDto(
    val shareId: String? = null,
    val ciphertext: String = "",
    val iv: String = "",
    val sharedBy: String? = null,
    val expiresAt: String? = null,
    val burnAfterRead: Boolean = false,
    /** True on the response that consumed a burn-after-read note. */
    val burned: Boolean = false,
    val createdAt: String? = null
)

/** Response from POST /api/notes/share. */
@Serializable
data class ShareNoteResponse(
    val message: String? = null,
    val shareId: String,
    /** Key-less link; the client appends "#<key>" before sharing it. */
    val shareUrl: String? = null,
    /**
     * Secret delete-token, returned exactly once at share time. Must be stored
     * on-device and presented to delete (unshare) the note later.
     */
    val deleteToken: String? = null,
    val expiresAt: String? = null,
    val burnAfterRead: Boolean = false
)

/** Result of creating a share link, ready to hand to the UI. */
data class ShareResult(
    val shareId: String,
    /** Full shareable link INCLUDING the "#<key>" fragment. */
    val url: String,
    /** Secret token required to later unshare this note (creator-only proof). */
    val deleteToken: String? = null,
    /** ISO-8601 expiry timestamp, for display. */
    val expiresAt: String? = null
)

/** A decrypted shared note, ready to render read-only. */
data class DecryptedNote(
    val title: String,
    val content: String,
    val sharedBy: String,
    val expiresAt: String?,
    val burnAfterRead: Boolean,
    val burned: Boolean,
    val createdAt: String?
)
