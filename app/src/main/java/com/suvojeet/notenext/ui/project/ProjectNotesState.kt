package com.suvojeet.notenext.ui.project

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.core.util.SortType
import com.suvojeet.notenext.ui.notes.SaveStatus

import com.suvojeet.notenext.core.model.NoteType

data class ProjectNotesState(
    val notes: List<NoteSummaryWithAttachments> = emptyList(),
    val todos: List<com.suvojeet.notenext.data.TodoWithSubtasks> = emptyList(),
    val projects: List<Project> = emptyList(),
    val sortType: SortType = SortType.DATE_MODIFIED,
    val layoutType: LayoutType = LayoutType.GRID,
    val selectedNoteIds: List<Int> = emptyList(),
    val expandedNoteId: Int? = null,
    val editingTitle: String = "",
    val editingContent: TextFieldValue = TextFieldValue(),
    val editingColor: Int = 0,
    val editingIsNewNote: Boolean = true,
    val editingLastEdited: Long = 0,
    val editingHistory: List<Pair<String, TextFieldValue>> = listOf("" to TextFieldValue()),
    val editingHistoryIndex: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val editingLabel: String? = null,
    val editingProjectId: Int? = null,
    val labels: List<String> = emptyList(),
    val showLabelDialog: Boolean = false,
    val filteredLabel: String? = null,
    val isBoldActive: Boolean = false,
    val isItalicActive: Boolean = false,
    val isUnderlineActive: Boolean = false,
    val activeHeadingStyle: Int = 0,
    val activeStyles: Set<SpanStyle> = emptySet(),
    val linkPreviews: List<LinkPreview> = emptyList(),
    val editingNoteType: NoteType = NoteType.TEXT,
    val editingChecklist: List<ChecklistItem> = emptyList(),
    val checklistInputValues: Map<String, TextFieldValue> = emptyMap(),
    val focusedChecklistItemId: String? = null,
    val isCheckedItemsExpanded: Boolean = true,
    val newlyAddedChecklistItemId: String? = null,
    val editingAttachments: List<com.suvojeet.notenext.data.Attachment> = emptyList(),
    val editingIsLocked: Boolean = false,
    val editingNoteVersions: List<com.suvojeet.notenext.data.NoteVersion> = emptyList(),
    val projectName: String = "",
    val projectDescription: String? = null,
    val editingReminderTime: Long? = null,
    val editingRepeatOption: String? = null,
    val editingExpiryTime: Long? = null,
    val saveStatus: SaveStatus = SaveStatus.SAVED,

    val isSummarizing: Boolean = false,
    val summaryResult: String? = null,
    val showSummaryDialog: Boolean = false,
    val isGeneratingChecklist: Boolean = false,
    val generatedChecklistPreview: List<String> = emptyList(),
    val isFixingGrammar: Boolean = false,
    val fixedContentPreview: String? = null,
    val originalContentBackup: TextFieldValue? = null,

    // AI advanced features
    val toneRewriteSelectedTone: com.suvojeet.notenext.data.ai.ToneOption? = null,
    val toneRewriteResult: String? = null,
    val isToneRewriting: Boolean = false,
    val toneRewriteError: String? = null,

    // Search in Note
    val isSearchingInNote: Boolean = false,
    val noteSearchQuery: String = "",
    val searchResultIndices: List<Int> = emptyList(),
    val currentSearchResultIndex: Int = -1
)
