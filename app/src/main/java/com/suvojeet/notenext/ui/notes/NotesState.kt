
package com.suvojeet.notenext.ui.notes

import com.suvojeet.notenext.data.SortType

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.NoteWithAttachments

import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.NoteVersion

data class NotesState(
    val notes: List<NoteWithAttachments> = emptyList(),
    val layoutType: LayoutType = LayoutType.GRID,
    val sortType: SortType = SortType.DATE_MODIFIED,
    val selectedNoteIds: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val filteredLabel: String? = null,
    val expandedNoteId: Int? = null,
    val showLabelDialog: Boolean = false,
    val isLoading: Boolean = true,

    // Properties from AddEditNoteState
    val editingTitle: String = "",
    val editingContent: TextFieldValue = TextFieldValue(),
    val editingColor: Int = 0,
    val editingIsNewNote: Boolean = true,
    val editingLastEdited: Long = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val editingLabel: String? = null,
    val editingHistory: List<Pair<String, TextFieldValue>> = listOf("" to TextFieldValue()),
    val editingHistoryIndex: Int = 0,
    val isBoldActive: Boolean = false,
    val isItalicActive: Boolean = false,
    val isUnderlineActive: Boolean = false,
    val activeHeadingStyle: Int = 0,
    val activeStyles: Set<SpanStyle> = emptySet(),
    val linkPreviews: List<LinkPreview> = emptyList(),
    val editingNoteType: String = "TEXT",
    val editingChecklist: List<ChecklistItem> = emptyList(),
    val newlyAddedChecklistItemId: String? = null,
    val editingAttachments: List<Attachment> = emptyList(),
    val editingIsLocked: Boolean = false,
    val editingNoteVersions: List<NoteVersion> = emptyList(),

    val projects: List<Project> = emptyList(),
    val searchQuery: String = ""
)
