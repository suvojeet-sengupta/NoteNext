
package com.example.notesapp.ui.notes

import com.example.notesapp.data.Note

sealed class NotesEvent {
    data class DeleteNote(val note: Note) : NotesEvent()
    object RestoreNote : NotesEvent()
    data class ToggleNoteSelection(val noteId: Int) : NotesEvent()
    object ClearSelection : NotesEvent()
    object PinSelectedNotes : NotesEvent()
    object DeleteSelectedNotes : NotesEvent()
}
