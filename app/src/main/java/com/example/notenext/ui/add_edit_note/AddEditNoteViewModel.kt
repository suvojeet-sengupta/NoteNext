
package com.example.notenext.ui.add_edit_note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notenext.data.Note
import com.example.notenext.data.NoteDao
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEditNoteViewModel(private val noteDao: NoteDao, private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _state = MutableStateFlow(AddEditNoteState())
    val state = _state.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AddEditNoteUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val noteId: Int = savedStateHandle.get<Int>("noteId") ?: -1

    init {
        if (noteId != -1) {
            viewModelScope.launch {
                noteDao.getNoteById(noteId)?.let {
                    _state.value = state.value.copy(
                        title = it.title,
                        content = it.content,
                        color = it.color,
                        isNewNote = false,
                        lastEdited = it.lastEdited,
                        history = listOf(it.title to it.content),
                        historyIndex = 0
                    )
                }
            }
        }
    }

    fun onEvent(event: AddEditNoteEvent) {
        when (event) {
            is AddEditNoteEvent.OnTitleChange -> {
                val newHistory = state.value.history.take(state.value.historyIndex + 1) + (event.title to state.value.content)
                _state.value = state.value.copy(
                    title = event.title,
                    history = newHistory,
                    historyIndex = newHistory.lastIndex
                )
            }
            is AddEditNoteEvent.OnContentChange -> {
                val newHistory = state.value.history.take(state.value.historyIndex + 1) + (state.value.title to event.content)
                _state.value = state.value.copy(
                    content = event.content,
                    history = newHistory,
                    historyIndex = newHistory.lastIndex
                )
            }
            is AddEditNoteEvent.OnColorChange -> {
                _state.value = state.value.copy(color = event.color)
            }
            is AddEditNoteEvent.OnUndoClick -> {
                if (state.value.historyIndex > 0) {
                    val newIndex = state.value.historyIndex - 1
                    val (title, content) = state.value.history[newIndex]
                    _state.value = state.value.copy(
                        title = title,
                        content = content,
                        historyIndex = newIndex
                    )
                }
            }
            is AddEditNoteEvent.OnRedoClick -> {
                if (state.value.historyIndex < state.value.history.lastIndex) {
                    val newIndex = state.value.historyIndex + 1
                    val (title, content) = state.value.history[newIndex]
                    _state.value = state.value.copy(
                        title = title,
                        content = content,
                        historyIndex = newIndex
                    )
                }
            }
            is AddEditNoteEvent.OnSaveNoteClick -> {
                viewModelScope.launch {
                    val currentTime = System.currentTimeMillis()
                    val note = if (noteId == -1) {
                        Note(
                            title = state.value.title,
                            content = state.value.content,
                            createdAt = currentTime,
                            lastEdited = currentTime,
                            color = state.value.color
                        )
                    } else {
                        Note(
                            id = noteId,
                            title = state.value.title,
                            content = state.value.content,
                            createdAt = noteDao.getNoteById(noteId)?.createdAt ?: System.currentTimeMillis(),
                            lastEdited = currentTime,
                            color = state.value.color
                        )
                    }
                    noteDao.insertNote(note)
                    _uiEvent.emit(AddEditNoteUiEvent.OnNoteSaved)
                }
            }
            is AddEditNoteEvent.OnDeleteNoteClick -> {
                viewModelScope.launch {
                    if (noteId != -1) {
                        noteDao.getNoteById(noteId)?.let { 
                            noteDao.deleteNote(it)
                            _uiEvent.emit(AddEditNoteUiEvent.OnNoteSaved)
                        }
                    }
                }
            }
        }
    }
}
