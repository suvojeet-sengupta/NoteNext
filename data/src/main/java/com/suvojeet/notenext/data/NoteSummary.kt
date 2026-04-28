package com.suvojeet.notenext.data

import androidx.compose.runtime.Immutable
import com.suvojeet.notenext.core.model.NoteType
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class NoteSummary(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: Long,
    val lastEdited: Long,
    val color: Int,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val reminderTime: Long?,
    val label: String?,
    val isBinned: Boolean,
    val binnedOn: Long?,
    val isImportant: Boolean = false,
    val noteType: NoteType,
    val projectId: Int?,
    val isLocked: Boolean,
    val position: Int,
    val aiSummary: String?,
    val iv: String?,
    val isEncrypted: Boolean,
    val repeatOption: String?,
    val linkPreviews: List<LinkPreview> = emptyList(),
    val expiryTime: Long? = null,
    val isDecoy: Boolean = false
)

