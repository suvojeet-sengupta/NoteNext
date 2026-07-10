package com.suvojeet.notenext.todo

import com.suvojeet.notenext.data.TodoItem

sealed class TodoEvent {
    data class AddTodo(val title: String, val description: String, val priority: Int, val dueDate: Long?, val reminderTime: Long?) : TodoEvent()
    data class UpdateTodo(val todo: TodoItem) : TodoEvent()
    data class DeleteTodo(val todo: TodoItem) : TodoEvent()
    data class ToggleComplete(val todo: TodoItem) : TodoEvent()
    data class SetFilter(val filter: TodoFilter) : TodoEvent()
    object DeleteAllCompleted : TodoEvent()
    object ShowAddDialog : TodoEvent()
    data class ShowEditDialog(val todo: TodoItem) : TodoEvent()
    object DismissDialog : TodoEvent()
    data class OnDraftChange(val content: String) : TodoEvent()
    data class SaveTodo(
        val title: String, 
        val description: String, 
        val priority: Int, 
        val dueDate: Long?, 
        val reminderTime: Long?,
        val projectId: Int? = null,
        val subtasks: List<com.suvojeet.notenext.data.TodoSubtask> = emptyList()
    ) : TodoEvent()
    
    // AI Todo
    object ShowAiTodoDialog : TodoEvent()
    object DismissAiTodoDialog : TodoEvent()
    data class GenerateAiTodos(val input: String) : TodoEvent()

    // Conversion & Sharing
    data class ConvertToNote(val todo: com.suvojeet.notenext.data.TodoWithSubtasks) : TodoEvent()
    data class ShareTodo(val todo: com.suvojeet.notenext.data.TodoWithSubtasks) : TodoEvent()

    // Reordering
    data class UpdatePositions(val items: List<TodoItem>) : TodoEvent()
}
