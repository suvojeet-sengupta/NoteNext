package com.suvojeet.notenext.domain.use_case

import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.core.util.SortType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(
        searchQuery: String = "",
        sortType: SortType = SortType.DATE_MODIFIED,
        isDecoy: Boolean = false
    ): Flow<List<NoteWithAttachments>> {
        return repository.getNotes(searchQuery, sortType, isDecoy = isDecoy)
    }

    fun getPinnedNoteSummaries(isDecoy: Boolean = false): Flow<List<NoteSummaryWithAttachments>> {
        return repository.getPinnedNoteSummaries(isDecoy = isDecoy)
    }

    fun getOtherNoteSummariesPaged(
        searchQuery: String = "",
        sortType: SortType = SortType.DATE_MODIFIED,
        isDecoy: Boolean = false
    ): Flow<androidx.paging.PagingData<NoteSummaryWithAttachments>> {
        return repository.getOtherNoteSummariesPaged(searchQuery, sortType, isDecoy = isDecoy)
    }
}
