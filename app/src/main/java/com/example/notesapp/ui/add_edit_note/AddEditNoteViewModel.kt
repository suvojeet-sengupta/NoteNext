
package com.example.notesapp.ui.add_edit_note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.data.Note
import com.example.notesapp.data.NoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEditNoteViewModel(private val noteDao: NoteDao, private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _state = MutableStateFlow(AddEditNoteState())
    val state = _state.asStateFlow()

    private val noteId: Int = savedStateHandle.get<Int>("noteId") ?: -1

    init {
        if (noteId != -1) {
            viewModelScope.launch {
                noteDao.getNoteById(noteId)?.let {
                    _state.value = state.value.copy(
                        title = it.title,
                        content = it.content,
                        isNewNote = false
                    )
                }
            }
        }
    }

    fun onEvent(event: AddEditNoteEvent) {
        when (event) {
            is AddEditNoteEvent.OnTitleChange -> {
                _state.value = state.value.copy(title = event.title)
            }
            is AddEditNoteEvent.OnContentChange -> {
                _state.value = state.value.copy(content = event.content)
            }
            is AddEditNoteEvent.OnSaveNoteClick -> {
                viewModelScope.launch {
                    val note = Note(
                        id = if (noteId == -1) 0 else noteId,
                        title = state.value.title,
                        content = state.value.content,
                        timestamp = System.currentTimeMillis()
                    )
                    noteDao.insertNote(note)
                    _state.value = state.value.copy(isNoteSaved = true)
                }
            }
        }
    }
}
