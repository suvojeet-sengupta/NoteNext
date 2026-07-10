package com.suvojeet.notenext.changelog

import kotlinx.serialization.Serializable

@Serializable
data class ChangelogList(
    val releases: List<Release>
)

@Serializable
data class Release(
    val version: String,
    val date: String,
    val items: List<ChangelogEntry>
)

@Serializable
data class ChangelogEntry(
    val title: String,
    val description: String,
    val type: String // "FEATURE", "FIX", "IMPROVEMENT"
)
