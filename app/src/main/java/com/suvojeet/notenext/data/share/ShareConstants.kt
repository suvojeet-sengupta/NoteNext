package com.suvojeet.notenext.data.share

/**
 * Endpoints and helpers for the NoteNext sharing/collaboration backend.
 *
 * The backend hosts both the REST API (under /api) and the public share page
 * (under /s/<id>) on the same origin, which is also where Socket.IO and the
 * App Links assetlinks.json live.
 */
object ShareConstants {
    /** Origin of the sharing backend (no trailing slash). Used for API calling. */
    const val BASE_URL = "https://api-notenext.suvojeetsengupta.in"

    /** Retrofit base URL (must end with a slash). */
    const val API_BASE_URL = "$BASE_URL/"

    /** Public, shareable link for a note. Opens the app via App Links, or the web page as fallback. */
    fun shareUrl(shareId: String): String = "$BASE_URL/s/$shareId"
}
