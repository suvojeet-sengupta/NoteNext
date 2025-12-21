package com.suvojeet.notenext.domain.use_case

import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.SortType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(
        searchQuery: String = "",
        sortType: SortType = SortType.DATE_MODIFIED
    ): Flow<List<NoteWithAttachments>> {
        return repository.getNotes(searchQuery, sortType)
    }
}
