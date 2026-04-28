package com.suvojeet.notenext.domain.use_case

import com.suvojeet.notenext.core.util.SortType
import javax.inject.Inject

data class NoteUseCases @Inject constructor(
    val getNotes: GetNotesUseCase,
    val deleteNote: DeleteNoteUseCase,
    val addNote: AddNoteUseCase,
    val getNote: GetNoteUseCase
) {
    fun getPinnedNoteSummaries(isDecoy: Boolean = false) = getNotes.getPinnedNoteSummaries(isDecoy)
    fun getOtherNoteSummariesPaged(query: String = "", sortType: SortType = SortType.DATE_MODIFIED, isDecoy: Boolean = false) =
        getNotes.getOtherNoteSummariesPaged(query, sortType, isDecoy)
}
