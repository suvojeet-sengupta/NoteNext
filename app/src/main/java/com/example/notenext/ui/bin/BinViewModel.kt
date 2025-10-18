package com.example.notenext.ui.bin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notenext.data.Note
import com.example.notenext.data.NoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BinViewModel(private val noteDao: NoteDao) : ViewModel() {

    private val _state = MutableStateFlow(BinState())
    val state = _state.asStateFlow()

    init {
        noteDao.getBinnedNotes()
            .onEach { notes ->
                _state.value = state.value.copy(notes = notes)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BinEvent) {
        when (event) {
            is BinEvent.RestoreNote -> {
                viewModelScope.launch {
                    noteDao.updateNote(event.note.copy(isBinned = false))
                }
            }
            is BinEvent.DeleteNotePermanently -> {
                viewModelScope.launch {
                    noteDao.deleteNote(event.note)
                }
            }
            is BinEvent.EmptyBin -> {
                viewModelScope.launch {
                    noteDao.emptyBin()
                }
            }
        }
    }
}
