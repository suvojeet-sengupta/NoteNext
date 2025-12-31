package com.suvojeet.notenext.ui.project

import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesUiEvent

fun NotesEvent.toProjectNotesEvent(): ProjectNotesEvent {
    return when (this) {
        is NotesEvent.DeleteNote -> ProjectNotesEvent.DeleteNote(this.note)
        is NotesEvent.RestoreNote -> ProjectNotesEvent.RestoreNote
        is NotesEvent.ToggleNoteSelection -> ProjectNotesEvent.ToggleNoteSelection(this.noteId)
        is NotesEvent.ClearSelection -> ProjectNotesEvent.ClearSelection
        is NotesEvent.TogglePinForSelectedNotes -> ProjectNotesEvent.TogglePinForSelectedNotes
        is NotesEvent.DeleteSelectedNotes -> ProjectNotesEvent.DeleteSelectedNotes
        is NotesEvent.SelectAllNotes -> ProjectNotesEvent.SelectAllNotes
        is NotesEvent.ArchiveSelectedNotes -> ProjectNotesEvent.ArchiveSelectedNotes
        is NotesEvent.ChangeColorForSelectedNotes -> ProjectNotesEvent.ChangeColorForSelectedNotes(this.color)
        is NotesEvent.CopySelectedNotes -> ProjectNotesEvent.CopySelectedNotes
        is NotesEvent.SendSelectedNotes -> ProjectNotesEvent.SendSelectedNotes
        is NotesEvent.SetReminderForSelectedNotes -> ProjectNotesEvent.SetReminderForSelectedNotes(this.date, this.time, this.repeatOption)
        is NotesEvent.ToggleImportantForSelectedNotes -> ProjectNotesEvent.ToggleImportantForSelectedNotes
        is NotesEvent.SetLabelForSelectedNotes -> ProjectNotesEvent.SetLabelForSelectedNotes(this.label)
        is NotesEvent.ExpandNote -> ProjectNotesEvent.ExpandNote(this.noteId, this.noteType)
        is NotesEvent.CollapseNote -> ProjectNotesEvent.CollapseNote
        is NotesEvent.OnChecklistItemCheckedChange -> ProjectNotesEvent.OnChecklistItemCheckedChange(this.itemId, this.isChecked)
        is NotesEvent.OnChecklistItemTextChange -> ProjectNotesEvent.OnChecklistItemTextChange(this.itemId, this.text)
        is NotesEvent.OnChecklistItemValueChange -> ProjectNotesEvent.OnChecklistItemValueChange(this.itemId, this.value)
        is NotesEvent.OnChecklistItemFocus -> ProjectNotesEvent.OnChecklistItemFocus(this.itemId)
        is NotesEvent.SwapChecklistItems -> ProjectNotesEvent.SwapChecklistItems(this.fromId, this.toId)
        is NotesEvent.AddChecklistItem -> ProjectNotesEvent.AddChecklistItem
        is NotesEvent.DeleteChecklistItem -> ProjectNotesEvent.DeleteChecklistItem(this.itemId)
        is NotesEvent.OnTitleChange -> ProjectNotesEvent.OnTitleChange(this.title)
        is NotesEvent.OnContentChange -> ProjectNotesEvent.OnContentChange(this.content)
        is NotesEvent.ApplyStyleToContent -> ProjectNotesEvent.ApplyStyleToContent(this.style)
        is NotesEvent.ApplyHeadingStyle -> ProjectNotesEvent.ApplyHeadingStyle(this.level)
        is NotesEvent.OnColorChange -> ProjectNotesEvent.OnColorChange(this.color)
        is NotesEvent.OnSaveNoteClick -> ProjectNotesEvent.OnSaveNoteClick
        is NotesEvent.OnDeleteNoteClick -> ProjectNotesEvent.OnDeleteNoteClick
        is NotesEvent.OnTogglePinClick -> ProjectNotesEvent.OnTogglePinClick
        is NotesEvent.OnToggleLockClick -> ProjectNotesEvent.OnToggleLockClick
        is NotesEvent.OnToggleArchiveClick -> ProjectNotesEvent.OnToggleArchiveClick
        is NotesEvent.OnUndoClick -> ProjectNotesEvent.OnUndoClick
        is NotesEvent.OnRedoClick -> ProjectNotesEvent.OnRedoClick
        is NotesEvent.OnCopyCurrentNoteClick -> ProjectNotesEvent.OnCopyCurrentNoteClick
        is NotesEvent.OnAddLabelsToCurrentNoteClick -> ProjectNotesEvent.OnAddLabelsToCurrentNoteClick
        is NotesEvent.OnLabelChange -> ProjectNotesEvent.OnLabelChange(this.label)
        is NotesEvent.DismissLabelDialog -> ProjectNotesEvent.DismissLabelDialog
        is NotesEvent.OnLinkDetected -> ProjectNotesEvent.OnLinkDetected(this.url)
        is NotesEvent.OnLinkPreviewFetched -> ProjectNotesEvent.OnLinkPreviewFetched(this.url, this.title, this.description, this.imageUrl)
        is NotesEvent.OnRemoveLinkPreview -> ProjectNotesEvent.OnRemoveLinkPreview(this.url)
        is NotesEvent.ToggleLayout -> ProjectNotesEvent.ToggleLayout
        is NotesEvent.SortNotes -> ProjectNotesEvent.SortNotes(this.sortType)
        is NotesEvent.OnInsertLink -> ProjectNotesEvent.OnInsertLink(this.url)
        is NotesEvent.ClearNewlyAddedChecklistItemId -> ProjectNotesEvent.ClearNewlyAddedChecklistItemId
        is NotesEvent.AddAttachment -> ProjectNotesEvent.AddAttachment(this.uri, this.mimeType)
        is NotesEvent.RemoveAttachment -> ProjectNotesEvent.RemoveAttachment(this.tempId)
        is NotesEvent.OnRestoreVersion -> ProjectNotesEvent.OnRestoreVersion(this.version)
        is NotesEvent.NavigateToNoteByTitle -> ProjectNotesEvent.NavigateToNoteByTitle(this.title)
        is NotesEvent.CreateProject -> throw IllegalArgumentException("CreateProject event cannot be converted")
        is NotesEvent.FilterByLabel -> throw IllegalArgumentException("FilterByLabel event cannot be converted")
        is NotesEvent.MoveSelectedNotesToProject -> throw IllegalArgumentException("MoveSelectedNotesToProject event cannot be converted")
        is NotesEvent.ToggleLockForSelectedNotes -> ProjectNotesEvent.ToggleLockForSelectedNotes
        is NotesEvent.CreateNoteFromSharedText -> throw IllegalArgumentException("CreateNoteFromSharedText event cannot be converted")
        is NotesEvent.OnSearchQueryChange -> throw IllegalArgumentException("OnSearchQueryChange event cannot be converted")
        is NotesEvent.OnToggleNoteType -> ProjectNotesEvent.OnToggleNoteType
        is NotesEvent.DeleteAllCheckedItems -> ProjectNotesEvent.DeleteAllCheckedItems
        is NotesEvent.ToggleCheckedItemsExpanded -> ProjectNotesEvent.ToggleCheckedItemsExpanded
    }
}

fun ProjectNotesUiEvent.toNotesUiEvent(): NotesUiEvent {
    return when (this) {
        is ProjectNotesUiEvent.SendNotes -> NotesUiEvent.SendNotes(this.title, this.content)
        is ProjectNotesUiEvent.ShowToast -> NotesUiEvent.ShowToast(this.message)
        is ProjectNotesUiEvent.LinkPreviewRemoved -> NotesUiEvent.LinkPreviewRemoved
        is ProjectNotesUiEvent.NavigateToNoteByTitle -> NotesUiEvent.NavigateToNoteByTitle(this.title)
    }
}