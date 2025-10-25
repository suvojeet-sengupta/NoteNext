
package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.LinkPreview

import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.ui.notes.SortType

data class NotesState(
    val notes: List<Note> = emptyList(),
    val layoutType: LayoutType = LayoutType.GRID,
    val sortType: SortType = SortType.DATE_MODIFIED,
    val selectedNoteIds: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val filteredLabel: String? = null,
    val expandedNoteId: Int? = null,
    val showLabelDialog: Boolean = false,

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
    val activeStyles: Set<SpanStyle> = emptySet(),
    val linkPreviews: List<LinkPreview> = emptyList(),
    val editingNoteType: String = "TEXT",
    val editingChecklist: List<ChecklistItem> = emptyList()
)
