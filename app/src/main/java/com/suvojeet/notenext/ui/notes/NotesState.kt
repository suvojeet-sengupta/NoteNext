package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.core.util.SortType

import com.suvojeet.notenext.core.model.NoteType

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.persistentMapOf

@Immutable
data class NotesListState(
    val notes: ImmutableList<NoteSummaryWithAttachments> = persistentListOf(),
    val pinnedNotes: ImmutableList<NoteSummaryWithAttachments> = persistentListOf(),
    val pagedNotes: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<NoteSummaryWithAttachments>> = kotlinx.coroutines.flow.emptyFlow(),
    val layoutType: LayoutType = LayoutType.GRID,
    val sortType: SortType = SortType.DATE_MODIFIED,
    val selectedNoteIds: ImmutableList<Int> = persistentListOf(),
    val labels: ImmutableList<String> = persistentListOf(),
    val filteredLabel: String? = null,
    val isLoading: Boolean = true,
    val projects: ImmutableList<Project> = persistentListOf(),
    val searchQuery: String = "",
    val filteredProjectId: Int? = null
)

@Immutable
data class NotesEditState(
    val labels: ImmutableList<String> = persistentListOf(),
    val expandedNoteId: Int? = null,
    val editingTitle: String = "",
    val editingContent: TextFieldValue = TextFieldValue(),
    val editingColor: Int = -1,
    val editingIsNewNote: Boolean = true,
    val editingLastEdited: Long? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val editingLabel: String? = null,
    val editingProjectId: Int? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isBoldActive: Boolean = false,
    val isItalicActive: Boolean = false,
    val isUnderlineActive: Boolean = false,
    val activeHeadingStyle: Int = 0,
    val activeStyles: ImmutableSet<androidx.compose.ui.text.SpanStyle> = persistentSetOf(),
    val linkPreviews: ImmutableList<LinkPreview> = persistentListOf(),
    val editingNoteType: NoteType = NoteType.TEXT,
    val editingChecklist: ImmutableList<ChecklistItem> = persistentListOf(),
    val checklistInputValues: ImmutableMap<String, TextFieldValue> = persistentMapOf(),
    val focusedChecklistItemId: String? = null,
    val isCheckedItemsExpanded: Boolean = true,
    val newlyAddedChecklistItemId: String? = null,
    val editingAttachments: ImmutableList<Attachment> = persistentListOf(),
    val editingIsLocked: Boolean = false,
    val editingNoteVersions: ImmutableList<NoteVersion> = persistentListOf(),
    val editingReminderTime: Long? = null,
    val editingRepeatOption: String? = null,
    val editingExpiryTime: Long? = null,
    val isSummarizing: Boolean = false,
    val summaryResult: String? = null,
    val showSummaryDialog: Boolean = false,
    val showLabelDialog: Boolean = false,
    val isGeneratingChecklist: Boolean = false,
    val generatedChecklistPreview: ImmutableList<String> = persistentListOf(),
    val isFixingGrammar: Boolean = false,
    val fixedContentPreview: String? = null,
    val originalContentBackup: TextFieldValue? = null,
    val saveStatus: SaveStatus = SaveStatus.SAVED,

    // Mention state
    val isMentionPopupVisible: Boolean = false,
    val mentionSearchQuery: String = "",
    val mentionableNotes: ImmutableList<NoteSummaryWithAttachments> = persistentListOf(),

    // External file & Search in note
    val externalUri: android.net.Uri? = null,
    val isSearchingInNote: Boolean = false,
    val noteSearchQuery: String = "",
    val searchResultIndices: ImmutableList<Int> = persistentListOf(),
    val currentSearchResultIndex: Int = -1,

    // ─── AI advanced features (gated by AIFeatureGate) ─────────────────
    // Tone rewriter
    val toneRewriteSelectedTone: com.suvojeet.notenext.data.ai.ToneOption? = null,
    val toneRewriteResult: String? = null,
    val isToneRewriting: Boolean = false,
    val toneRewriteError: String? = null,

    // Auto-tagging suggestions (Feature 2)
    val suggestedLabels: ImmutableList<String> = persistentListOf(),

    // Smart reminder detection (Feature 6)
    val extractedReminder: com.suvojeet.notenext.data.ai.ExtractedReminder? = null,

    // Linked notes (Feature 7)
    val linkedNotes: ImmutableList<com.suvojeet.notenext.data.Note> = persistentListOf()
)

enum class SaveStatus {
    SAVED,
    SAVING,
    UNSAVED,
    ERROR
}
