package com.suvojeet.notenext.todo

import androidx.compose.runtime.Immutable
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.TodoSubtask

@Immutable
data class TodoState(
    val filter: TodoFilter = TodoFilter.All,
    val isLoading: Boolean = true,
    val showAddEditDialog: Boolean = false,
    val editingTodo: TodoItem? = null,
    val editingSubtasks: List<TodoSubtask> = emptyList(),
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val completedTodayCount: Int = 0,
    val projectId: Int? = null,
    
    // AI Todo
    val isGenerating: Boolean = false,
    val showAiTodoDialog: Boolean = false,
    val aiTodoResult: List<Pair<String, String>> = emptyList()
)
