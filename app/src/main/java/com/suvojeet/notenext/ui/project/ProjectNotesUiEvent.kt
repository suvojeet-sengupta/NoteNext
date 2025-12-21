
package com.suvojeet.notenext.ui.project

sealed class ProjectNotesUiEvent {
    data class SendNotes(val title: String, val content: String) : ProjectNotesUiEvent()
    data class ShowToast(val message: String) : ProjectNotesUiEvent()
    object LinkPreviewRemoved : ProjectNotesUiEvent()
    data class NavigateToNoteByTitle(val title: String) : ProjectNotesUiEvent()
}
