package com.suvojeet.notenext.data.backup

import com.google.gson.annotations.SerializedName

data class KeepNote(
    @SerializedName("color") val color: String? = null,
    @SerializedName("isTrashed") val isTrashed: Boolean = false,
    @SerializedName("isPinned") val isPinned: Boolean = false,
    @SerializedName("isArchived") val isArchived: Boolean = false,
    @SerializedName("textContent") val textContent: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("userEditedTimestampUsec") val userEditedTimestampUsec: Long = 0,
    @SerializedName("createdTimestampUsec") val createdTimestampUsec: Long = 0,
    @SerializedName("labels") val labels: List<KeepLabel>? = null,
    @SerializedName("listContent") val listContent: List<KeepListItem>? = null,
    @SerializedName("attachments") val attachments: List<KeepAttachment>? = null
)

data class KeepLabel(
    @SerializedName("name") val name: String
)

data class KeepListItem(
    @SerializedName("text") val text: String,
    @SerializedName("isChecked") val isChecked: Boolean
)

data class KeepAttachment(
    @SerializedName("filePath") val filePath: String,
    @SerializedName("mimetype") val mimetype: String
)
