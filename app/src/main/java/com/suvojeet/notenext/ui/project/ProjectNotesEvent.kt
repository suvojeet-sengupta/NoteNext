package com.suvojeet.notenext.ui.project

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.ui.notes.SortType
import com.suvojeet.notenext.ui.reminder.RepeatOption
import java.time.LocalDate
import java.time.LocalTime

sealed class ProjectNotesEvent {
    data class DeleteNote(val note: NoteWithAttachments) : ProjectNotesEvent()
    object RestoreNote : ProjectNotesEvent()
    data class ToggleNoteSelection(val noteId: Int) : ProjectNotesEvent()
    object ClearSelection : ProjectNotesEvent()
    object TogglePinForSelectedNotes : ProjectNotesEvent()
    object DeleteSelectedNotes : ProjectNotesEvent()
    object ArchiveSelectedNotes : ProjectNotesEvent()
    data class ChangeColorForSelectedNotes(val color: Int) : ProjectNotesEvent()
    object CopySelectedNotes : ProjectNotesEvent()
    object SendSelectedNotes : ProjectNotesEvent()
    data class SetReminderForSelectedNotes(val date: LocalDate, val time: LocalTime, val repeatOption: RepeatOption) : ProjectNotesEvent()
    object ToggleImportantForSelectedNotes : ProjectNotesEvent()
    data class SetLabelForSelectedNotes(val label: String) : ProjectNotesEvent()
    data class ExpandNote(val noteId: Int, val noteType: String = "TEXT") : ProjectNotesEvent()
    object CollapseNote : ProjectNotesEvent()

    // Checklist Events
    data class OnChecklistItemCheckedChange(val itemId: String, val isChecked: Boolean) : ProjectNotesEvent()
    data class OnChecklistItemTextChange(val itemId: String, val text: String) : ProjectNotesEvent()
    object AddChecklistItem : ProjectNotesEvent()
    data class DeleteChecklistItem(val itemId: String) : ProjectNotesEvent()

    // Events from AddEditNoteEvent
    data class OnTitleChange(val title: String) : ProjectNotesEvent()
    data class OnContentChange(val content: TextFieldValue) : ProjectNotesEvent()
    data class ApplyStyleToContent(val style: SpanStyle) : ProjectNotesEvent()
    data class OnColorChange(val color: Int) : ProjectNotesEvent()
    object OnSaveNoteClick : ProjectNotesEvent()
    object OnDeleteNoteClick : ProjectNotesEvent()
    object OnTogglePinClick : ProjectNotesEvent()
    object OnToggleArchiveClick : ProjectNotesEvent()
    object OnUndoClick : ProjectNotesEvent()
    object OnRedoClick : ProjectNotesEvent()
    object OnCopyCurrentNoteClick : ProjectNotesEvent()
    object OnAddLabelsToCurrentNoteClick : ProjectNotesEvent()
    data class OnLabelChange(val label: String) : ProjectNotesEvent()
    object DismissLabelDialog : ProjectNotesEvent()

    data class OnLinkDetected(val url: String) : ProjectNotesEvent()
    data class OnLinkPreviewFetched(val url: String, val title: String?, val description: String?, val imageUrl: String?) : ProjectNotesEvent()
    data class OnRemoveLinkPreview(val url: String) : ProjectNotesEvent()
    object ToggleLayout : ProjectNotesEvent()
    data class SortNotes(val sortType: SortType) : ProjectNotesEvent()
    data class OnInsertLink(val url: String) : ProjectNotesEvent()
    object ClearNewlyAddedChecklistItemId : ProjectNotesEvent()
    data class AddAttachment(val uri: String, val mimeType: String) : ProjectNotesEvent()
    data class RemoveAttachment(val tempId: String) : ProjectNotesEvent()
}