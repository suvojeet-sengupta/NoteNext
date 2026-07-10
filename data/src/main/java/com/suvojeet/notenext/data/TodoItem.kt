package com.suvojeet.notenext.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 0, // 0 = Low, 1 = Medium, 2 = High
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val position: Int = 0,
    val projectId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class TodoWithSubtasks(
    @Embedded val todo: TodoItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "todoId"
    )
    val subtasks: List<TodoSubtask>
)
