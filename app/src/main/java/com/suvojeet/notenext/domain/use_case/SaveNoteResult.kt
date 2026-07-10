package com.suvojeet.notenext.domain.use_case

sealed class SaveNoteResult {
    data class Success(val noteId: Int) : SaveNoteResult()
    object Deleted : SaveNoteResult()
    object Ignored : SaveNoteResult()
}
