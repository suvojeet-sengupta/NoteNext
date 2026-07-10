package com.suvojeet.notenext.data

import kotlinx.serialization.Serializable

@Serializable
data class LinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?
)
