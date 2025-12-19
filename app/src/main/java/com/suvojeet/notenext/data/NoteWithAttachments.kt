package com.suvojeet.notenext.data

import androidx.room.Embedded
import androidx.room.Relation

data class NoteWithAttachments(
    @Embedded val note: Note,
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
