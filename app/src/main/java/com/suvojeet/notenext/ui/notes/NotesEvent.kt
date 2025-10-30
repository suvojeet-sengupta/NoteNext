
package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.LinkPreview

import com.suvojeet.notenext.data.NoteWithAttachments

import com.suvojeet.notenext.ui.reminder.RepeatOption
import java.time.LocalDate
import java.time.LocalTime

sealed class NotesEvent {
    data class DeleteNote(val note: NoteWithAttachments) : NotesEvent()
    object RestoreNote : NotesEvent()
    data class ToggleNoteSelection(val noteId: Int) : NotesEvent()
    object ClearSelection : NotesEvent()
    object TogglePinForSelectedNotes : NotesEvent()
    object DeleteSelectedNotes : NotesEvent()
    object ArchiveSelectedNotes : NotesEvent()
    data class ChangeColorForSelectedNotes(val color: Int) : NotesEvent()
    object CopySelectedNotes : NotesEvent()
    object SendSelectedNotes : NotesEvent()
    data class SetReminderForSelectedNotes(val date: LocalDate, val time: LocalTime, val repeatOption: RepeatOption) : NotesEvent()
    object ToggleImportantForSelectedNotes : NotesEvent()
    data class SetLabelForSelectedNotes(val label: String) : NotesEvent()
    data class ExpandNote(val noteId: Int, val noteType: String = "TEXT") : NotesEvent()
    object CollapseNote : NotesEvent()

    // Checklist Events
    data class OnChecklistItemCheckedChange(val itemId: String, val isChecked: Boolean) : NotesEvent()
    data class OnChecklistItemTextChange(val itemId: String, val text: String) : NotesEvent()
    object AddChecklistItem : NotesEvent()
    data class DeleteChecklistItem(val itemId: String) : NotesEvent()

    // Events from AddEditNoteEvent
    data class OnTitleChange(val title: String) : NotesEvent()
    data class OnContentChange(val content: TextFieldValue) : NotesEvent()
    data class ApplyStyleToContent(val style: SpanStyle) : NotesEvent()
    data class ApplyHeadingStyle(val level: Int) : NotesEvent()
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
    data class OnInsertLink(val url: String) : NotesEvent()
    object ClearNewlyAddedChecklistItemId : NotesEvent()
    data class AddAttachment(val uri: String, val mimeType: String) : NotesEvent()
    data class RemoveAttachment(val tempId: String) : NotesEvent()
    data class CreateProject(val name: String) : NotesEvent()
    data class MoveSelectedNotesToProject(val projectId: Int?) : NotesEvent()
}
