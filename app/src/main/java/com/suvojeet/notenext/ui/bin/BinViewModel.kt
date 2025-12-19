package com.suvojeet.notenext.ui.bin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BinViewModel @Inject constructor(private val repository: com.suvojeet.notenext.data.NoteRepository, private val savedStateHandle: androidx.lifecycle.SavedStateHandle) : ViewModel() {

    private val _state = MutableStateFlow(BinState())
    val state = _state.asStateFlow()

    init {
        repository.getBinnedNotes()
            .onEach { list ->
                _state.value = state.value.copy(notes = list.map { it.note })
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BinEvent) {
        when (event) {
            is BinEvent.RestoreNote -> {
                viewModelScope.launch {
                    try {
                        repository.updateNote(event.note.copy(isBinned = false, binnedOn = null))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            is BinEvent.DeleteNotePermanently -> {
                viewModelScope.launch {
                    try {
                        repository.deleteNote(event.note)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            is BinEvent.EmptyBin -> {
                viewModelScope.launch {
                    try {
                        repository.emptyBin()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            is BinEvent.ToggleNoteSelection -> {
                val selectedIds = _state.value.selectedNoteIds.toMutableSet()
                if (selectedIds.contains(event.noteId)) {
                    selectedIds.remove(event.noteId)
                } else {
                    selectedIds.add(event.noteId)
                }
                _state.value = _state.value.copy(selectedNoteIds = selectedIds)
            }
            is BinEvent.ClearSelection -> {
                _state.value = _state.value.copy(selectedNoteIds = emptySet())
            }
            is BinEvent.RestoreSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = _state.value.notes.filter { _state.value.selectedNoteIds.contains(it.id) }
                    selectedNotes.forEach { note ->
                        repository.updateNote(note.copy(isBinned = false, binnedOn = null))
                    }
                    _state.value = _state.value.copy(selectedNoteIds = emptySet())
                }
            }
            is BinEvent.DeleteSelectedNotesPermanently -> {
                viewModelScope.launch {
                    val selectedNotes = _state.value.notes.filter { _state.value.selectedNoteIds.contains(it.id) }
                    selectedNotes.forEach { note ->
                        repository.deleteNote(note)
                    }
                    _state.value = _state.value.copy(selectedNoteIds = emptySet())
                }
            }
            is BinEvent.ExpandNote -> {
                _state.value = _state.value.copy(expandedNoteId = event.noteId)
            }
            is BinEvent.CollapseNote -> {
                _state.value = _state.value.copy(expandedNoteId = null)
            }
        }
    }
}
