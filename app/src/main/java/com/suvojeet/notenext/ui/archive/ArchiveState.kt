package com.suvojeet.notenext.ui.archive

import com.suvojeet.notenext.data.NoteWithAttachments

data class ArchiveState(
    val notes: List<NoteWithAttachments> = emptyList()
)
