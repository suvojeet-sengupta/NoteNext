package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import java.util.Calendar

interface TodoRepository {
    fun getPagedTodos(): Flow<PagingData<TodoWithSubtasks>>
    fun getPagedActiveTodos(): Flow<PagingData<TodoWithSubtasks>>
    fun getPagedCompletedTodos(): Flow<PagingData<TodoWithSubtasks>>
    fun getTodosByProject(projectId: Int): Flow<List<TodoWithSubtasks>>
    suspend fun getTodoById(id: Int): TodoWithSubtasks?
    suspend fun insertTodo(todo: TodoItem): Long
    suspend fun updateTodo(todo: TodoItem)
    suspend fun deleteTodo(todo: TodoItem)
    suspend fun deleteAllCompleted()
    fun getActiveCount(): Flow<Int>
    fun getCompletedCount(): Flow<Int>
    fun getCompletedTodayCount(): Flow<Int>
    suspend fun getMaxPosition(): Int
    suspend fun updatePositions(items: List<TodoItem>)
    
    // Subtasks
    suspend fun insertSubtasks(subtasks: List<TodoSubtask>)
    suspend fun updateSubtask(subtask: TodoSubtask)
    suspend fun deleteSubtask(subtask: TodoSubtask)
    suspend fun deleteSubtasksForTodo(todoId: Int)

    // Backup support (Bug C1)
    suspend fun getAllTodosList(): List<TodoItem>
    suspend fun getAllSubtasksList(): List<TodoSubtask>
    suspend fun deleteAllTodos()
}

class TodoRepositoryImpl(
    private val todoDao: TodoDao
) : TodoRepository {
    
    override fun getPagedTodos(): Flow<PagingData<TodoWithSubtasks>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { todoDao.getAllTodos() }
        ).flow
    }

    override fun getPagedActiveTodos(): Flow<PagingData<TodoWithSubtasks>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { todoDao.getActiveTodos() }
        ).flow
    }

    override fun getPagedCompletedTodos(): Flow<PagingData<TodoWithSubtasks>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { todoDao.getCompletedTodos() }
        ).flow
    }

    override fun getTodosByProject(projectId: Int): Flow<List<TodoWithSubtasks>> = todoDao.getTodosByProject(projectId)
    
    override suspend fun getTodoById(id: Int): TodoWithSubtasks? = todoDao.getTodoWithSubtasksById(id)
    
    override suspend fun insertTodo(todo: TodoItem): Long = todoDao.insertTodo(todo)
    
    override suspend fun updateTodo(todo: TodoItem) = todoDao.updateTodo(todo)
    
    override suspend fun deleteTodo(todo: TodoItem) = todoDao.deleteTodo(todo)
    
    override suspend fun deleteAllCompleted() = todoDao.deleteAllCompleted()
    
    override fun getActiveCount(): Flow<Int> = todoDao.getActiveCount()
    
    override fun getCompletedCount(): Flow<Int> = todoDao.getCompletedCount()

    override fun getCompletedTodayCount(): Flow<Int> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return todoDao.getCompletedTodayCount(calendar.timeInMillis)
    }

    override suspend fun getMaxPosition(): Int = todoDao.getMaxPosition() ?: 0

    override suspend fun updatePositions(items: List<TodoItem>) {
        items.forEachIndexed { index, todo ->
            todoDao.updatePosition(todo.id, index)
        }
    }

    override suspend fun insertSubtasks(subtasks: List<TodoSubtask>) = todoDao.insertSubtasks(subtasks)
    override suspend fun updateSubtask(subtask: TodoSubtask) = todoDao.updateSubtask(subtask)
    override suspend fun deleteSubtask(subtask: TodoSubtask) = todoDao.deleteSubtask(subtask)
    override suspend fun deleteSubtasksForTodo(todoId: Int) = todoDao.deleteSubtasksForTodo(todoId)

    override suspend fun getAllTodosList(): List<TodoItem> = todoDao.getAllTodosList()
    override suspend fun getAllSubtasksList(): List<TodoSubtask> = todoDao.getAllSubtasksList()
    override suspend fun deleteAllTodos() = todoDao.deleteAllTodos()
}
