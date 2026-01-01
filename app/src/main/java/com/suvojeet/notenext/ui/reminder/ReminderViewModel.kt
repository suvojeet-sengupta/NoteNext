package com.suvojeet.notenext.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _allReminders = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<Note>> = _allReminders

    val upcomingReminders: StateFlow<List<Note>> = _allReminders.map { notes ->
        val now = System.currentTimeMillis()
        notes.filter { (it.reminderTime ?: 0L) > now }
            .sortedBy { it.reminderTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val elapsedReminders: StateFlow<List<Note>> = _allReminders.map { notes ->
        val now = System.currentTimeMillis()
        notes.filter { (it.reminderTime ?: 0L) <= now }
            .sortedByDescending { it.reminderTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

}
