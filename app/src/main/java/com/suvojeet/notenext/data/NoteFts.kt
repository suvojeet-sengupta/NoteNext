package com.suvojeet.notenext.data

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "notes_fts")
@Fts4(contentEntity = Note::class)
data class NoteFts(
    val title: String,
    val content: String
)
