package com.suvojeet.notenext.ui.notes

sealed class NotesUiEvent {
    data class SendNotes(val title: String, val content: String) : NotesUiEvent()
    /**
     * A collaborative share link was created and is ready to share / open.
     * [deleteToken] is the creator's secret unshare token (null if we don't hold
     * one for this link, e.g. a share created before tokens existed).
     */
    data class ShareLinkReady(
        val url: String,
        val shareId: String,
        val title: String,
        val noteToken: String? = null,
        /** ISO-8601 expiry of the share, for display in the link dialog. */
        val expiresAt: String? = null
    ) : NotesUiEvent() {
        val deleteToken: String? get() = noteToken
    }
    /** Ask the UI to show the expiry / burn-after-read picker before creating a share link. */
    object ShowShareOptions : NotesUiEvent()
    /** Toggle a "checking existing link…" progress indicator during re-share dedup. */
    data class ShareLinkChecking(val checking: Boolean) : NotesUiEvent()
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : NotesUiEvent()
    object LinkPreviewRemoved : NotesUiEvent()
    data class ProjectCreated(val projectName: String) : NotesUiEvent()
    data class NavigateToNoteByTitle(val title: String) : NotesUiEvent()
    data class ScrollToSearchResult(val index: Int) : NotesUiEvent()
}