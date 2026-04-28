package com.suvojeet.notenext.core.util

data class NoteSearchState(
    val query: String,
    val sortType: SortType,
    val projectId: Int?,
    val isDecoy: Boolean
)
