package com.suvojeet.notenext.domain.use_case

import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.NoteWithAttachments
import javax.inject.Inject

class GetNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: Int): NoteWithAttachments? {
        return repository.getNoteById(id)
    }
}
