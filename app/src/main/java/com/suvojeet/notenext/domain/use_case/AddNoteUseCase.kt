package com.suvojeet.notenext.domain.use_case

import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteRepository
import javax.inject.Inject

class AddNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Long {
        if (note.title.isBlank() && note.content.isBlank()) {
            // Depending on requirements, we might throw an exception or just return -1
            // throw InvalidNoteException("The title and content of the note can't be empty.")
            // For now, allow empty notes or let ViewModel handle it
        }
        return repository.insertNote(note)
    }
}
