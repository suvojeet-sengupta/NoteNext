
package com.example.notenext.ui.notes

import com.example.notenext.data.Note

data class NotesState(
    val notes: List<Note> = emptyList(),
    val selectedNoteIds: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val expandedNoteId: Int? = null,

    // Properties from AddEditNoteState
    val editingTitle: String = "",
    val editingContent: String = "",
    val editingColor: Int = 0,
    val editingIsNewNote: Boolean = true,
    val editingLastEdited: Long = 0,
    val editingHistory: List<Pair<String, String>> = listOf("" to ""),
    val editingHistoryIndex: Int = 0
)
