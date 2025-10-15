package com.example.notenext.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notenext.data.NoteDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ArchiveViewModel(private val noteDao: NoteDao) : ViewModel() {

    val state: StateFlow<ArchiveState> = noteDao.getArchivedNotes()
        .map { ArchiveState(notes = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArchiveState()
        )
}
