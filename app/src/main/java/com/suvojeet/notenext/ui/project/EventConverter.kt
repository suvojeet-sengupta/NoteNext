package com.suvojeet.notenext.ui.project
        is NotesEvent.SetLabelForSelectedNotes -> ProjectNotesEvent.SetLabelForSelectedNotes(this.label)
        is NotesEvent.ExpandNote -> ProjectNotesEvent.ExpandNote(this.noteId, this.noteType)
        is NotesEvent.CollapseNote -> ProjectNotesEvent.CollapseNote
        is NotesEvent.OnChecklistItemCheckedChange -> ProjectNotesEvent.OnChecklistItemCheckedChange(this.itemId, this.isChecked)
        is NotesEvent.OnChecklistItemTextChange -> ProjectNotesEvent.OnChecklistItemTextChange(this.itemId, this.text)
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
        is ProjectNotesUiEvent.ShowToast -> NotesUiEvent.ShowToast(this.message)
        is ProjectNotesUiEvent.LinkPreviewRemoved -> NotesUiEvent.LinkPreviewRemoved
    }
}