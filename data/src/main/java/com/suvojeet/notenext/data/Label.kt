package com.suvojeet.notenext.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Immutable
@Serializable
@Entity(tableName = "labels")
data class Label(
    @PrimaryKey
    val name: String,
    val parentName: String? = null
)
