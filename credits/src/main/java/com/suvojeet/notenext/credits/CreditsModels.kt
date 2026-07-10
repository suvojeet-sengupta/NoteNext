package com.suvojeet.notenext.credits

import kotlinx.serialization.Serializable

@Serializable
data class CreditsData(
    val contributors: List<Contributor>,
    val libraries: List<Library>,
    val resources: List<Resource>
)

@Serializable
data class Contributor(
    val name: String,
    val role: String,
    val avatarUrl: String,
    val githubUrl: String? = null,
    val telegramUrl: String? = null
)

@Serializable
data class Library(
    val name: String,
    val description: String
)

@Serializable
data class Resource(
    val name: String,
    val description: String
)
