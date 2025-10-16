package com.example.notenext.ui.archive

import com.example.notenext.data.Note

sealed interface ArchiveEvent {
    data class UnarchiveNote(val note: Note) : ArchiveEvent
}