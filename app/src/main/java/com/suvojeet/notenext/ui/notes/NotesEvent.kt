
package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.LinkPreview

sealed class NotesEvent {
    data class DeleteNote(val note: Note) : NotesEvent()
    object RestoreNote : NotesEvent()
    data class ToggleNoteSelection(val noteId: Int) : NotesEvent()
    object ClearSelection : NotesEvent()
    object TogglePinForSelectedNotes : NotesEvent()
    object DeleteSelectedNotes : NotesEvent()
    object ArchiveSelectedNotes : NotesEvent()
    data class ChangeColorForSelectedNotes(val color: Int) : NotesEvent()
    object CopySelectedNotes : NotesEvent()
    object SendSelectedNotes : NotesEvent()
    data class SetReminderForSelectedNotes(val reminder: Long?) : NotesEvent()
    object ToggleImportantForSelectedNotes : NotesEvent()
    data class SetLabelForSelectedNotes(val label: String) : NotesEvent()
    data class ExpandNote(val noteId: Int) : NotesEvent()
    object CollapseNote : NotesEvent()

    // Events from AddEditNoteEvent
    data class OnTitleChange(val title: String) : NotesEvent()
    data class OnContentChange(val content: TextFieldValue) : NotesEvent()
    data class ApplyStyleToContent(val style: SpanStyle) : NotesEvent()
    data class OnColorChange(val color: Int) : NotesEvent()
    object OnSaveNoteClick : NotesEvent()
    object OnDeleteNoteClick : NotesEvent()
    object OnTogglePinClick : NotesEvent()
    object OnToggleArchiveClick : NotesEvent()
    object OnUndoClick : NotesEvent()
    object OnRedoClick : NotesEvent()
    object OnCopyCurrentNoteClick : NotesEvent()
    object OnAddLabelsToCurrentNoteClick : NotesEvent()
    data class OnLabelChange(val label: String) : NotesEvent()
    object DismissLabelDialog : NotesEvent()
    data class FilterByLabel(val label: String?) : NotesEvent()

    data class OnLinkDetected(val url: String) : NotesEvent()
    data class OnLinkPreviewFetched(val url: String, val title: String?, val description: String?, val imageUrl: String?) : NotesEvent()
    data class OnRemoveLinkPreview(val url: String) : NotesEvent()
    object ToggleLayout : NotesEvent()
    data class SortNotes(val sortType: SortType) : NotesEvent()
}
