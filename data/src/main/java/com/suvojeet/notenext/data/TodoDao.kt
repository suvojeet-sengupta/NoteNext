package com.suvojeet.notenext.data

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Transaction
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, position ASC, priority DESC, dueDate ASC, createdAt DESC")
    fun getAllTodos(): PagingSource<Int, TodoWithSubtasks>

    @Transaction
    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY position ASC, priority DESC, dueDate ASC, createdAt DESC")
    fun getActiveTodos(): PagingSource<Int, TodoWithSubtasks>

    @Transaction
    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTodos(): PagingSource<Int, TodoWithSubtasks>

    @Transaction
    @Query("SELECT * FROM todos WHERE projectId = :projectId ORDER BY isCompleted ASC, position ASC")
    fun getTodosByProject(projectId: Int): Flow<List<TodoWithSubtasks>>

    @Query("SELECT MAX(position) FROM todos")
    suspend fun getMaxPosition(): Int?

    @Query("UPDATE todos SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Int, position: Int)

    @Transaction
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoWithSubtasksById(id: Int): TodoWithSubtasks?

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Int): TodoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<TodoSubtask>)

    @Update
    suspend fun updateSubtask(subtask: TodoSubtask)

    @Delete
    suspend fun deleteSubtask(subtask: TodoSubtask)

    @Query("DELETE FROM todo_subtasks WHERE todoId = :todoId")
    suspend fun deleteSubtasksForTodo(todoId: Int)

    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun deleteAllCompleted()

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0")
    fun getActiveCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1 AND completedAt >= :startOfDay")
    fun getCompletedTodayCount(startOfDay: Long): Flow<Int>

    // Backup support (Bug C1): one-shot snapshots for inclusion in backup ZIPs.
    @Query("SELECT * FROM todos")
    suspend fun getAllTodosList(): List<TodoItem>

    @Query("SELECT * FROM todo_subtasks")
    suspend fun getAllSubtasksList(): List<TodoSubtask>

    // Backup support: wipe all todos on non-merge restore. Subtasks cascade via FK.
    @Query("DELETE FROM todos")
    suspend fun deleteAllTodos()
}
