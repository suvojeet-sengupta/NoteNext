package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.AnnotatedString
import com.suvojeet.notenext.data.NoteWithAttachments

data class NoteUiModel(
    val noteWithAttachments: NoteWithAttachments,
    val displayTitle: AnnotatedString,
    val displayContent: AnnotatedString,
    val isLocked: Boolean,
    val binnedDaysRemaining: Int? = null
)
