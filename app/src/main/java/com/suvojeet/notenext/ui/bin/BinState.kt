package com.suvojeet.notenext.ui.bin

import com.suvojeet.notenext.data.Note

data class BinState(
    val notes: List<Note> = emptyList(),
    val selectedNoteIds: Set<Int> = emptySet(),
    val expandedNoteId: Int? = null
)
