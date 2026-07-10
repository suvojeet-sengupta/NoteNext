package com.suvojeet.notenext.ui.bin

import androidx.compose.runtime.Immutable
import com.suvojeet.notenext.data.NoteSummaryWithAttachments

@Immutable
data class BinState(
    val notes: List<NoteSummaryWithAttachments> = emptyList(),
    val selectedNoteIds: Set<Int> = emptySet(),
    val expandedNoteId: Int? = null,
    val autoDeleteDays: Int = 7
)
