package com.suvojeet.notenext.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.TodoWithSubtasks
import com.suvojeet.notenext.data.TodoRepository
import com.suvojeet.notenext.data.repository.AiRepository
import com.suvojeet.notenext.data.repository.AiResult
import com.suvojeet.notenext.data.repository.onFailure
import com.suvojeet.notenext.data.repository.onSuccess
import com.suvojeet.notenext.data.repository.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TodoUiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : TodoUiEvent()
    data class ShareTodo(val title: String, val content: String) : TodoUiEvent()
}

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository,
    private val noteRepository: com.suvojeet.notenext.data.NoteRepository,
    private val aiRepository: AiRepository,
    private val reminderScheduler: com.suvojeet.notenext.data.ReminderScheduler,
    private val todoUseCases: com.suvojeet.notenext.domain.use_case.todo.TodoUseCases,
    private val draftRepository: com.suvojeet.notenext.data.repository.DraftRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TodoState())
    val state: StateFlow<TodoState> = _state.asStateFlow()

    val projects = noteRepository.getProjects()

    private val _filter = MutableStateFlow<TodoFilter>(TodoFilter.All)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagedTodos: Flow<PagingData<TodoWithSubtasks>> = _filter.flatMapLatest { filter ->
        when (filter) {
            is TodoFilter.All -> repository.getPagedTodos()
            is TodoFilter.Active -> repository.getPagedActiveTodos()
            is TodoFilter.Completed -> repository.getPagedCompletedTodos()
        }
    }.cachedIn(viewModelScope)

    private val _events = MutableSharedFlow<TodoUiEvent>()
    val events: SharedFlow<TodoUiEvent> = _events.asSharedFlow()

    init {
        observeTodos()
    }

    private fun observeTodos() {
        combine(
            repository.getActiveCount(),
            repository.getCompletedCount(),
            repository.getCompletedTodayCount(),
            _filter
        ) { activeCount, completedCount, completedTodayCount, filter ->
            _state.value = _state.value.copy(
                isLoading = false,
                activeCount = activeCount,
                completedCount = completedCount,
                completedTodayCount = completedTodayCount,
                filter = filter
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: TodoEvent) {
        when (event) {
            is TodoEvent.AddTodo -> {
                viewModelScope.launch {
                    todoUseCases.saveTodo(
                        todoId = null,
                        title = event.title,
                        description = event.description,
                        priority = event.priority,
                        dueDate = event.dueDate,
                        reminderTime = event.reminderTime,
                        projectId = null,
                        subtasks = emptyList()
                    )
                }
            }
            is TodoEvent.UpdateTodo -> {
                viewModelScope.launch {
                    todoUseCases.saveTodo(
                        todoId = event.todo.id,
                        title = event.todo.title,
                        description = event.todo.description,
                        priority = event.todo.priority,
                        dueDate = event.todo.dueDate,
                        reminderTime = event.todo.reminderTime,
                        projectId = event.todo.projectId,
                        subtasks = emptyList() // Subtasks handled separately in Dialog Save
                    )
                }
            }
            is TodoEvent.DeleteTodo -> {
                viewModelScope.launch {
                    repository.deleteTodo(event.todo)
                    reminderScheduler.cancelTodoReminder(event.todo)
                }
            }
            is TodoEvent.ToggleComplete -> {
                viewModelScope.launch {
                    todoUseCases.completeTodo(event.todo, !event.todo.isCompleted)
                }
            }
            is TodoEvent.SetFilter -> {
                _filter.value = event.filter
            }
            is TodoEvent.DeleteAllCompleted -> {
                viewModelScope.launch {
                    repository.deleteAllCompleted()
                }
            }
            is TodoEvent.ShowAddDialog -> {
                viewModelScope.launch {
                    val draft = draftRepository.todoDraft.first()
                    _state.value = _state.value.copy(
                        showAddEditDialog = true,
                        editingTodo = if (!draft.isNullOrBlank()) {
                            TodoItem(title = draft.substringBefore("\n"), description = draft.substringAfter("\n", ""), priority = 0, position = 0, createdAt = 0)
                        } else null,
                        editingSubtasks = emptyList()
                    )
                }
            }
            is TodoEvent.ShowEditDialog -> {
                viewModelScope.launch {
                    val todoWithSubtasks = repository.getTodoById(event.todo.id)
                    _state.value = _state.value.copy(
                        showAddEditDialog = true,
                        editingTodo = event.todo,
                        editingSubtasks = todoWithSubtasks?.subtasks ?: emptyList()
                    )
                }
            }
            is TodoEvent.DismissDialog -> {
                _state.value = _state.value.copy(
                    showAddEditDialog = false,
                    editingTodo = null,
                    editingSubtasks = emptyList()
                )
            }
            is TodoEvent.OnDraftChange -> {
                viewModelScope.launch {
                    draftRepository.saveTodoDraft(event.content)
                }
            }
            is TodoEvent.SaveTodo -> {
                viewModelScope.launch {
                    todoUseCases.saveTodo(
                        todoId = _state.value.editingTodo?.id,
                        title = event.title,
                        description = event.description,
                        priority = event.priority,
                        dueDate = event.dueDate,
                        reminderTime = event.reminderTime,
                        projectId = event.projectId,
                        subtasks = event.subtasks
                    )
                    draftRepository.clearTodoDraft()
                    _state.value = _state.value.copy(
                        showAddEditDialog = false,
                        editingTodo = null,
                        editingSubtasks = emptyList()
                    )
                }
            }
            is TodoEvent.ShowAiTodoDialog -> {
                _state.value = _state.value.copy(showAiTodoDialog = true)
            }
            is TodoEvent.DismissAiTodoDialog -> {
                _state.value = _state.value.copy(showAiTodoDialog = false)
            }
            is TodoEvent.GenerateAiTodos -> {
                viewModelScope.launch {
                    try {
                        aiRepository.generateTodos(event.input)
                            .onStart { _state.value = _state.value.copy(isGenerating = true) }
                            .collect { result ->
                                result.onSuccess { todos ->
                                    val maxPos = repository.getMaxPosition()
                                    todos.forEachIndexed { index, (title, description) ->
                                        val todo = TodoItem(
                                            title = title,
                                            description = description,
                                            priority = 1,
                                            position = maxPos + index + 1,
                                            createdAt = System.currentTimeMillis()
                                        )
                                        repository.insertTodo(todo)
                                    }
                                    _state.value = _state.value.copy(
                                        isGenerating = false,
                                        showAiTodoDialog = false
                                    )
                                    _events.emit(TodoUiEvent.ShowSnackbar("Successfully generated ${todos.size} tasks"))
                                }.onFailure { failure ->
                                    val errorMessage = failure.toUserMessage("Failed to generate tasks.")
                                    _state.value = _state.value.copy(isGenerating = false)
                                    _events.emit(TodoUiEvent.ShowSnackbar(errorMessage, actionLabel = "Retry", onAction = { onEvent(TodoEvent.GenerateAiTodos(event.input)) }))
                                }
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        e.printStackTrace()
                        _state.value = _state.value.copy(isGenerating = false)
                    }
                }
            }
            is TodoEvent.UpdatePositions -> {
                viewModelScope.launch {
                    repository.updatePositions(event.items)
                }
            }
            is TodoEvent.ConvertToNote -> {
                viewModelScope.launch {
                    val todo = event.todo.todo
                    val subtasks = event.todo.subtasks
                    
                    val currentTime = System.currentTimeMillis()
                    val note = com.suvojeet.notenext.data.Note(
                        title = todo.title,
                        content = todo.description,
                        createdAt = currentTime,
                        lastEdited = currentTime,
                        color = 0,
                        noteType = if (subtasks.isNotEmpty()) com.suvojeet.notenext.core.model.NoteType.CHECKLIST else com.suvojeet.notenext.core.model.NoteType.TEXT,
                        projectId = todo.projectId
                    )
                    
                    val noteId = noteRepository.insertNote(note).toInt()
                    
                    if (subtasks.isNotEmpty()) {
                        val checklistItems = subtasks.mapIndexed { index, subtask ->
                            com.suvojeet.notenext.data.ChecklistItem(
                                noteId = noteId,
                                text = subtask.text,
                                isChecked = subtask.isChecked,
                                position = index
                            )
                        }
                        noteRepository.insertChecklistItems(checklistItems)
                    }
                    
                    repository.deleteTodo(todo)
                    reminderScheduler.cancelTodoReminder(todo)
                    _events.emit(TodoUiEvent.ShowSnackbar("Converted to Note"))
                }
            }
            is TodoEvent.ShareTodo -> {
                viewModelScope.launch {
                    val todo = event.todo.todo
                    val subtasks = event.todo.subtasks
                    
                    val sb = StringBuilder()
                    if (todo.description.isNotBlank()) {
                        sb.append(todo.description).append("\n\n")
                    }
                    
                    if (subtasks.isNotEmpty()) {
                        subtasks.forEach { subtask ->
                            val status = if (subtask.isChecked) "[x]" else "[ ]"
                            sb.append("$status ${subtask.text}\n")
                        }
                    }
                    
                    _events.emit(TodoUiEvent.ShareTodo(todo.title, sb.toString()))
                }
            }
        }
    }
}
