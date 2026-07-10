package com.suvojeet.notenext.ui.archive

import androidx.compose.runtime.Immutable
import com.suvojeet.notenext.data.NoteSummaryWithAttachments

@Immutable
data class ArchiveState(
    val notes: List<NoteSummaryWithAttachments> = emptyList()
)
