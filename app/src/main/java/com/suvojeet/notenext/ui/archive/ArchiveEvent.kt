package com.suvojeet.notenext.ui.archive

import com.suvojeet.notenext.data.NoteSummary

sealed interface ArchiveEvent {
    data class UnarchiveNote(val note: NoteSummary) : ArchiveEvent
}