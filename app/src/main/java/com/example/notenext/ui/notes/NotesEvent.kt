
package com.example.notenext.ui.notes

import com.example.notenext.data.Note

sealed class NotesEvent {
    data class DeleteNote(val note: Note) : NotesEvent()
    object RestoreNote : NotesEvent()
    data class ToggleNoteSelection(val noteId: Int) : NotesEvent()
    object ClearSelection : NotesEvent()
    object TogglePinForSelectedNotes : NotesEvent()
    object DeleteSelectedNotes : NotesEvent()
    object ArchiveSelectedNotes : NotesEvent()
    data class ChangeColorForSelectedNotes(val color: Int) : NotesEvent()
    object CopySelectedNotes : NotesEvent()
    object SendSelectedNotes : NotesEvent()
    data class SetReminderForSelectedNotes(val reminder: Long?) : NotesEvent()
    object ToggleImportantForSelectedNotes : NotesEvent()
}
