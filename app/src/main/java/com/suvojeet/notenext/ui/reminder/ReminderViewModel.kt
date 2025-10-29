package com.suvojeet.notenext.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReminderViewModel(private val noteDao: NoteDao) : ViewModel() {

    private val _reminders = MutableStateFlow<List<Note>>(emptyList())
    val reminders: StateFlow<List<Note>> = _reminders

    init {
        viewModelScope.launch {
            noteDao.getNotesWithReminders(System.currentTimeMillis()).collect { notes ->
                _reminders.value = notes
            }
        }
    }
}
