package com.suvojeet.notenext.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class ChecklistItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val noteId: Int = 0,
    val text: String,
    val isChecked: Boolean,
    val position: Int = 0,
    val level: Int = 0
)