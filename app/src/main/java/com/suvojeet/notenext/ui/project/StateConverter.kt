package com.suvojeet.notenext.ui.project

import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.notes.NotesListState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.persistentListOf

fun ProjectNotesState.toNotesEditState(): NotesEditState {
    return NotesEditState(
        expandedNoteId = this.expandedNoteId,
        editingTitle = this.editingTitle,
        editingContent = this.editingContent,
        editingColor = this.editingColor,
        editingIsNewNote = this.editingIsNewNote,
        editingLastEdited = this.editingLastEdited,
        canUndo = this.editingHistoryIndex > 0,
        canRedo = this.editingHistory.isNotEmpty() && this.editingHistoryIndex < this.editingHistory.lastIndex,
        isPinned = this.isPinned,
        isArchived = this.isArchived,
        editingLabel = this.editingLabel,
        labels = this.labels.toImmutableList(),
        isBoldActive = this.isBoldActive,
        isItalicActive = this.isItalicActive,
        isUnderlineActive = this.isUnderlineActive,
        activeHeadingStyle = this.activeHeadingStyle,
        activeStyles = this.activeStyles.toImmutableSet(),
        linkPreviews = this.linkPreviews.toImmutableList(),
        editingNoteType = this.editingNoteType,
        editingChecklist = this.editingChecklist.toImmutableList(),
        checklistInputValues = this.checklistInputValues.toImmutableMap(),
        focusedChecklistItemId = this.focusedChecklistItemId,
        isCheckedItemsExpanded = this.isCheckedItemsExpanded,
        newlyAddedChecklistItemId = this.newlyAddedChecklistItemId,
        editingAttachments = this.editingAttachments.toImmutableList(),
        editingIsLocked = this.editingIsLocked,
        editingNoteVersions = this.editingNoteVersions.toImmutableList(),
        editingReminderTime = this.editingReminderTime,
        editingRepeatOption = this.editingRepeatOption,
        saveStatus = this.saveStatus,
        isSummarizing = this.isSummarizing,
        summaryResult = this.summaryResult,
        showSummaryDialog = this.showSummaryDialog,
        isGeneratingChecklist = this.isGeneratingChecklist,
        generatedChecklistPreview = this.generatedChecklistPreview.toImmutableList(),
        isFixingGrammar = this.isFixingGrammar,
        fixedContentPreview = this.fixedContentPreview,
        originalContentBackup = this.originalContentBackup,
        isMentionPopupVisible = false,
        mentionSearchQuery = "",
        mentionableNotes = persistentListOf(),
        toneRewriteSelectedTone = this.toneRewriteSelectedTone,
        toneRewriteResult = this.toneRewriteResult,
        isToneRewriting = this.isToneRewriting,
        toneRewriteError = this.toneRewriteError
        )
        }
fun ProjectNotesState.toNotesListState(): NotesListState {
    return NotesListState(
        notes = this.notes.toImmutableList(),
        pinnedNotes = this.notes.filter { it.note.isPinned }.toImmutableList(),
        layoutType = this.layoutType,
        sortType = this.sortType,
        selectedNoteIds = this.selectedNoteIds.toImmutableList(),
        labels = this.labels.toImmutableList(),
        filteredLabel = this.filteredLabel,
        isLoading = false,
        projects = this.projects.toImmutableList(),
        searchQuery = "",
        filteredProjectId = null
    )
}
