package com.suvojeet.notenext.data

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class NoteSummaryWithAttachments(
    @Embedded val note: NoteSummary,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val attachments: List<Attachment>,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val checklistItems: List<ChecklistItem>
)
