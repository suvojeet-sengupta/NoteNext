package com.example.notesapp.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.data.Note
import com.example.notesapp.data.NoteDao
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NotesViewModel(private val noteDao: NoteDao) : ViewModel() {

    private val _state = MutableStateFlow(NotesState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events = _events.asSharedFlow()

    private var recentlyDeletedNote: Note? = null

    init {
        noteDao.getNotes().onEach {
            _state.value = state.value.copy(notes = it)
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.DeleteNote -> {
                viewModelScope.launch {
                    noteDao.deleteNote(event.note)
                    recentlyDeletedNote = event.note
                }
            }
            is NotesEvent.RestoreNote -> {
                viewModelScope.launch {
                    noteDao.insertNote(recentlyDeletedNote ?: return@launch)
                    recentlyDeletedNote = null
                }
            }
            is NotesEvent.ToggleNoteSelection -> {
                val selectedIds = state.value.selectedNoteIds.toMutableList()
                if (selectedIds.contains(event.noteId)) {
                    selectedIds.remove(event.noteId)
                } else {
                    selectedIds.add(event.noteId)
                }
                _state.value = state.value.copy(selectedNoteIds = selectedIds)
            }
            is NotesEvent.ClearSelection -> {
                _state.value = state.value.copy(selectedNoteIds = emptyList())
            }
            is NotesEvent.TogglePinForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.copy(isPinned = !note.isPinned))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.DeleteSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.id) }
                    for (note in selectedNotes) {
                        noteDao.deleteNote(note)
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ArchiveSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.copy(isArchived = !note.isArchived))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ToggleImportantForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.copy(isImportant = !note.isImportant))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ChangeColorForSelectedNotes -> {
                // TODO: Implement color picker
            }
            is NotesEvent.CopySelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.copy(id = 0, title = "${note.title} (Copy)"))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.SendSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.id) }
                    if (selectedNotes.isNotEmpty()) {
                        val title = if (selectedNotes.size == 1) selectedNotes.first().title else "Multiple Notes"
                        val content = selectedNotes.joinToString("\n\n---\n\n") { "Title: ${it.title}\n\n${it.content}" }
                        _events.emit(NotesUiEvent.SendNotes(title, content))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.SetReminderForSelectedNotes -> {
                // TODO: Implement reminder
            }
        }
    }
}