package com.example.notenext.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notenext.data.Note
import com.example.notenext.data.NoteDao
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
        noteDao.getNotes().onEach { notes ->
            val labels = notes.mapNotNull { it.label }.filter { it.isNotEmpty() }.distinct()
            _state.value = state.value.copy(
                notes = notes,
                labels = labels
            )
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
            is NotesEvent.SetLabelForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.copy(label = event.label))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ExpandNote -> {
                viewModelScope.launch {
                    if (event.noteId != -1) {
                        noteDao.getNoteById(event.noteId)?.let { note ->
                            _state.value = state.value.copy(
                                expandedNoteId = event.noteId,
                                editingTitle = note.title,
                                editingContent = note.content,
                                editingColor = note.color,
                                editingIsNewNote = false,
                                editingLastEdited = note.lastEdited,
                                editingHistory = listOf(note.title to note.content),
                                editingHistoryIndex = 0
                            )
                        }
                    } else {
                        _state.value = state.value.copy(
                            expandedNoteId = -1,
                            editingTitle = "",
                            editingContent = "",
                            editingColor = 0,
                            editingIsNewNote = true,
                            editingLastEdited = 0,
                            editingHistory = listOf("" to ""),
                            editingHistoryIndex = 0
                        )
                    }
                }
            }
            is NotesEvent.CollapseNote -> {
                viewModelScope.launch {
                    onEvent(NotesEvent.OnSaveNoteClick)
                    _state.value = state.value.copy(expandedNoteId = null)
                }
            }
            is NotesEvent.OnTitleChange -> {
                val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (event.title to state.value.editingContent)
                _state.value = state.value.copy(
                    editingTitle = event.title,
                    editingHistory = newHistory,
                    editingHistoryIndex = newHistory.lastIndex
                )
            }
            is NotesEvent.OnContentChange -> {
                val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to event.content)
                _state.value = state.value.copy(
                    editingContent = event.content,
                    editingHistory = newHistory,
                    editingHistoryIndex = newHistory.lastIndex
                )
            }
            is NotesEvent.OnColorChange -> {
                _state.value = state.value.copy(editingColor = event.color)
            }
            is NotesEvent.OnUndoClick -> {
                if (state.value.editingHistoryIndex > 0) {
                    val newIndex = state.value.editingHistoryIndex - 1
                    val (title, content) = state.value.editingHistory[newIndex]
                    _state.value = state.value.copy(
                        editingTitle = title,
                        editingContent = content,
                        editingHistoryIndex = newIndex
                    )
                }
            }
            is NotesEvent.OnRedoClick -> {
                if (state.value.editingHistoryIndex < state.value.editingHistory.lastIndex) {
                    val newIndex = state.value.editingHistoryIndex + 1
                    val (title, content) = state.value.editingHistory[newIndex]
                    _state.value = state.value.copy(
                        editingTitle = title,
                        editingContent = content,
                        editingHistoryIndex = newIndex
                    )
                }
            }
            is NotesEvent.OnSaveNoteClick -> {
                viewModelScope.launch {
                    val currentTime = System.currentTimeMillis()
                    val noteId = state.value.expandedNoteId
                    if (noteId == null || (state.value.editingTitle.isEmpty() && state.value.editingContent.isEmpty())) {
                        if (noteId != -1) { // Don't delete if it was a new note
                            // If the note is empty, delete it
                            state.value.expandedNoteId?.let { id ->
                                noteDao.getNoteById(id)?.let { noteDao.deleteNote(it) }
                            }
                        }
                        _state.value = state.value.copy(expandedNoteId = null)
                        return@launch
                    }


                    val note = if (noteId == -1) {
                        Note(
                            title = state.value.editingTitle,
                            content = state.value.editingContent,
                            createdAt = currentTime,
                            lastEdited = currentTime,
                            color = state.value.editingColor
                        )
                    } else {
                        Note(
                            id = noteId,
                            title = state.value.editingTitle,
                            content = state.value.editingContent,
                            createdAt = noteDao.getNoteById(noteId)?.createdAt ?: System.currentTimeMillis(),
                            lastEdited = currentTime,
                            color = state.value.editingColor
                        )
                    }
                    noteDao.insertNote(note)
                    _state.value = state.value.copy(expandedNoteId = null)
                }
            }
            is NotesEvent.OnDeleteNoteClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let {
                        if (it != -1) {
                            noteDao.getNoteById(it)?.let { note ->
                                noteDao.deleteNote(note)
                            }
                        }
                    }
                    _state.value = state.value.copy(expandedNoteId = null)
                }
            }
        }
    }
}