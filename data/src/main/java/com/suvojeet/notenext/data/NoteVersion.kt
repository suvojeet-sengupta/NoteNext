
package com.suvojeet.notenext.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import com.suvojeet.notenext.core.model.NoteType

@Entity(
    tableName = "note_versions",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["noteId"])]
)
data class NoteVersion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val noteId: Int,
    val title: String,
    val content: String,
    val timestamp: Long,
    val noteType: NoteType = NoteType.TEXT,
    val iv: String? = null,
    val isEncrypted: Boolean = false
)
