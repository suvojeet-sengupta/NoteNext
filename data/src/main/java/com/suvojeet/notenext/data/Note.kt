
package com.suvojeet.notenext.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.suvojeet.notenext.core.model.NoteType
import kotlinx.serialization.Serializable

@Immutable
@Serializable
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["lastEdited"]),
        Index(value = ["createdAt"]),
        Index(value = ["isPinned"]),
        Index(value = ["isArchived"]),
        Index(value = ["isBinned"]),
        Index(value = ["projectId"])
    ]
)
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
    val reminderTime: Long? = null,
    val repeatOption: String? = null,
    val isImportant: Boolean = false,
    val label: String? = null,
    val isBinned: Boolean = false,
    val binnedOn: Long? = null,
    val linkPreviews: List<LinkPreview> = emptyList(),
    val noteType: NoteType = NoteType.TEXT,
    val projectId: Int? = null,
    val isLocked: Boolean = false,
    val position: Int = 0,
    val aiSummary: String? = null,
    val iv: String? = null,
    val isEncrypted: Boolean = false,
    val expiryTime: Long? = null,
    val isDecoy: Boolean = false
) {
    fun toNoteSummary(): NoteSummary {
        return NoteSummary(
            id = id,
            title = title,
            content = content,
            createdAt = createdAt,
            lastEdited = lastEdited,
            color = color,
            isPinned = isPinned,
            isArchived = isArchived,
            reminderTime = reminderTime,
            label = label,
            isBinned = isBinned,
            binnedOn = binnedOn,
            isImportant = isImportant,
            noteType = noteType,
            projectId = projectId,
            isLocked = isLocked,
            position = position,
            aiSummary = aiSummary,
            iv = iv,
            isEncrypted = isEncrypted,
            repeatOption = repeatOption,
            linkPreviews = linkPreviews,
            expiryTime = expiryTime,
            isDecoy = isDecoy
        )
    }
}
