package com.suvojeet.notenext.ui.archive

import com.suvojeet.notenext.data.Note

sealed interface ArchiveEvent {
    data class UnarchiveNote(val note: Note) : ArchiveEvent
}