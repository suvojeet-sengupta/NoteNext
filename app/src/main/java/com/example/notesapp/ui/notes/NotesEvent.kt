
package com.example.notesapp.ui.notes

import com.example.notesapp.data.Note

sealed class NotesEvent {
    data class DeleteNote(val note: Note) : NotesEvent()
    object RestoreNote : NotesEvent()
}
