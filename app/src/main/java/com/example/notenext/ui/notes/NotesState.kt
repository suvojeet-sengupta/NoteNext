
package com.example.notenext.ui.notes

import com.example.notenext.data.Note

data class NotesState(
    val notes: List<Note> = emptyList(),
    val selectedNoteIds: List<Int> = emptyList()
)
