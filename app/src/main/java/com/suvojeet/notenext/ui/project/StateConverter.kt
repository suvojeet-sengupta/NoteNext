
package com.suvojeet.notenext.ui.project

import com.suvojeet.notenext.ui.notes.NotesState

fun ProjectNotesState.toNotesState(): NotesState {
    return NotesState(
        notes = this.notes,
        projects = this.projects,
        sortType = this.sortType,
        layoutType = this.layoutType,
        selectedNoteIds = this.selectedNoteIds,
        expandedNoteId = this.expandedNoteId,
        editingTitle = this.editingTitle,
        editingContent = this.editingContent,
        editingColor = this.editingColor,
        editingIsNewNote = this.editingIsNewNote,
        editingLastEdited = this.editingLastEdited,
        editingHistory = this.editingHistory,
        editingHistoryIndex = this.editingHistoryIndex,
        isPinned = this.isPinned,
        isArchived = this.isArchived,
        editingLabel = this.editingLabel,
        labels = this.labels,
        showLabelDialog = this.showLabelDialog,
        filteredLabel = this.filteredLabel,
        isBoldActive = this.isBoldActive,
        isItalicActive = this.isItalicActive,
        isUnderlineActive = this.isUnderlineActive,
        activeStyles = this.activeStyles,
        linkPreviews = this.linkPreviews,
        editingNoteType = this.editingNoteType,
        editingChecklist = this.editingChecklist,
        newlyAddedChecklistItemId = this.newlyAddedChecklistItemId,
        editingAttachments = this.editingAttachments
    )
}
