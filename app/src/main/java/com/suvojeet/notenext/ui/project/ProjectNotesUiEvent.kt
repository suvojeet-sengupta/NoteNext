
package com.suvojeet.notenext.ui.project

sealed class ProjectNotesUiEvent {
    data class SendNotes(val title: String, val content: String) : ProjectNotesUiEvent()
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : ProjectNotesUiEvent()
    object LinkPreviewRemoved : ProjectNotesUiEvent()
    data class NavigateToNoteByTitle(val title: String) : ProjectNotesUiEvent()
    data class ScrollToSearchResult(val index: Int) : ProjectNotesUiEvent()
}
