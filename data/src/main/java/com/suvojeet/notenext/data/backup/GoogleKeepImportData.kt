package com.suvojeet.notenext.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class KeepNote(
    @SerialName("color") val color: String? = null,
    @SerialName("isTrashed") val isTrashed: Boolean = false,
    @SerialName("isPinned") val isPinned: Boolean = false,
    @SerialName("isArchived") val isArchived: Boolean = false,
    @SerialName("textContent") val textContent: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("userEditedTimestampUsec") val userEditedTimestampUsec: Long = 0,
    @SerialName("createdTimestampUsec") val createdTimestampUsec: Long = 0,
    @SerialName("labels") val labels: List<KeepLabel>? = null,
    @SerialName("listContent") val listContent: List<KeepListItem>? = null,
    @SerialName("attachments") val attachments: List<KeepAttachment>? = null
)

@Serializable
data class KeepLabel(
    @SerialName("name") val name: String
)

@Serializable
data class KeepListItem(
    @SerialName("text") val text: String,
    @SerialName("isChecked") val isChecked: Boolean
)

@Serializable
data class KeepAttachment(
    @SerialName("filePath") val filePath: String,
    @SerialName("mimetype") val mimetype: String
)
