package com.suvojeet.notenext.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.suvojeet.notenext.core.model.AttachmentType
import kotlinx.serialization.Serializable

@Immutable
@Serializable
@Entity(
    tableName = "attachments",
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
data class Attachment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tempId: String = java.util.UUID.randomUUID().toString(),
    val noteId: Int,
    val uri: String,
    val type: AttachmentType,
    val mimeType: String
)
