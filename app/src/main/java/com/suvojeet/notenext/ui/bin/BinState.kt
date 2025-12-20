package com.suvojeet.notenext.ui.bin

import com.suvojeet.notenext.data.NoteWithAttachments

data class BinState(
    val notes: List<NoteWithAttachments> = emptyList(),
    val selectedNoteIds: Set<Int> = emptySet(),
    val expandedNoteId: Int? = null,
    val autoDeleteDays: Int = 7
)
