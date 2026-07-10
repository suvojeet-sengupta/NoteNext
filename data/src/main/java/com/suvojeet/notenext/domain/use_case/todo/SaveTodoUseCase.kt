package com.suvojeet.notenext.domain.use_case.todo

import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.TodoRepository
import com.suvojeet.notenext.data.TodoSubtask
import com.suvojeet.notenext.data.ReminderScheduler
import javax.inject.Inject

class SaveTodoUseCase @Inject constructor(
    private val repository: TodoRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(
        todoId: Int?,
        title: String,
        description: String,
        priority: Int,
        dueDate: Long?,
        reminderTime: Long?,
        projectId: Int?,
        subtasks: List<TodoSubtask>
    ) {
        if (todoId != null && todoId != 0) {
            // Update
            val existing = repository.getTodoById(todoId)?.todo ?: return
            val updatedTodo = existing.copy(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                reminderTime = reminderTime,
                projectId = projectId
            )
            repository.updateTodo(updatedTodo)
            
            if (updatedTodo.reminderTime != null && !updatedTodo.isCompleted) {
                reminderScheduler.scheduleTodoReminder(updatedTodo)
            } else {
                reminderScheduler.cancelTodoReminder(updatedTodo)
            }

            repository.deleteSubtasksForTodo(todoId)
            repository.insertSubtasks(subtasks.map { it.copy(todoId = todoId) })
        } else {
            // Create
            val maxPos = repository.getMaxPosition()
            val newTodo = TodoItem(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                reminderTime = reminderTime,
                projectId = projectId,
                position = maxPos + 1,
                createdAt = System.currentTimeMillis()
            )
            val newId = repository.insertTodo(newTodo).toInt()
            
            if (newTodo.reminderTime != null) {
                reminderScheduler.scheduleTodoReminder(newTodo.copy(id = newId))
            }
            
            repository.insertSubtasks(subtasks.map { it.copy(todoId = newId) })
        }
    }
}
