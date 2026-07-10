package com.suvojeet.notenext.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(
    tableName = "todo_subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TodoItem::class,
            parentColumns = ["id"],
            childColumns = ["todoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["todoId"])]
)
data class TodoSubtask(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val todoId: Int = 0,
    val text: String,
    val isChecked: Boolean = false,
    val position: Int = 0
)
