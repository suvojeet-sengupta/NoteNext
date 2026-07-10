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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val reminderScheduler: com.suvojeet.notenext.data.ReminderScheduler
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

    fun deleteReminder(note: Note) {
        viewModelScope.launch {
            reminderScheduler.cancelNoteReminder(note)
            val updatedNote = note.copy(reminderTime = null, repeatOption = null)
            repository.updateNote(updatedNote)
        }
    }

    fun saveReminder(title: String, time: Long, repeatOption: String? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newNote = Note(
                title = title,
                content = "",
                createdAt = now,
                lastEdited = now,
                color = 0,
                reminderTime = time,
                repeatOption = repeatOption,
                isImportant = true // Default for standalone reminders
            )
            val id = repository.insertNote(newNote)
            val noteWithId = newNote.copy(id = id.toInt())
            reminderScheduler.scheduleNoteReminder(noteWithId)
        }
    }
}
