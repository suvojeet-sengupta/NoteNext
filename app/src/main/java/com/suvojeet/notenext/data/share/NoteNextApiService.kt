package com.suvojeet.notenext.data.share

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

import retrofit2.http.PUT

/** Retrofit interface for the NoteNext end-to-end encrypted sharing backend. */
interface NoteNextApiService {

    /** Uploads an encrypted note (ciphertext + iv only) and returns the share link + delete-token/noteToken. */
    @POST("api/notes/share")
    suspend fun shareNote(@Body body: ShareNoteRequest): ShareNoteResponse

    /**
     * Fetches an encrypted note. Throws on 404 (missing) / 410 (expired or burned).
     * The caller decrypts the returned ciphertext with the key from the link fragment.
     */
    @GET("api/notes/{shareId}")
    suspend fun getNote(@Path("shareId") shareId: String): SharedNoteDto

    /**
     * Non-consuming existence/metadata check — does NOT count as a read, so it never
     * burns a burn-after-read note. Throws on 404 (missing) / 410 (expired).
     */
    @GET("api/notes/{shareId}/status")
    suspend fun getStatus(@Path("shareId") shareId: String): ShareStatusDto

    /**
     * Updates an existing note. Backend authorizes via x-note-token header.
     */
    @PUT("api/notes/{shareId}")
    suspend fun updateNote(
        @Path("shareId") shareId: String,
        @Header("x-note-token") noteToken: String,
        @Body body: ShareNoteRequest
    ): ShareNoteResponse

    /**
     * Delete (unshare) a note. Backend authorizes via x-note-token header.
     */
    @DELETE("api/notes/{shareId}")
    suspend fun deleteNote(
        @Path("shareId") shareId: String,
        @Header("x-note-token") noteToken: String
    )
}
