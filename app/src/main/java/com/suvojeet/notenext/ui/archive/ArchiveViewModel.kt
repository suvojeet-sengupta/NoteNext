package com.suvojeet.notenext.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.NoteDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(private val noteDao: NoteDao) : ViewModel() {

    val state: StateFlow<ArchiveState> = noteDao.getArchivedNotes()
        .map { list -> list.map { it.note } }
        .map { ArchiveState(notes = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArchiveState()
        )

    fun onEvent(event: ArchiveEvent) {
        when (event) {
            is ArchiveEvent.UnarchiveNote -> {
                viewModelScope.launch {
                    noteDao.insertNote(event.note.copy(isArchived = false))
                }
            }
        }
    }
}
