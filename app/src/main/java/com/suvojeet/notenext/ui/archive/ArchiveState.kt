package com.suvojeet.notenext.ui.archive

import com.suvojeet.notenext.data.Note

data class ArchiveState(
    val notes: List<Note> = emptyList()
)
