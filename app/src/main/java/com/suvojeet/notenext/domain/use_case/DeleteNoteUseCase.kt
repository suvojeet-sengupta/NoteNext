package com.suvojeet.notenext.domain.use_case

import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteRepository
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note) {
        // Soft delete (move to bin)
        repository.updateNote(note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
    }
}
