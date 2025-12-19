package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.ui.notes.HtmlConverter
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.ProjectDao
import com.suvojeet.notenext.data.SortType
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.util.AlarmScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import com.suvojeet.notenext.ui.reminder.RepeatOption
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest

import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.NoteWithAttachments
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val alarmScheduler: AlarmScheduler,
    private val richTextController: RichTextController
) : ViewModel() {

    private val _state = MutableStateFlow(NotesState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events = _events.asSharedFlow()

    private var recentlyDeletedNote: Note? = null

    private val _searchQuery = MutableStateFlow("")
    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED)

    init {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        combine(
            combine(_searchQuery, _sortType) { query, sortType -> query to sortType }
                .flatMapLatest { (query, sortType) ->
                    repository.getNotes(query, sortType)
                },
            repository.getLabels(),
            repository.getProjects(),
            _sortType,
            _searchQuery
        ) { notes, labels, projects, sortType, query ->
            _state.value = _state.value.copy(
                notes = notes,
                labels = labels.map { it.name },
                projects = projects,
                isLoading = false,
                sortType = sortType,
                searchQuery = query
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.OnSearchQueryChange -> {
                _searchQuery.value = event.query
            }
            is NotesEvent.DeleteNote -> {
                viewModelScope.launch {
                    val noteToBin = event.note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())
                    repository.updateNote(noteToBin)
                    recentlyDeletedNote = event.note.note
                    _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
                }
            }
            is NotesEvent.RestoreNote -> {
                viewModelScope.launch {
                    recentlyDeletedNote?.let { restoredNote ->
                        repository.updateNote(restoredNote.copy(isBinned = false))
                        recentlyDeletedNote = null
                    }
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
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    val areNotesBeingPinned = selectedNotes.firstOrNull()?.note?.isPinned == false
                    for (note in selectedNotes) {
                        repository.insertNote(note.note.copy(isPinned = areNotesBeingPinned))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    val message = if (areNotesBeingPinned) {
                        if (selectedNotes.size > 1) "${selectedNotes.size} notes pinned" else "Note pinned"
                    } else {
                        if (selectedNotes.size > 1) "${selectedNotes.size} notes unpinned" else "Note unpinned"
                    }
                    _events.emit(NotesUiEvent.ShowToast(message))
                }
            }
            is NotesEvent.DeleteSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    _events.emit(NotesUiEvent.ShowToast("${selectedNotes.size} notes moved to Bin"))
                }
            }
            is NotesEvent.ArchiveSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.insertNote(note.note.copy(isArchived = !note.note.isArchived))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ToggleImportantForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.insertNote(note.note.copy(isImportant = !note.note.isImportant))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ChangeColorForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.insertNote(note.note.copy(color = event.color))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    _events.emit(NotesUiEvent.ShowToast("Color updated"))
                }
            }
            is NotesEvent.CopySelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (noteWithAttachments in selectedNotes) {
                        val copiedNote = noteWithAttachments.note.copy(id = 0, title = "${noteWithAttachments.note.title} (Copy)")
                        val newNoteId = repository.insertNote(copiedNote)
                        noteWithAttachments.attachments.forEach { attachment ->
                            repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId.toInt()))
                        }
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    val message = if (selectedNotes.size > 1) "${selectedNotes.size} notes copied" else "Note copied"
                    _events.emit(NotesUiEvent.ShowToast(message))
                }
            }
            is NotesEvent.SendSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    if (selectedNotes.isNotEmpty()) {
                        val title = if (selectedNotes.size == 1) selectedNotes.first().note.title else "Multiple Notes"
                        val content = selectedNotes.joinToString("\n\n---\n\n") { "Title: ${it.note.title}\n\n${HtmlConverter.htmlToPlainText(it.note.content)}" }
                        _events.emit(NotesUiEvent.SendNotes(title, content))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.SetReminderForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    val reminderDateTime = LocalDateTime.of(event.date, event.time)
                    val reminderMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    for (noteWithAttachments in selectedNotes) {
                        val updatedNote = noteWithAttachments.note.copy(
                            reminderTime = reminderMillis,
                            repeatOption = event.repeatOption.name // Store enum name as string
                        )
                        repository.updateNote(updatedNote)
                        alarmScheduler.schedule(updatedNote)
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    _events.emit(NotesUiEvent.ShowToast("Reminder set for ${selectedNotes.size} notes"))
                }
            }
            is NotesEvent.SetLabelForSelectedNotes -> {
                viewModelScope.launch {
                    if (event.label.isNotBlank()) {
                        repository.insertLabel(Label(event.label))
                    }
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.insertNote(note.note.copy(label = event.label))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ExpandNote -> {
                viewModelScope.launch {
                    if (event.noteId != -1) {
                        repository.getNoteById(event.noteId)?.let { noteWithAttachments ->
                            val note = noteWithAttachments.note
                            val content = if (note.noteType == "TEXT") {
                                HtmlConverter.htmlToAnnotatedString(note.content)
                            } else {
                                AnnotatedString("")
                            }
                            val checklist = if (note.noteType == "CHECKLIST") {
                                try {
                                    Gson().fromJson(note.content, object : TypeToken<List<ChecklistItem>>() {}.type)
                                } catch (e: Exception) {
                                    emptyList<ChecklistItem>()
                                }
                            } else {
                                emptyList<ChecklistItem>()
                            }
                            _state.value = state.value.copy(
                                expandedNoteId = event.noteId,
                                editingTitle = note.title,
                                editingContent = TextFieldValue(content),
                                editingColor = note.color,
                                editingIsNewNote = false,
                                editingLastEdited = note.lastEdited,
                                isPinned = note.isPinned,
                                isArchived = note.isArchived,
                                editingLabel = note.label,
                                editingHistory = listOf(note.title to TextFieldValue(content)),
                                editingHistoryIndex = 0,
                                linkPreviews = note.linkPreviews,
                                editingNoteType = note.noteType,
                                editingChecklist = checklist,
                                editingAttachments = noteWithAttachments.attachments.map { it.copy(tempId = java.util.UUID.randomUUID().toString()) }
                            )
                        }
                    } else {
                        _state.value = state.value.copy(
                            expandedNoteId = -1,
                            editingTitle = "",
                            editingContent = TextFieldValue(),
                            editingColor = 0,
                            editingIsNewNote = true,
                            editingLastEdited = 0,
                            editingHistory = listOf("" to TextFieldValue()),
                            editingHistoryIndex = 0,
                            editingLabel = null,
                            linkPreviews = emptyList(),
                            editingNoteType = event.noteType,
                            editingChecklist = if (event.noteType == "CHECKLIST") listOf(ChecklistItem(text = "", isChecked = false)) else emptyList(),
                            editingAttachments = emptyList()
                        )
                    }
                }
            }
            is NotesEvent.CollapseNote -> {
                onEvent(NotesEvent.OnSaveNoteClick)
            }
            is NotesEvent.AddChecklistItem -> {
                val newItem = ChecklistItem(text = "", isChecked = false)
                val updatedChecklist = state.value.editingChecklist + newItem
                _state.value = state.value.copy(
                    editingChecklist = updatedChecklist,
                    newlyAddedChecklistItemId = newItem.id
                )
            }
            is NotesEvent.DeleteChecklistItem -> {
                val updatedChecklist = state.value.editingChecklist.filterNot { it.id == event.itemId }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
            }
            is NotesEvent.OnChecklistItemCheckedChange -> {
                val updatedChecklist = state.value.editingChecklist.toMutableList()
                val index = updatedChecklist.indexOfFirst { it.id == event.itemId }
                if (index != -1) {
                    val item = updatedChecklist.removeAt(index).copy(isChecked = event.isChecked)
                    if (event.isChecked) {
                        updatedChecklist.add(item) // Add to the end if checked
                    } else {
                        // Find the first checked item and insert before it, or at the end if no checked items
                        val firstCheckedIndex = updatedChecklist.indexOfFirst { it.isChecked }
                        if (firstCheckedIndex != -1) {
                            updatedChecklist.add(firstCheckedIndex, item)
                        } else {
                            updatedChecklist.add(0, item) // Add to the beginning if no checked items
                        }
                    }
                }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
            }
            is NotesEvent.OnChecklistItemTextChange -> {
                val updatedChecklist = state.value.editingChecklist.map {
                    if (it.id == event.itemId) it.copy(text = event.text) else it
                }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
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
                if (state.value.editingNoteType == "TEXT") {
                    val newContent = event.content
                    val oldContent = state.value.editingContent

                    val finalContent = richTextController.processContentChange(
                        oldContent,
                        newContent,
                        state.value.activeStyles,
                        state.value.activeHeadingStyle
                    )

                    val selection = finalContent.selection
                    val styles = if (selection.collapsed) {
                        if (selection.start > 0) {
                            finalContent.annotatedString.spanStyles.filter {
                                it.start <= selection.start - 1 && it.end >= selection.start
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        finalContent.annotatedString.spanStyles.filter {
                            maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                        }
                    }
                    val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to finalContent)
                    _state.value = state.value.copy(
                        editingContent = finalContent,
                        editingHistory = newHistory,
                        editingHistoryIndex = newHistory.lastIndex,
                        isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                        isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                        isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                    )

                    // Link detection
                    val urlRegex = "(https?://[\\w.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?)".toRegex()
                    val detectedUrls = urlRegex.findAll(finalContent.text).map { it.value }.toSet() // Use Set for efficient lookup

                    val existingLinkPreviews = state.value.linkPreviews.filter { detectedUrls.contains(it.url) }
                    val newUrlsToFetch = detectedUrls.filter { url -> existingLinkPreviews.none { it.url == url } }

                    viewModelScope.launch {
                        val fetchedNewLinkPreviews = newUrlsToFetch.map { url ->
                            async { linkPreviewRepository.getLinkPreview(url) }
                        }.awaitAll()

                        val combinedLinkPreviews = (existingLinkPreviews + fetchedNewLinkPreviews).distinctBy { it.url }

                        _state.value = _state.value.copy(linkPreviews = combinedLinkPreviews)
                    }
                }
            }
            is NotesEvent.ApplyStyleToContent -> {
                val result = richTextController.toggleStyle(
                    state.value.editingContent,
                    event.style,
                    state.value.activeStyles,
                    state.value.isBoldActive,
                    state.value.isItalicActive,
                    state.value.isUnderlineActive
                )

                if (result.updatedActiveStyles != null) {
                    val activeStyles = result.updatedActiveStyles
                    _state.value = state.value.copy(
                        activeStyles = activeStyles,
                        isBoldActive = activeStyles.any { it.fontWeight == FontWeight.Bold },
                        isItalicActive = activeStyles.any { it.fontStyle == FontStyle.Italic },
                        isUnderlineActive = activeStyles.any { it.textDecoration == TextDecoration.Underline }
                    )
                } else if (result.updatedContent != null) {
                    val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to result.updatedContent)
                    _state.value = state.value.copy(
                        editingContent = result.updatedContent,
                        editingHistory = newHistory,
                        editingHistoryIndex = newHistory.lastIndex
                    )
                }
            }
            is NotesEvent.ApplyHeadingStyle -> {
                val updatedContent = richTextController.applyHeading(state.value.editingContent, event.level)

                if (updatedContent == null) {
                    // Selection is collapsed, update active styles for future typing
                    val newActiveStyles = mutableSetOf<SpanStyle>()
                    if (event.level != 0) {
                        newActiveStyles.add(richTextController.getHeadingStyle(event.level))
                    }
                    _state.value = state.value.copy(
                        activeHeadingStyle = event.level,
                        activeStyles = newActiveStyles,
                        isBoldActive = false,
                        isItalicActive = false,
                        isUnderlineActive = false
                    )
                } else {
                    // Applied to selection
                    val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to updatedContent)

                    _state.value = state.value.copy(
                        editingContent = updatedContent,
                        editingHistory = newHistory,
                        editingHistoryIndex = newHistory.lastIndex,
                        activeHeadingStyle = event.level
                    )
                }
            }
            is NotesEvent.OnColorChange -> {
                _state.value = state.value.copy(editingColor = event.color)
            }
            is NotesEvent.OnLabelChange -> {
                viewModelScope.launch {
                    repository.insertLabel(Label(event.label))
                    _state.value = state.value.copy(editingLabel = event.label)
                }
            }
            is NotesEvent.OnTogglePinClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let { noteId ->
                        repository.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isPinned = !note.note.isPinned)
                            repository.insertNote(updatedNote)
                            val updatedNotesList = state.value.notes.map { if (it.note.id == updatedNote.id) it.copy(note = updatedNote) else it }
                            _state.value = state.value.copy(
                                isPinned = updatedNote.isPinned,
                                notes = updatedNotesList
                            )
                            val message = if (updatedNote.isPinned) "Note pinned" else "Note unpinned"
                            _events.emit(NotesUiEvent.ShowToast(message))
                        }
                    }
                }
            }
            is NotesEvent.OnToggleArchiveClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let { noteId ->
                        repository.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isArchived = !note.note.isArchived)
                            repository.insertNote(updatedNote)
                            val updatedNotesList = state.value.notes.map { if (it.note.id == updatedNote.id) it.copy(note = updatedNote) else it }
                            _state.value = state.value.copy(
                                isArchived = updatedNote.isArchived,
                                notes = updatedNotesList
                            )
                        }
                    }
                }
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
                    val noteId = state.value.expandedNoteId
                    if (noteId == null) return@launch

                    val title = state.value.editingTitle
                    val content = if (state.value.editingNoteType == "TEXT") {
                        HtmlConverter.annotatedStringToHtml(state.value.editingContent.annotatedString)
                    } else {
                        Gson().toJson(state.value.editingChecklist)
                    }

                    if (title.isBlank() && (state.value.editingNoteType == "TEXT" && content.isBlank() || state.value.editingNoteType == "CHECKLIST" && state.value.editingChecklist.all { it.text.isBlank() })) {
                        if (noteId != -1) { // It's an existing note, so delete it
                            repository.getNoteById(noteId)?.let { repository.updateNote(it.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())) }
                        }
                    } else {
                        val currentTime = System.currentTimeMillis()
                        val note = if (noteId == -1) { // New note
                            Note(
                                title = title,
                                content = content,
                                createdAt = currentTime,
                                lastEdited = currentTime,
                                color = state.value.editingColor,
                                isPinned = state.value.isPinned,
                                isArchived = state.value.isArchived,
                                label = state.value.editingLabel,
                                linkPreviews = state.value.linkPreviews,
                                noteType = state.value.editingNoteType
                            )
                        } else { // Existing note
                            repository.getNoteById(noteId)?.let { existingNote ->
                                existingNote.note.copy(
                                    title = title,
                                    content = content,
                                    lastEdited = currentTime,
                                    color = state.value.editingColor,
                                    isPinned = state.value.isPinned,
                                    isArchived = state.value.isArchived,
                                    label = state.value.editingLabel,
                                    linkPreviews = state.value.linkPreviews,
                                    noteType = state.value.editingNoteType
                                )
                            }
                        }
                        if (note != null) {
                            val currentNoteId = if (noteId == -1) { // New note
                                repository.insertNote(note)
                            } else { // Existing note
                                repository.updateNote(note)
                                noteId.toLong() // Convert Int to Long for consistency
                            }

                            // Handle attachments
                            val existingAttachmentsInDb = if (noteId != -1) {
                                repository.getNoteById(noteId)?.attachments ?: emptyList()
                            } else {
                                emptyList()
                            }

                            val attachmentsToAdd = state.value.editingAttachments.filter { uiAttachment ->
                                existingAttachmentsInDb.none { dbAttachment ->
                                    dbAttachment.uri == uiAttachment.uri && dbAttachment.type == uiAttachment.type
                                }
                            }

                            val attachmentsToRemove = existingAttachmentsInDb.filter { dbAttachment ->
                                state.value.editingAttachments.none { uiAttachment ->
                                    uiAttachment.uri == dbAttachment.uri && uiAttachment.type == dbAttachment.type
                                }
                            }

                            attachmentsToRemove.forEach { attachment ->
                                repository.deleteAttachment(attachment)
                            }

                            attachmentsToAdd.forEach { attachment ->
                                repository.insertAttachment(attachment.copy(noteId = currentNoteId.toInt()))
                            }
                        }
                    }

                    // Reset editing state and collapse
                    _state.value = state.value.copy(
                        expandedNoteId = null,
                        editingTitle = "",
                        editingContent = TextFieldValue(),
                        editingColor = 0,
                        editingIsNewNote = true,
                        editingLastEdited = 0,
                        editingHistory = listOf("" to TextFieldValue()),
                        editingHistoryIndex = 0,
                        isPinned = false,
                        isArchived = false,
                        editingLabel = null,
                        isBoldActive = false,
                        isItalicActive = false,
                        isUnderlineActive = false,
                        activeStyles = emptySet(),
                        linkPreviews = emptyList(),
                        editingChecklist = emptyList(),
                        editingAttachments = emptyList()
                    )
                }
            }
            is NotesEvent.OnDeleteNoteClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let {
                        if (it != -1) {
                            repository.getNoteById(it)?.let { note ->
                                repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                                _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
                            }
                        }
                    }
                    _state.value = state.value.copy(expandedNoteId = null)
                }
            }
            is NotesEvent.OnCopyCurrentNoteClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let {
                        repository.getNoteById(it)?.let { noteWithAttachments ->
                            val copiedNote = noteWithAttachments.note.copy(id = 0, title = "${noteWithAttachments.note.title} (Copy)", createdAt = System.currentTimeMillis(), lastEdited = System.currentTimeMillis())
                            val newNoteId = repository.insertNote(copiedNote)
                            noteWithAttachments.attachments.forEach { attachment ->
                                repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId.toInt()))
                            }
                            _events.emit(NotesUiEvent.ShowToast("Note copied"))
                        }
                    }
                }
            }
            is NotesEvent.OnAddLabelsToCurrentNoteClick -> {
                _state.value = state.value.copy(showLabelDialog = true)
            }
            is NotesEvent.DismissLabelDialog -> {
                _state.value = state.value.copy(showLabelDialog = false)
            }
            is NotesEvent.FilterByLabel -> {
                _state.value = state.value.copy(filteredLabel = event.label)
            }
            is NotesEvent.ToggleLayout -> {
                val newLayout = if (state.value.layoutType == LayoutType.GRID) LayoutType.LIST else LayoutType.GRID
                _state.value = state.value.copy(layoutType = newLayout)
            }
            is NotesEvent.SortNotes -> {
                _sortType.value = event.sortType
            }
            is NotesEvent.OnRemoveLinkPreview -> {
                val updatedLinkPreviews = state.value.linkPreviews.filter { it.url != event.url }
                _state.value = state.value.copy(linkPreviews = updatedLinkPreviews)
                viewModelScope.launch {
                    _events.emit(NotesUiEvent.LinkPreviewRemoved)
                }
            }
            is NotesEvent.OnInsertLink -> {
                val content = state.value.editingContent
                val selection = content.selection
                if (!selection.collapsed) {
                    val selectedText = content.text.substring(selection.start, selection.end)
                    val newAnnotatedString = buildAnnotatedString {
                        append(content.annotatedString.subSequence(0, selection.start))
                        pushStringAnnotation(tag = "URL", annotation = event.url)
                        withStyle(style = SpanStyle(color = androidx.compose.ui.graphics.Color.Blue, textDecoration = TextDecoration.Underline)) {
                            append(selectedText)
                        }
                        pop()
                        append(content.annotatedString.subSequence(selection.end, content.text.length))
                    }
                    val newTextFieldValue = content.copy(annotatedString = newAnnotatedString)
                    _state.value = state.value.copy(editingContent = newTextFieldValue)
                }
            }
            is NotesEvent.ClearNewlyAddedChecklistItemId -> {
                _state.value = state.value.copy(newlyAddedChecklistItemId = null)
            }
            is NotesEvent.AddAttachment -> {
                val type = when {
                    event.mimeType.startsWith("image") -> "IMAGE"
                    event.mimeType.startsWith("video") -> "VIDEO"
                    event.mimeType.startsWith("audio") -> "AUDIO"
                    else -> "FILE"
                }
                val attachment = Attachment(
                    noteId = state.value.expandedNoteId ?: -1,
                    uri = event.uri,
                    type = type,
                    mimeType = event.mimeType,
                    tempId = java.util.UUID.randomUUID().toString()
                )
                _state.value = state.value.copy(editingAttachments = state.value.editingAttachments + attachment)
            }
            is NotesEvent.OnLinkDetected -> { /* TODO: Handle link detection */ }
            is NotesEvent.OnLinkPreviewFetched -> { /* TODO: Handle link preview fetched */ }
            is NotesEvent.RemoveAttachment -> {
                viewModelScope.launch {
                    val attachmentToRemove = _state.value.editingAttachments.firstOrNull { it.tempId == event.tempId }
                    attachmentToRemove?.let {
                        if (it.id != 0) { // Only delete from DB if it has a real ID
                            repository.deleteAttachmentById(it.id)
                        }
                        val updatedAttachments = _state.value.editingAttachments.filter { attachment -> attachment.tempId != event.tempId }
                        _state.value = _state.value.copy(editingAttachments = updatedAttachments)
                    }
                }
            }
            is NotesEvent.CreateProject -> {
                viewModelScope.launch {
                    val newProject = Project(name = event.name)
                    repository.insertProject(newProject)
                    _events.emit(NotesUiEvent.ProjectCreated(event.name))
                }
            }
            is NotesEvent.MoveSelectedNotesToProject -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.insertNote(note.note.copy(projectId = event.projectId))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    _events.emit(NotesUiEvent.ShowToast("${selectedNotes.size} notes moved to project"))
                }
            }
            is NotesEvent.CreateNoteFromSharedText -> {
                _state.value = state.value.copy(
                    expandedNoteId = -1,
                    editingTitle = "",
                    editingContent = TextFieldValue(event.text),
                    editingColor = 0,
                    editingIsNewNote = true,
                    editingLastEdited = 0,
                    editingHistory = listOf("" to TextFieldValue(event.text)),
                    editingHistoryIndex = 0,
                    editingLabel = null,
                    linkPreviews = emptyList(),
                    editingNoteType = "TEXT",
                    editingChecklist = emptyList(),
                    editingAttachments = emptyList()
                )
            }
        }
    }


}

