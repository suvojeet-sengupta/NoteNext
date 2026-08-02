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
    /** When true, the note is deleted after [maxReads] reads. */
    val burnAfterRead: Boolean = false,
    /** Reads allowed before a burn-after-read note self-destructs (server clamps 1..10). */
    val maxReads: Int = 1
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
    val maxReads: Int = 1,
    /** True on the response that consumed the last allowed read of a burn note. */
    val burned: Boolean = false,
    val createdAt: String? = null
)

/** Lightweight status of a share, from GET /api/notes/:id/status (does NOT consume a read). */
@Serializable
data class ShareStatusDto(
    val exists: Boolean = false,
    val shareId: String? = null,
    val expiresAt: String? = null,
    val burnAfterRead: Boolean = false,
    val maxReads: Int = 1,
    val views: Int = 0
)

/** Response from POST /api/notes/share. */
@Serializable
data class ShareNoteResponse(
    val message: String? = null,
    val shareId: String,
    /** Key-less link; the client appends "#<key>" before sharing it. */
    val shareUrl: String? = null,
    /** Universal secret note-token for editing or deleting notes on server. */
    val noteToken: String? = null,
    val expiresAt: String? = null,
    val burnAfterRead: Boolean = false,
    val maxReads: Int = 1
)

/** Result of creating a share link, ready to hand to the UI. */
data class ShareResult(
    val shareId: String,
    /** Full shareable link INCLUDING the "#<key>" fragment. */
    val url: String,
    /** Base64url AES key (the "#<key>" fragment), stored locally to reuse the link. */
    val key: String,
    /** Universal secret note token required for edit/delete operations in x-note-token header. */
    val noteToken: String? = null,
    /** ISO-8601 expiry timestamp, for display. */
    val expiresAt: String? = null
) {
    val deleteToken: String? get() = noteToken
}

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
