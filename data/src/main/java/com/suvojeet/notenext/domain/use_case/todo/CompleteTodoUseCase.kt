package com.suvojeet.notenext.domain.use_case.todo

import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.TodoRepository
import com.suvojeet.notenext.data.ReminderScheduler
import javax.inject.Inject

class CompleteTodoUseCase @Inject constructor(
    private val repository: TodoRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(todo: TodoItem, isCompleted: Boolean) {
        val updatedTodo = todo.copy(
            isCompleted = isCompleted,
            completedAt = if (isCompleted) System.currentTimeMillis() else null
        )
        repository.updateTodo(updatedTodo)
        
        if (isCompleted) {
            reminderScheduler.cancelTodoReminder(updatedTodo)
        } else if (updatedTodo.reminderTime != null) {
            reminderScheduler.scheduleTodoReminder(updatedTodo)
        }
    }
}
