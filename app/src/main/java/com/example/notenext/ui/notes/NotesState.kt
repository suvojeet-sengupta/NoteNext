
package com.example.notenext.ui.notes

import com.example.notenext.data.Note

data class NotesState(
    val notes: List<Note> = emptyList(),
    val selectedNoteIds: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val filteredLabel: String? = null,
    val expandedNoteId: Int? = null,
    val showLabelDialog: Boolean = false,

    // Properties from AddEditNoteState
    val editingTitle: String = "",
    val editingContent: String = "",
    val editingColor: Int = 0,
    val editingIsNewNote: Boolean = true,
    val editingLastEdited: Long = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val editingLabel: String? = null,
    val editingHistory: List<Pair<String, String>> = listOf("" to ""),
    val editingHistoryIndex: Int = 0
)
