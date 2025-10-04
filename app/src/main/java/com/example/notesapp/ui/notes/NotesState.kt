
package com.example.notesapp.ui.notes

import com.example.notesapp.data.Note

data class NotesState(
    val notes: List<Note> = emptyList()
)
