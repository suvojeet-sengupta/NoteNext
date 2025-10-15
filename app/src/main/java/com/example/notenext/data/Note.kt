
package com.example.notenext.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val lastEdited: Long,
    val color: Int,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val reminder: Long? = null,
    val isImportant: Boolean = false,
    val label: String? = null
)
