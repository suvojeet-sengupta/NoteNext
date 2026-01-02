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
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.ProjectDao
import com.suvojeet.notenext.data.SortType
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.data.AlarmScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import com.suvojeet.notenext.data.RepeatOption
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
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.domain.use_case.NoteUseCases
import com.suvojeet.notenext.ui.util.UndoRedoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.suvojeet.notenext.widget.NoteWidgetProvider
import com.suvojeet.notenext.R
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val noteUseCases: NoteUseCases,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val alarmScheduler: AlarmScheduler,
    private val richTextController: RichTextController,
    private val groqRepository: com.suvojeet.notenext.data.repository.GroqRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NotesState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events = _events.asSharedFlow()

    private var recentlyDeletedNote: Note? = null
    
    private val undoRedoManager = UndoRedoManager<Pair<String, TextFieldValue>>("" to TextFieldValue())

    private val _searchQuery = MutableStateFlow("")
    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED)

    init {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        combine(
            combine(_searchQuery, _sortType) { query, sortType -> query to sortType }
                .flatMapLatest { (query, sortType) ->
                    noteUseCases.getNotes(query, sortType)
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

        viewModelScope.launch {
             NoteSelectionManager.actions.collect { style ->
                 onEvent(NotesEvent.ApplyStyleToContent(style))
             }
        }
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.GenerateChecklist -> {
                viewModelScope.launch {
                    _state.value = state.value.copy(isSummarizing = true) // Reuse summarizing flag for loading indicator
                    groqRepository.generateChecklist(event.topic).collect { result ->
                        result.onSuccess { items ->
                            val checklistItems = items.mapIndexed { index, text -> 
                                ChecklistItem(
                                    id = java.util.UUID.randomUUID().toString(),
                                    text = text, 
                                    isChecked = false, 
                                    position = index,
                                    noteId = state.value.expandedNoteId ?: 0
                                ) 
                            }
                            
                            // Initialize text fields map for the new items
                            val newInputValues = checklistItems.associate { item ->
                                item.id to TextFieldValue(item.text)
                            }

                            _state.value = state.value.copy(
                                isSummarizing = false,
                                editingNoteType = "CHECKLIST",
                                editingChecklist = checklistItems,
                                checklistInputValues = newInputValues
                            )
                        }.onFailure {
                            _state.value = state.value.copy(isSummarizing = false)
                            _events.emit(NotesUiEvent.ShowToast("Failed to generate checklist: ${it.message}"))
                        }
                    }
                }
            }
            is NotesEvent.OnSearchQueryChange -> {
                _searchQuery.value = event.query
            }
            is NotesEvent.OnRestoreVersion -> {
                viewModelScope.launch {
                    val content = HtmlConverter.htmlToAnnotatedString(event.version.content)
                    _state.value = state.value.copy(
                        editingTitle = event.version.title,
                        editingContent = TextFieldValue(content),
                        editingNoteType = event.version.noteType
                    )
                    _events.emit(NotesUiEvent.ShowToast("Version restored"))
                }
            }
            is NotesEvent.NavigateToNoteByTitle -> {
                viewModelScope.launch {
                    _events.emit(NotesUiEvent.NavigateToNoteByTitle(event.title))
                }
            }
            is NotesEvent.DeleteNote -> {
                viewModelScope.launch {
                    noteUseCases.deleteNote(event.note.note)
                    recentlyDeletedNote = event.note.note
                    _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
                    updateWidgets()
                }
            }
            is NotesEvent.RestoreNote -> {
                viewModelScope.launch {
                    recentlyDeletedNote?.let { restoredNote ->
                        repository.updateNote(restoredNote.copy(isBinned = false))
                        recentlyDeletedNote = null
                        updateWidgets()
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
            is NotesEvent.SelectAllNotes -> {
                val notesToSelect = if (state.value.filteredLabel != null) {
                    state.value.notes.filter { it.note.label == state.value.filteredLabel }
                } else {
                    state.value.notes
                }
                val allIds = notesToSelect.map { it.note.id }
                _state.value = state.value.copy(selectedNoteIds = allIds)
            }
            is NotesEvent.TogglePinForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    val areNotesBeingPinned = selectedNotes.firstOrNull()?.note?.isPinned == false
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isPinned = areNotesBeingPinned))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    val message = if (areNotesBeingPinned) {
                        if (selectedNotes.size > 1) "${selectedNotes.size} notes pinned" else "Note pinned"
                    } else {
                        if (selectedNotes.size > 1) "${selectedNotes.size} notes unpinned" else "Note unpinned"
                    }
                    _events.emit(NotesUiEvent.ShowToast(message))
                    updateWidgets()
                }
            }
            is NotesEvent.ToggleLockForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                     val areNotesBeingLocked = selectedNotes.firstOrNull()?.note?.isLocked == false
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isLocked = areNotesBeingLocked))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    val message = if (areNotesBeingLocked) {
                        if (selectedNotes.size > 1) "${selectedNotes.size} notes locked" else "Note locked"
                    } else {
                        if (selectedNotes.size > 1) "${selectedNotes.size} notes unlocked" else "Note unlocked"
                    }
                    _events.emit(NotesUiEvent.ShowToast(message))
                    updateWidgets()
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
                    updateWidgets()
                }
            }
            is NotesEvent.ArchiveSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isArchived = !note.note.isArchived))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    updateWidgets()
                }
            }
            is NotesEvent.ToggleImportantForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isImportant = !note.note.isImportant))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ChangeColorForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(color = event.color))
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
                        // Copy checklist items
                        val newChecklistItems = noteWithAttachments.checklistItems.map { item ->
                            item.copy(id = java.util.UUID.randomUUID().toString(), noteId = newNoteId.toInt())
                        }
                        repository.insertChecklistItems(newChecklistItems)
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
                        val contentBuilder = StringBuilder()
                        selectedNotes.forEachIndexed { index, it ->
                            contentBuilder.append("Title: ${it.note.title}\n\n${HtmlConverter.htmlToPlainText(it.note.content)}")
                            if (index < selectedNotes.size - 1) {
                                contentBuilder.append("\n\n---\n\n")
                            }
                        }
                        _events.emit(NotesUiEvent.SendNotes(title, contentBuilder.toString()))
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
                        repository.updateNote(note.note.copy(label = event.label))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ExpandNote -> {
                viewModelScope.launch {
                    if (event.noteId != -1) {
                        noteUseCases.getNote(event.noteId)?.let { noteWithAttachments ->
                            val note = noteWithAttachments.note
                            val content = if (note.noteType == "TEXT") {
                                HtmlConverter.htmlToAnnotatedString(note.content)
                            } else {
                                AnnotatedString("")
                            }
                            val checklist = if (note.noteType == "CHECKLIST") {
                                noteWithAttachments.checklistItems.sortedBy { it.position }
                            } else {
                                emptyList<ChecklistItem>()
                            }

                            // Fetch versions
                            viewModelScope.launch {
                                repository.getNoteVersions(event.noteId).collect { versions ->
                                    _state.value = _state.value.copy(editingNoteVersions = versions)
                                }
                            }
                            
                            val contentValue = TextFieldValue(content)
                            undoRedoManager.reset(note.title to contentValue)

                            _state.value = state.value.copy(
                                expandedNoteId = event.noteId,
                                editingTitle = note.title,
                                editingContent = contentValue,
                                editingColor = note.color,
                                editingIsNewNote = false,
                                editingLastEdited = note.lastEdited,
                                isPinned = note.isPinned,
                                isArchived = note.isArchived,
                                editingLabel = note.label,
                                canUndo = undoRedoManager.canUndo.value,
                                canRedo = undoRedoManager.canRedo.value,
                                linkPreviews = note.linkPreviews,
                                editingNoteType = note.noteType,
                                editingChecklist = checklist,
                                editingAttachments = noteWithAttachments.attachments.map { it.copy(tempId = java.util.UUID.randomUUID().toString()) },
                                editingIsLocked = note.isLocked,
                                checklistInputValues = checklist.associate { item ->
                                    item.id to TextFieldValue(richTextController.parseMarkdownToAnnotatedString(item.text))
                                },
                                editingReminderTime = note.reminderTime,
                                editingRepeatOption = note.repeatOption,
                                summaryResult = note.aiSummary,
                                showSummaryDialog = false
                            )
                        }
                    } else {
                        undoRedoManager.reset("" to TextFieldValue())
                        _state.value = state.value.copy(
                            expandedNoteId = -1,
                            editingTitle = "",
                            editingContent = TextFieldValue(),
                            editingColor = 0,
                            editingIsNewNote = true,
                            editingLastEdited = 0,
                            canUndo = undoRedoManager.canUndo.value,
                            canRedo = undoRedoManager.canRedo.value,
                            editingLabel = null,
                            linkPreviews = emptyList(),
                            editingNoteType = event.noteType,
                            editingChecklist = if (event.noteType == "CHECKLIST") {
                                val newItem = ChecklistItem(text = "", isChecked = false)
                                listOf(newItem)
                            } else emptyList(),
                            checklistInputValues = if (event.noteType == "CHECKLIST") {
                                val newItem = ChecklistItem(text = "", isChecked = false)
                                mapOf(newItem.id to TextFieldValue(""))
                            } else emptyMap(),
                            editingAttachments = emptyList(),
                            editingIsLocked = false,
                            editingNoteVersions = emptyList(),
                            summaryResult = null,
                            showSummaryDialog = false
                        )
                    }
                }
            }
            is NotesEvent.OnToggleLockClick -> {
                viewModelScope.launch {
                    val currentLockState = state.value.editingIsLocked
                    _state.value = state.value.copy(editingIsLocked = !currentLockState)
                    // If note exists, update immediately
                    state.value.expandedNoteId?.let { noteId ->
                         if (noteId != -1) {
                             repository.getNoteById(noteId)?.let { note ->
                                 repository.updateNote(note.note.copy(isLocked = !currentLockState))
                             }
                             // Update list locally
                             val updatedNotesList = state.value.notes.map { if (it.note.id == noteId) it.copy(note = it.note.copy(isLocked = !currentLockState)) else it }
                             _state.value = _state.value.copy(notes = updatedNotesList)
                             _events.emit(NotesUiEvent.ShowToast(if (!currentLockState) "Note locked" else "Note unlocked"))
                             updateWidgets()
                         }
                    }
                }
            }
            is NotesEvent.CollapseNote -> {
                onEvent(NotesEvent.OnSaveNoteClick)
            }
            is NotesEvent.AddChecklistItem -> {
                val newItem = ChecklistItem(text = "", isChecked = false, position = state.value.editingChecklist.size)
                val updatedChecklist = state.value.editingChecklist + newItem
                _state.value = state.value.copy(
                    editingChecklist = updatedChecklist,
                    newlyAddedChecklistItemId = newItem.id,
                    checklistInputValues = state.value.checklistInputValues + (newItem.id to TextFieldValue(""))
                )
            }
            is NotesEvent.SwapChecklistItems -> {
                val list = state.value.editingChecklist.toMutableList()
                val fromIndex = list.indexOfFirst { it.id == event.fromId }
                val toIndex = list.indexOfFirst { it.id == event.toId }

                if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                    java.util.Collections.swap(list, fromIndex, toIndex)
                    // Update all positions to match indices to be safe
                    val updatedList = list.mapIndexed { index, item -> item.copy(position = index) }
                    _state.value = state.value.copy(editingChecklist = updatedList)
                }
            }
            is NotesEvent.DeleteChecklistItem -> {
                val updatedChecklist = state.value.editingChecklist.filterNot { it.id == event.itemId }
                _state.value = state.value.copy(
                    editingChecklist = updatedChecklist,
                    checklistInputValues = state.value.checklistInputValues - event.itemId
                )
            }
            is NotesEvent.IndentChecklistItem -> {
                val updatedChecklist = state.value.editingChecklist.map {
                    if (it.id == event.itemId) it.copy(level = kotlin.math.min(it.level + 1, 5)) else it
                }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
            }
            is NotesEvent.OutdentChecklistItem -> {
                val updatedChecklist = state.value.editingChecklist.map {
                    if (it.id == event.itemId) it.copy(level = kotlin.math.max(it.level - 1, 0)) else it
                }
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
                undoRedoManager.addState(event.title to state.value.editingContent)
                _state.value = state.value.copy(
                    editingTitle = event.title,
                    canUndo = undoRedoManager.canUndo.value,
                    canRedo = undoRedoManager.canRedo.value,
                    summaryResult = null // Invalidate cache on title change
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
                    
                    undoRedoManager.addState(state.value.editingTitle to finalContent)
                    
                    _state.value = state.value.copy(
                        editingContent = finalContent,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value,
                        isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                        isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                        isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline },
                        summaryResult = null // Invalidate cache on content change
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
            is NotesEvent.OnChecklistItemValueChange -> {
                val updatedInputValues = state.value.checklistInputValues.toMutableMap()
                updatedInputValues[event.itemId] = event.value

                // Check for styles at selection to update toolbar state
                 val selection = event.value.selection
                 val styles = if (selection.collapsed) {
                    if (selection.start > 0) {
                        event.value.annotatedString.spanStyles.filter {
                            it.start <= selection.start - 1 && it.end >= selection.start
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    event.value.annotatedString.spanStyles.filter {
                        maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                    }
                }

                _state.value = state.value.copy(
                    checklistInputValues = updatedInputValues,
                     isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                     isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                     isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                )
                
                // Async update for persistence model
                viewModelScope.launch {
                    val updatedText = HtmlConverter.annotatedStringToHtml(event.value.annotatedString).let {
                        com.suvojeet.notenext.data.MarkdownExporter.convertHtmlToMarkdown(it)
                    }

                    val updatedChecklist = _state.value.editingChecklist.map {
                        if (it.id == event.itemId) it.copy(text = updatedText) else it
                    }
                    _state.value = _state.value.copy(editingChecklist = updatedChecklist)
                }
            }
            is NotesEvent.OnChecklistItemFocus -> {
                _state.value = state.value.copy(focusedChecklistItemId = event.itemId)
                // Update active styles based on the focused item's cursor position handled in ValueChange or just reset/check here
                val value = state.value.checklistInputValues[event.itemId]
                if (value != null) {
                     val selection = value.selection
                     val styles = value.annotatedString.spanStyles.filter {
                        maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                    }
                     _state.value = state.value.copy(
                         isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                         isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                         isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                     )
                }
            }
            is NotesEvent.ApplyStyleToContent -> {
                if (state.value.editingNoteType == "CHECKLIST") {
                    val focusedId = state.value.focusedChecklistItemId
                    if (focusedId != null) {
                        val currentValue = state.value.checklistInputValues[focusedId]
                        if (currentValue != null) {
                             val result = richTextController.toggleStyle(
                                currentValue,
                                event.style,
                                emptySet(), // We don't track activeStyles per item easily yet, relies on result
                                state.value.isBoldActive,
                                state.value.isItalicActive,
                                state.value.isUnderlineActive
                            )
                            if (result.updatedContent != null) {
                                onEvent(NotesEvent.OnChecklistItemValueChange(focusedId, result.updatedContent))
                            }
                        }
                    }
                } else {
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
                    undoRedoManager.addState(state.value.editingTitle to result.updatedContent)
                    _state.value = state.value.copy(
                        editingContent = result.updatedContent,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value
                    )
                }
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
                    undoRedoManager.addState(state.value.editingTitle to updatedContent)

                    _state.value = state.value.copy(
                        editingContent = updatedContent,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value,
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
                val newPinnedState = !state.value.isPinned
                _state.value = state.value.copy(isPinned = newPinnedState)
                viewModelScope.launch {
                    saveNote(shouldCollapse = false)
                    val message = if (newPinnedState) "Note pinned" else "Note unpinned"
                    _events.emit(NotesUiEvent.ShowToast(message))
                }
            }
            is NotesEvent.OnToggleArchiveClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let { noteId ->
                        repository.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isArchived = !note.note.isArchived)
                            repository.updateNote(updatedNote)
                            val updatedNotesList = state.value.notes.map { if (it.note.id == updatedNote.id) it.copy(note = updatedNote) else it }
                            _state.value = state.value.copy(
                                isArchived = updatedNote.isArchived,
                                notes = updatedNotesList
                            )
                            updateWidgets()
                        }
                    }
                }
            }
            is NotesEvent.OnUndoClick -> {
                undoRedoManager.undo()?.let { (title, content) ->
                    _state.value = state.value.copy(
                        editingTitle = title,
                        editingContent = content,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value
                    )
                }
            }
            is NotesEvent.OnRedoClick -> {
                undoRedoManager.redo()?.let { (title, content) ->
                    _state.value = state.value.copy(
                        editingTitle = title,
                        editingContent = content,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value
                    )
                }
            }
            is NotesEvent.OnSaveNoteClick -> {
                viewModelScope.launch {
                    saveNote(shouldCollapse = true)
                }
            }
            is NotesEvent.OnDeleteNoteClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let {
                        if (it != -1) {
                            repository.getNoteById(it)?.let { note ->
                                repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                                _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
                                updateWidgets()
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
                        repository.updateNote(note.note.copy(projectId = event.projectId))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    _events.emit(NotesUiEvent.ShowToast("${selectedNotes.size} notes moved to project"))
                }
            }
            is NotesEvent.OnToggleNoteType -> {
                val currentType = state.value.editingNoteType
                if (currentType == "TEXT") {
                    // Convert TEXT to CHECKLIST
                    val lines = state.value.editingContent.text.split("\n")
                    val checklistItems = lines.filter { it.isNotBlank() }.mapIndexed { index, text ->
                        ChecklistItem(text = text.trim(), isChecked = false, position = index)
                    }
                    // If empty, add one empty item
                    val finalItems = if (checklistItems.isEmpty()) listOf(ChecklistItem(text = "", isChecked = false, position = 0)) else checklistItems
                    
                    _state.value = state.value.copy(
                        editingNoteType = "CHECKLIST",
                        editingChecklist = finalItems,
                        editingContent = TextFieldValue("") // Clear text content
                    )
                } else {
                    // Convert CHECKLIST to TEXT
                    val textContent = state.value.editingChecklist.joinToString("\n") { it.text }
                    _state.value = state.value.copy(
                        editingNoteType = "TEXT",
                        editingContent = TextFieldValue(textContent),
                        editingChecklist = emptyList()
                    )
                }
            }
            is NotesEvent.ToggleCheckedItemsExpanded -> {
                _state.value = state.value.copy(
                    isCheckedItemsExpanded = !state.value.isCheckedItemsExpanded
                )
            }
            is NotesEvent.SummarizeNote -> {
                val content = if (state.value.editingNoteType == "CHECKLIST") {
                    state.value.editingChecklist.joinToString("\n") { it.text }
                } else {
                    state.value.editingContent.text
                }

                if (content.isNotBlank()) {
                    if (state.value.summaryResult != null) {
                         // Cache hit - just show dialog
                         _state.value = _state.value.copy(showSummaryDialog = true)
                    } else {
                         // Cache miss - fetch
                        _state.value = _state.value.copy(isSummarizing = true, showSummaryDialog = true)
                        viewModelScope.launch {
                            groqRepository.summarizeNote(content).collect { result ->
                                result.onSuccess { summary ->
                                    _state.value = _state.value.copy(isSummarizing = false, summaryResult = summary)
                                }.onFailure {
                                    _state.value = _state.value.copy(
                                        isSummarizing = false,
                                        summaryResult = "Error: " + it.localizedMessage
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is NotesEvent.ClearSummary -> {
                _state.value = _state.value.copy(showSummaryDialog = false)
            }
            is NotesEvent.DeleteAllCheckedItems -> {
                val updatedChecklist = state.value.editingChecklist.filter { !it.isChecked }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
            }
            is NotesEvent.CreateNoteFromSharedText -> {
                undoRedoManager.reset("" to TextFieldValue(event.text))
                _state.value = state.value.copy(
                    expandedNoteId = -1,
                    editingTitle = "",
                    editingContent = TextFieldValue(event.text),
                    editingColor = 0,
                    editingIsNewNote = true,
                    editingLastEdited = 0,
                    canUndo = undoRedoManager.canUndo.value,
                    canRedo = undoRedoManager.canRedo.value,
                    editingLabel = null,
                    linkPreviews = emptyList(),
                    editingNoteType = "TEXT",
                    editingChecklist = emptyList(),
                    editingAttachments = emptyList()
                )
            }
            is NotesEvent.OnReminderChange -> {
                _state.value = state.value.copy(
                    editingReminderTime = event.time,
                    editingRepeatOption = event.repeatOption
                )
            }
        }
    }


    private fun updateWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, NoteWidgetProvider::class.java))
        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
    }

    suspend fun getNoteLockStatus(noteId: Int): Boolean {
        return repository.getNoteById(noteId)?.note?.isLocked == true
    }

    suspend fun getNoteIdByTitle(title: String): Int? {
        return repository.getNoteIdByTitle(title)
    }

    private suspend fun saveNote(shouldCollapse: Boolean) {
        val noteId = state.value.expandedNoteId
        if (noteId == null) return

        val title = state.value.editingTitle
        val content = if (state.value.editingNoteType == "TEXT") {
            HtmlConverter.annotatedStringToHtml(state.value.editingContent.annotatedString)
        } else {
            ""
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
                    noteType = state.value.editingNoteType,
                    isLocked = state.value.editingIsLocked,
                    reminderTime = state.value.editingReminderTime,
                    repeatOption = state.value.editingRepeatOption
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
                        noteType = state.value.editingNoteType,
                        isLocked = state.value.editingIsLocked,
                        reminderTime = state.value.editingReminderTime,
                        repeatOption = state.value.editingRepeatOption,
                        aiSummary = state.value.summaryResult
                    )
                }
            }
            if (note != null) {
                val currentNoteId = if (noteId == -1) { // New note
                    repository.insertNote(note)
                } else { // Existing note
                    // Before updating, save current state as a version if it's not a new note
                    repository.getNoteById(noteId)?.let { oldNoteWithAttachments ->
                        val oldNote = oldNoteWithAttachments.note
                        // Only save version if content or title changed
                        if (oldNote.title != title || oldNote.content != content) {
                            repository.insertNoteVersion(
                                NoteVersion(
                                    noteId = noteId,
                                    title = oldNote.title,
                                    content = oldNote.content,
                                    timestamp = oldNote.lastEdited,
                                    noteType = oldNote.noteType
                                )
                            )
                            repository.limitNoteVersions(noteId, 10)
                        }
                    }
                    repository.updateNote(note)
                    noteId.toLong() // Convert Int to Long for consistency
                }

                if (state.value.editingReminderTime != null) {
                    alarmScheduler.schedule(note.copy(id = currentNoteId.toInt()))
                } else if (noteId != -1) {
                    alarmScheduler.cancel(note.copy(id = currentNoteId.toInt()))
                }

                // Handle Checklist Items
                if (state.value.editingNoteType == "CHECKLIST") {
                    val checklistItems = state.value.editingChecklist.mapIndexed { index, item ->
                        item.copy(noteId = currentNoteId.toInt(), position = index)
                    }
                    repository.deleteChecklistForNote(currentNoteId.toInt())
                    repository.insertChecklistItems(checklistItems)
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

                // Update expandedNoteId if it was a new note
                if (noteId == -1 && !shouldCollapse) {
                    _state.value = state.value.copy(expandedNoteId = currentNoteId.toInt())
                }
            }
        }

        if (shouldCollapse) {
            // Reset editing state and collapse
            _state.value = state.value.copy(
                expandedNoteId = null,
                editingTitle = "",
                editingContent = TextFieldValue(),
                editingColor = 0,
                editingIsNewNote = true,
                editingLastEdited = 0,
                canUndo = false,
                canRedo = false,
                isPinned = false,
                isArchived = false,
                editingLabel = null,
                isBoldActive = false,
                isItalicActive = false,
                isUnderlineActive = false,
                activeStyles = emptySet(),
                linkPreviews = emptyList(),
                editingChecklist = emptyList(),
                editingAttachments = emptyList(),
                editingReminderTime = null,
                editingRepeatOption = null
            )
        }
        updateWidgets()
    }
}

