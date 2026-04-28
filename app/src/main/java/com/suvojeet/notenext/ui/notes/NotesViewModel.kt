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
import androidx.paging.cachedIn
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.core.util.ImageUtils
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.ProjectDao
import com.suvojeet.notenext.core.util.SortType
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

import com.suvojeet.notenext.core.model.AttachmentType
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.repository.AiRepository
import com.suvojeet.notenext.data.repository.AiResult
import com.suvojeet.notenext.data.repository.onFailure
import com.suvojeet.notenext.data.repository.onSuccess
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
import com.suvojeet.notenext.ui.theme.NoteGradients
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val todoRepository: com.suvojeet.notenext.data.TodoRepository,
    private val noteUseCases: NoteUseCases,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val alarmScheduler: AlarmScheduler,
    private val richTextController: RichTextController,
    private val aiRepository: AiRepository,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val editorDelegate: com.suvojeet.notenext.ui.notes.delegate.NoteEditorDelegate,
    private val listDelegate: com.suvojeet.notenext.ui.notes.delegate.NoteListDelegate,
    private val bulkActionDelegate: com.suvojeet.notenext.ui.notes.delegate.BulkActionDelegate,
    private val aiDelegate: com.suvojeet.notenext.ui.notes.delegate.AIDelegate,
    private val aiSuggestionsDelegate: com.suvojeet.notenext.ui.notes.delegate.AISuggestionsDelegate,
    private val aiFeatureGate: com.suvojeet.notenext.data.ai.AIFeatureGate
) : ViewModel() {

    companion object {
        private const val KEY_EDITING_TITLE = "editing_title"
        private const val KEY_EDITING_CONTENT = "editing_content"
        private const val KEY_EXPANDED_NOTE_ID = "expanded_note_id"
        private const val KEY_NOTE_TYPE = "note_type"
    }

    val listState = listDelegate.listState
    val editState = editorDelegate.editState

    // High-frequency editing flows to isolate recomposition
    val editingContent = editState.map { it.editingContent }.distinctUntilChanged()
    val editingTitle = editState.map { it.editingTitle }.distinctUntilChanged()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events = _events.asSharedFlow()

    private var isDecoySession: Boolean = false
    private var recentlyDeletedNote: Note? = null
    
    private var selectionActionsJob: Job? = null
    private var linkDetectionJob: Job? = null

    private var lastCreatedNoteId: Int? = null

    private fun scheduleAutoSave() {
        editorDelegate.scheduleAutoSave(viewModelScope) {
            saveNote(shouldCollapse = false)
        }
    }

    init {
        listDelegate.observeNotes(viewModelScope)

        selectionActionsJob = viewModelScope.launch {
            try {
                 NoteSelectionManager.actions.collect { style ->
                     onEvent(NotesEvent.ApplyStyleToContent(style))
                 }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Restore expanded note if process was killed
        val restoredNoteId = savedStateHandle.get<Int>(KEY_EXPANDED_NOTE_ID)
        if (restoredNoteId != null && restoredNoteId != -1) {
            onEvent(NotesEvent.ExpandNote(restoredNoteId))
        }

        repository.getLabels().onEach { labels ->
            val labelNames = labels.map { it.name }.toImmutableList()
            editorDelegate.updateState { it.copy(labels = labelNames) }
        }.launchIn(viewModelScope)
    }

    fun setDecoyMode(isDecoy: Boolean) {
        this.isDecoySession = isDecoy
        listDelegate.setDecoyMode(isDecoy)
    }

    override fun onCleared() {
        super.onCleared()
        editorDelegate.cancelAutoSave()
        selectionActionsJob?.cancel()
        linkDetectionJob?.cancel()
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.GenerateChecklist -> {
                viewModelScope.launch {
                    if (!aiFeatureGate.isEnabled(com.suvojeet.notenext.data.ai.AIFeature.CHECKLIST)) {
                        _events.emit(NotesUiEvent.ShowToast("Checklist generation is disabled in AI Settings"))
                        return@launch
                    }
                    editorDelegate.updateState { it.copy(isGeneratingChecklist = true, generatedChecklistPreview = persistentListOf()) }
                    aiRepository.generateChecklist(event.topic).collect { result ->
                        result.onSuccess { items ->
                            // Store preview instead of inserting directly
                            editorDelegate.updateState { it.copy(
                                isGeneratingChecklist = false,
                                generatedChecklistPreview = items.toImmutableList()
                            ) }
                        }.onFailure { failure ->
                            editorDelegate.updateState { it.copy(isGeneratingChecklist = false, generatedChecklistPreview = persistentListOf()) }

                            val errorMessage = when (failure) {
                                is AiResult.RateLimited -> "AI is busy. Please try again in ${failure.retryAfterSeconds}s."
                                is AiResult.InvalidKey -> "Invalid API key. Check your settings."
                                is AiResult.NetworkError -> "Network error: ${failure.message}"
                                is AiResult.AllModelsFailed -> "All AI models failed to respond. Try again later."
                                else -> "Failed to generate checklist."
                            }
                            _events.emit(NotesUiEvent.ShowToast(errorMessage))
                        }
                    }
                }
            }
            is NotesEvent.InsertGeneratedChecklist -> {
                val items = event.items
                if (items.isNotEmpty()) {
                    val checklistItems = items.mapIndexed { index, text -> 
                        ChecklistItem(
                            id = java.util.UUID.randomUUID().toString(),
                            text = text, 
                            isChecked = false, 
                            position = editState.value.editingChecklist.size + index,
                            noteId = editState.value.expandedNoteId ?: 0
                        ) 
                    }.toImmutableList()
                    
                    val newInputValues = checklistItems.associate { item ->
                        item.id to TextFieldValue(item.text)
                    }.toImmutableMap()

                    editorDelegate.updateState { it.copy(
                        editingNoteType = NoteType.CHECKLIST,
                        editingChecklist = (editState.value.editingChecklist + checklistItems).toImmutableList(),
                        checklistInputValues = (editState.value.checklistInputValues + newInputValues).toImmutableMap(),
                        generatedChecklistPreview = persistentListOf()
                    ) }
                }
            }
            is NotesEvent.ClearGeneratedChecklist -> {
                editorDelegate.updateState { it.copy(generatedChecklistPreview = persistentListOf(), isGeneratingChecklist = false) }
            }
            is NotesEvent.FixGrammar -> {
                viewModelScope.launch {
                    if (!aiFeatureGate.isEnabled(com.suvojeet.notenext.data.ai.AIFeature.GRAMMAR)) {
                        _events.emit(NotesUiEvent.ShowToast("Grammar fix is disabled in AI Settings"))
                        return@launch
                    }
                    aiDelegate.fixGrammar(editState.value.editingContent, viewModelScope, _events) { transform ->
                        editorDelegate.updateState(transform)
                    }
                }
            }
            is NotesEvent.AutoSaveNote -> {
                viewModelScope.launch {
                    saveNote(shouldCollapse = false)
                    editorDelegate.updateState { it.copy(saveStatus = SaveStatus.SAVED) }
                }
            }
            is NotesEvent.OnMentionSearchQueryChange -> {
                val query = event.query
                val filteredNotes = listState.value.notes.filter { 
                    it.note.title.contains(query, ignoreCase = true) && 
                    it.note.id != editState.value.expandedNoteId 
                }.toImmutableList()
                editorDelegate.updateState { it.copy(
                    isMentionPopupVisible = true,
                    mentionSearchQuery = query,
                    mentionableNotes = filteredNotes
                ) }
            }
            is NotesEvent.InsertMention -> {
                val currentText = editState.value.editingContent.text
                val selection = editState.value.editingContent.selection
                val mentionText = "@${editState.value.mentionSearchQuery}"
                
                // Find the mention text before the cursor and replace it with Markdown link
                val textBeforeCursor = currentText.substring(0, selection.start)
                val textAfterCursor = currentText.substring(selection.end)
                
                val lastMentionIndex = textBeforeCursor.lastIndexOf(mentionText)
                if (lastMentionIndex != -1) {
                    val wikiLink = "[[${event.noteTitle}]]"
                    val newTextBeforeCursor = textBeforeCursor.substring(0, lastMentionIndex) + wikiLink
                    
                    val newAnnotatedString = richTextController.parseMarkdownToAnnotatedString(newTextBeforeCursor + textAfterCursor)
                    val newCursorPosition = newTextBeforeCursor.length
                    
                    editorDelegate.updateState { it.copy(
                        editingContent = TextFieldValue(newAnnotatedString, androidx.compose.ui.text.TextRange(newCursorPosition)),
                        isMentionPopupVisible = false,
                        mentionSearchQuery = "",
                        mentionableNotes = persistentListOf()
                    ) }
                } else {
                    editorDelegate.updateState { it.copy(
                        isMentionPopupVisible = false,
                        mentionSearchQuery = "",
                        mentionableNotes = persistentListOf()
                    ) }
                }
            }
            is NotesEvent.CloseMentionPopup -> {
                editorDelegate.updateState { it.copy(
                    isMentionPopupVisible = false,
                    mentionSearchQuery = "",
                    mentionableNotes = persistentListOf()
                ) }
            }
            is NotesEvent.ImportImage -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val uri = event.uri
                    val compressedUri = ImageUtils.compressImage(context, uri)
                    if (compressedUri != null) {
                        val mimeType = context.contentResolver.getType(compressedUri)
                        onEvent(NotesEvent.AddAttachment(compressedUri.toString(), mimeType ?: "image/jpeg"))
                    } else {
                        // Fallback: Copy to internal storage manually if compression fails
                        try {
                            val fileName = "IMG_${java.util.UUID.randomUUID()}.jpg"
                            val destFile = java.io.File(context.filesDir, "images/$fileName")
                            destFile.parentFile?.mkdirs()
                            
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                java.io.FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            val localUri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", destFile
                            )
                            val mimeType = context.contentResolver.getType(localUri)
                            onEvent(NotesEvent.AddAttachment(localUri.toString(), mimeType ?: "image/jpeg"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            _events.emit(NotesUiEvent.ShowToast("Failed to import image: ${e.message}"))
                        }
                    }
                }
            }
            is NotesEvent.ApplyGrammarFix -> {
                val fixedContent = editState.value.fixedContentPreview
                if (fixedContent != null) {
                    editorDelegate.updateState { it.copy(
                        editingContent = TextFieldValue(fixedContent), // Apply clean text
                        fixedContentPreview = null,
                        originalContentBackup = null
                    ) }
                    viewModelScope.launch { _events.emit(NotesUiEvent.ShowToast("Fixed!")) }
                }
            }
            is NotesEvent.ClearGrammarFix -> {
                // Revert to backup
                editState.value.originalContentBackup?.let { backup ->
                    editorDelegate.updateState { it.copy(
                        editingContent = backup,
                        fixedContentPreview = null,
                        originalContentBackup = null
                    ) }
                } ?: run {
                     editorDelegate.updateState { it.copy(fixedContentPreview = null, isFixingGrammar = false) }
                }
            }
            is NotesEvent.LoadExternalFile -> {
                viewModelScope.launch {
                    try {
                        val uri = event.uri
                        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                        val fileName = com.suvojeet.notenext.util.ContextUtils.getFileName(context, uri) ?: "External Note"
                        
                        editorDelegate.reset(fileName, TextFieldValue(content))
                        
                        editorDelegate.updateState { it.copy(
                            expandedNoteId = -1, // Treat as new note but with external URI
                            externalUri = uri,
                            editingTitle = fileName,
                            editingContent = TextFieldValue(richTextController.parseMarkdownToAnnotatedString(content)),
                            editingNoteType = NoteType.TEXT,
                            editingIsNewNote = true,
                            canUndo = false,
                            canRedo = false
                        ) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _events.emit(NotesUiEvent.ShowToast("Failed to load file: ${e.message}"))
                    }
                }
            }
            is NotesEvent.SaveExternalAsNote -> {
                viewModelScope.launch {
                    val currentTime = System.currentTimeMillis()
                    val note = Note(
                        title = editState.value.editingTitle,
                        content = if (editState.value.editingNoteType == NoteType.TEXT) {
                            HtmlConverter.annotatedStringToHtml(editState.value.editingContent.annotatedString)
                        } else "",
                        createdAt = currentTime,
                        lastEdited = currentTime,
                        color = editState.value.editingColor,
                        noteType = editState.value.editingNoteType
                    )
                    val newId = repository.insertNote(note)
                    require(newId <= Int.MAX_VALUE) { "Note ID overflow" }
                    
                    editorDelegate.updateState { it.copy(
                        expandedNoteId = newId.toInt(),
                        externalUri = null,
                        editingIsNewNote = false
                    ) }
                    _events.emit(NotesUiEvent.ShowToast("Saved as internal note"))
                    updateWidgets()
                }
            }
            is NotesEvent.ToggleNoteSearch -> {
                val isSearching = !editState.value.isSearchingInNote
                editorDelegate.updateState { it.copy(
                    isSearchingInNote = isSearching,
                    noteSearchQuery = if (!isSearching) "" else editState.value.noteSearchQuery,
                    searchResultIndices = if (!isSearching) persistentListOf() else editState.value.searchResultIndices,
                    currentSearchResultIndex = if (!isSearching) -1 else editState.value.currentSearchResultIndex
                ) }
            }
            is NotesEvent.OnNoteSearchQueryChange -> {
                val query = event.query
                val content = editState.value.editingContent.text
                val indices = if (query.isNotBlank()) {
                    val foundIndices = mutableListOf<Int>()
                    var index = content.indexOf(query, ignoreCase = true)
                    while (index >= 0) {
                        foundIndices.add(index)
                        index = content.indexOf(query, index + 1, ignoreCase = true)
                    }
                    foundIndices.toImmutableList()
                } else persistentListOf()

                editorDelegate.updateState { it.copy(
                    noteSearchQuery = query,
                    searchResultIndices = indices,
                    currentSearchResultIndex = if (indices.isNotEmpty()) 0 else -1
                ) }
                
                if (indices.isNotEmpty()) {
                    viewModelScope.launch {
                        _events.emit(NotesUiEvent.ScrollToSearchResult(indices[0]))
                    }
                }
            }
            is NotesEvent.NextSearchResult -> {
                val indices = editState.value.searchResultIndices
                if (indices.isNotEmpty()) {
                    val nextIndex = (editState.value.currentSearchResultIndex + 1) % indices.size
                    editorDelegate.updateState { it.copy(currentSearchResultIndex = nextIndex) }
                    viewModelScope.launch {
                        _events.emit(NotesUiEvent.ScrollToSearchResult(indices[nextIndex]))
                    }
                }
            }
            is NotesEvent.PreviousSearchResult -> {
                val indices = editState.value.searchResultIndices
                if (indices.isNotEmpty()) {
                    val prevIndex = if (editState.value.currentSearchResultIndex <= 0) indices.size - 1 else editState.value.currentSearchResultIndex - 1
                    editorDelegate.updateState { it.copy(currentSearchResultIndex = prevIndex) }
                    viewModelScope.launch {
                        _events.emit(NotesUiEvent.ScrollToSearchResult(indices[prevIndex]))
                    }
                }
            }
            is NotesEvent.OnSearchQueryChange -> {
                listDelegate.setSearchQuery(event.query)
            }
            is NotesEvent.SortNotes -> {
                listDelegate.setSortType(event.sortType)
            }
            is NotesEvent.FilterByProject -> {
                listDelegate.setProjectId(event.projectId)
            }
            is NotesEvent.ToggleNoteSelection -> {
                listDelegate.toggleSelection(event.noteId)
            }
            is NotesEvent.ClearSelection -> {
                listDelegate.clearSelection()
            }
            is NotesEvent.SelectAllNotes -> {
                viewModelScope.launch {
                    val allIds = repository.getAllNoteIds(
                        searchQuery = listState.value.searchQuery,
                        projectId = listState.value.filteredProjectId,
                        isDecoy = isDecoySession
                    )
                    listDelegate.updateState { it.copy(selectedNoteIds = allIds.toImmutableList()) }
                }
            }
            is NotesEvent.TogglePinForSelectedNotes -> {
                viewModelScope.launch {
                    val allSelectedNotes = getSelectedNotes()
                    if (allSelectedNotes.isEmpty()) return@launch

                    val areNotesBeingPinned = allSelectedNotes.any { !it.note.isPinned }
                    
                    for (note in allSelectedNotes) {
                        repository.updateNote(note.note.copy(isPinned = areNotesBeingPinned))
                    }

                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                    val message = if (areNotesBeingPinned) {

                        if (allSelectedNotes.size > 1) "${allSelectedNotes.size} notes pinned" else "Note pinned"
                    } else {
                        if (allSelectedNotes.size > 1) "${allSelectedNotes.size} notes unpinned" else "Note unpinned"
                    }
                    _events.emit(NotesUiEvent.ShowToast(message))
                    updateWidgets()
                }
            }
            is NotesEvent.ToggleLockForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    if (selectedNotes.isEmpty()) return@launch
                    val areNotesBeingLocked = selectedNotes.firstOrNull()?.note?.isLocked == false
                    try {
                        for (note in selectedNotes) {
                            var noteToUpdate = note.note.copy(isLocked = areNotesBeingLocked)
                            if (!areNotesBeingLocked && note.note.isEncrypted) {
                                val decrypted = com.suvojeet.notenext.util.CryptoUtils.decryptNote(note.note)
                                noteToUpdate = decrypted.copy(isLocked = false)
                            }
                            repository.updateNote(noteToUpdate)
                            }
                            listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                            val message = if (areNotesBeingLocked) {

                            if (selectedNotes.size > 1) "${selectedNotes.size} notes locked" else "Note locked"
                        } else {
                            if (selectedNotes.size > 1) "${selectedNotes.size} notes unlocked" else "Note unlocked"
                        }
                        _events.emit(NotesUiEvent.ShowToast(message))
                        updateWidgets()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val errorMessage = if (areNotesBeingLocked) "Failed to lock notes" else "Failed to unlock notes: Authentication may be required"
                        _events.emit(NotesUiEvent.ShowToast(errorMessage))
                    }
                }
            }
            is NotesEvent.DeleteSelectedNotes -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                    }
                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                    _events.emit(NotesUiEvent.ShowToast("${selectedNotes.size} notes moved to Bin"))
                    updateWidgets()
                }
            }
            is NotesEvent.ArchiveSelectedNotes -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isArchived = !note.note.isArchived))
                    }
                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                    updateWidgets()
                }
            }
            is NotesEvent.ToggleImportantForSelectedNotes -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isImportant = !note.note.isImportant))
                    }
                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                }
            }
            is NotesEvent.ChangeColorForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(color = event.color))
                    }
                    listDelegate.updateState { it.copy(
                        selectedNoteIds = persistentListOf()
                    ) }
                    _events.emit(NotesUiEvent.ShowToast("Color updated"))
                }
            }
            is NotesEvent.CopySelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (noteWithAttachments in selectedNotes) {
                        val copiedNote = noteWithAttachments.note.copy(
                            id = 0, 
                            title = "${noteWithAttachments.note.title} (Copy)",
                            isDecoy = isDecoySession
                        )
                        val newNoteId = repository.insertNote(copiedNote)
                        require(newNoteId <= Int.MAX_VALUE) { "Note ID overflow" }
                        noteWithAttachments.attachments.forEach { attachment ->
                            repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId.toInt()))
                        }
                        // Copy checklist items
                        val newChecklistItems = noteWithAttachments.checklistItems.map { item ->
                            item.copy(id = java.util.UUID.randomUUID().toString(), noteId = newNoteId.toInt())
                        }
                        repository.insertChecklistItems(newChecklistItems)
                    }
                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                    val message = if (selectedNotes.size > 1) "${selectedNotes.size} notes copied" else "Note copied"
                    _events.emit(NotesUiEvent.ShowToast(message))
                }
            }
            is NotesEvent.SendSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    if (selectedNotes.isNotEmpty()) {
                        val title = if (selectedNotes.size == 1) selectedNotes.first().note.title else "Multiple Notes"
                        val contentBuilder = StringBuilder()
                        selectedNotes.forEachIndexed { index, it ->
                            contentBuilder.append("Title: ${it.note.title}\n\n")
                            if (it.note.noteType == NoteType.CHECKLIST) {
                                it.checklistItems.sortedBy { item -> item.position }.forEach { item ->
                                    val status = if (item.isChecked) "[x]" else "[ ]"
                                    contentBuilder.append("$status ${item.text}\n")
                                }
                            } else {
                                contentBuilder.append(HtmlConverter.htmlToPlainText(it.note.content))
                            }
                            
                            if (index < selectedNotes.size - 1) {
                                contentBuilder.append("\n\n---\n\n")
                            }
                        }
                        _events.emit(NotesUiEvent.SendNotes(title, contentBuilder.toString()))
                    }
                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                }
            }
            is NotesEvent.SetReminderForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
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
                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                    _events.emit(NotesUiEvent.ShowToast("Reminder set for ${selectedNotes.size} notes"))
                }
            }
            is NotesEvent.SetLabelForSelectedNotes -> {
                viewModelScope.launch {
                    if (event.label.isNotBlank()) {
                        repository.insertLabel(Label(event.label))
                    }
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(label = event.label))
                    }
                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                }
            }
            is NotesEvent.ExpandNote -> {
                lastCreatedNoteId = null
                viewModelScope.launch {
                    if (event.noteId != -1) {
                        noteUseCases.getNote(event.noteId)?.let { noteWithAttachments ->
                            val note = if (noteWithAttachments.note.isLocked) {
                                com.suvojeet.notenext.util.CryptoUtils.decryptNote(
                                    noteWithAttachments.note,
                                    event.authenticatedCipherTitle,
                                    event.authenticatedCipherContent
                                )
                            } else {                                noteWithAttachments.note
                            }

                            val content = if (note.noteType == NoteType.TEXT) {
                                HtmlConverter.htmlToAnnotatedString(note.content)
                            } else {
                                AnnotatedString("")
                            }
                            
                            val checklist = if (note.noteType == NoteType.CHECKLIST) {
                                noteWithAttachments.checklistItems.sortedBy { it.position }.map { item ->
                                    if (note.isLocked && item.isEncrypted) {
                                        com.suvojeet.notenext.util.CryptoUtils.decryptChecklistItem(item, isLocked = true)
                                    } else {
                                        item
                                    }
                                }.toImmutableList()
                            } else {
                                persistentListOf<ChecklistItem>()
                            }

                            viewModelScope.launch {
                                repository.getNoteVersions(event.noteId).collect { versions ->
                                    editorDelegate.updateState { it.copy(editingNoteVersions = versions.toImmutableList()) }
                                }
                            }
                            
                            val contentValue = TextFieldValue(content)
                            editorDelegate.reset(note.title, contentValue)

                            savedStateHandle[KEY_EXPANDED_NOTE_ID] = event.noteId
                            savedStateHandle[KEY_EDITING_TITLE] = note.title
                            savedStateHandle[KEY_EDITING_CONTENT] = note.content
                            savedStateHandle[KEY_NOTE_TYPE] = note.noteType.name

                            editorDelegate.updateState { it.copy(
                                expandedNoteId = event.noteId,
                                editingTitle = note.title,
                                editingContent = contentValue,
                                editingColor = note.color,
                                editingIsNewNote = false,
                                editingLastEdited = note.lastEdited,
                                isPinned = note.isPinned,
                                isArchived = note.isArchived,
                                editingLabel = note.label,
                                editingProjectId = note.projectId,
                                canUndo = false, // Will be updated by reset() above
                                canRedo = false,
                                linkPreviews = note.linkPreviews.toImmutableList(),
                                editingNoteType = note.noteType,
                                editingChecklist = checklist,
                                editingAttachments = noteWithAttachments.attachments.map { it.copy(tempId = java.util.UUID.randomUUID().toString()) }.toImmutableList(),
                                editingIsLocked = note.isLocked,
                                checklistInputValues = checklist.associate { item ->
                                    item.id to TextFieldValue(richTextController.parseMarkdownToAnnotatedString(item.text))
                                }.toImmutableMap(),
                                editingReminderTime = note.reminderTime,
                                editingRepeatOption = note.repeatOption,
                                summaryResult = note.aiSummary,
                                showSummaryDialog = false
                            ) }

                            // Compute linked notes immediately on open
                            viewModelScope.launch {
                                val linked = aiSuggestionsDelegate.findLinkedNotes(event.noteId, note.title, note.content)
                                editorDelegate.updateState { it.copy(linkedNotes = linked.toImmutableList()) }
                            }
                        }
                    } else {
                        editorDelegate.reset("", TextFieldValue())
                        savedStateHandle[KEY_EXPANDED_NOTE_ID] = -1
                        savedStateHandle[KEY_EDITING_TITLE] = ""
                        savedStateHandle[KEY_EDITING_CONTENT] = ""
                        savedStateHandle[KEY_NOTE_TYPE] = event.noteType.name
                        editorDelegate.updateState { it.copy(
                            expandedNoteId = -1,
                            editingTitle = "",
                            editingContent = TextFieldValue(),
                            editingColor = NoteGradients.NO_COLOR,
                            editingIsNewNote = true,
                            editingLastEdited = 0,
                            canUndo = false,
                            canRedo = false,
                            editingLabel = null,
                            linkPreviews = persistentListOf(),
                            editingNoteType = event.noteType,
                            editingChecklist = persistentListOf(),
                            checklistInputValues = persistentMapOf(),
                            editingAttachments = persistentListOf(),
                            editingIsLocked = false,
                            editingNoteVersions = persistentListOf(),
                            summaryResult = null,
                            showSummaryDialog = false
                        ) }
                    }
                }
            }
            is NotesEvent.OnToggleLockClick -> {
                viewModelScope.launch {
                    val currentLockState = editState.value.editingIsLocked
                    val newLockState = !currentLockState
                    editorDelegate.updateState { it.copy(editingIsLocked = newLockState) }
                    
                    // If note exists, update immediately using the current decrypted state
                    editState.value.expandedNoteId?.let { noteId ->
                         if (noteId != -1) {
                             saveNote(shouldCollapse = false)
                             _events.emit(NotesUiEvent.ShowToast(if (newLockState) "Note locked" else "Note unlocked"))
                             updateWidgets()
                         }
                    }
                }
            }
            is NotesEvent.CollapseNote -> {
                onEvent(NotesEvent.OnSaveNoteClick)
                savedStateHandle.remove<Int>(KEY_EXPANDED_NOTE_ID)
                savedStateHandle.remove<String>(KEY_EDITING_TITLE)
                savedStateHandle.remove<String>(KEY_EDITING_CONTENT)
                savedStateHandle.remove<String>(KEY_NOTE_TYPE)
            }

            is NotesEvent.AddChecklistItem -> {
                if (editState.value.editingChecklist.size >= 500) return
                val (updatedChecklist, newItemId) = ChecklistManager.addChecklistItem(editState.value.editingChecklist)
                editorDelegate.updateState { it.copy(
                    editingChecklist = updatedChecklist.toImmutableList(),
                    newlyAddedChecklistItemId = newItemId,
                    checklistInputValues = (editState.value.checklistInputValues + (newItemId to TextFieldValue(""))).toImmutableMap()
                ) }
                scheduleAutoSave()
            }
            is NotesEvent.AddChecklistItemAfter -> {
                if (editState.value.editingChecklist.size >= 500) return
                val (updatedChecklist, newItemId) = ChecklistManager.addChecklistItemAfter(editState.value.editingChecklist, event.itemId)
                editorDelegate.updateState { it.copy(
                    editingChecklist = updatedChecklist.toImmutableList(),
                    newlyAddedChecklistItemId = newItemId,
                    checklistInputValues = (editState.value.checklistInputValues + (newItemId to TextFieldValue(""))).toImmutableMap()
                ) }
                scheduleAutoSave()
            }
            is NotesEvent.SwapChecklistItems -> {
                val updatedList = ChecklistManager.swapItems(editState.value.editingChecklist, event.fromId, event.toId)
                if (updatedList != editState.value.editingChecklist) {
                    editorDelegate.updateState { it.copy(editingChecklist = updatedList.toImmutableList()) }
                    scheduleAutoSave()
                }
            }
            is NotesEvent.DeleteChecklistItem -> {
                val updatedChecklist = ChecklistManager.deleteItem(editState.value.editingChecklist, event.itemId)
                editorDelegate.updateState { it.copy(
                    editingChecklist = updatedChecklist.toImmutableList(),
                    checklistInputValues = (editState.value.checklistInputValues - event.itemId).toImmutableMap()
                ) }
                scheduleAutoSave()
            }
            is NotesEvent.IndentChecklistItem -> {
                val updatedChecklist = ChecklistManager.indentItem(editState.value.editingChecklist, event.itemId)
                editorDelegate.updateState { it.copy(editingChecklist = updatedChecklist.toImmutableList()) }
                scheduleAutoSave()
            }
            is NotesEvent.OutdentChecklistItem -> {
                val updatedChecklist = ChecklistManager.outdentItem(editState.value.editingChecklist, event.itemId)
                editorDelegate.updateState { it.copy(editingChecklist = updatedChecklist.toImmutableList()) }
                scheduleAutoSave()
            }

            is NotesEvent.OnChecklistItemCheckedChange -> {
                val updatedChecklist = ChecklistManager.changeItemCheckedState(editState.value.editingChecklist, event.itemId, event.isChecked)
                editorDelegate.updateState { it.copy(editingChecklist = updatedChecklist.toImmutableList()) }
                scheduleAutoSave()
            }
            is NotesEvent.OnChecklistItemTextChange -> {
                if (event.text.length > 2000) return
                val updatedChecklist = ChecklistManager.changeItemText(editState.value.editingChecklist, event.itemId, event.text)
                editorDelegate.updateState { it.copy(editingChecklist = updatedChecklist.toImmutableList()) }
                scheduleAutoSave()
            }
            is NotesEvent.OnTitleChange -> {
                editorDelegate.onTitleChange(event.title)
                scheduleAutoSave()
            }
            is NotesEvent.OnContentChange -> {
                editorDelegate.onContentChange(event.content, viewModelScope) {
                    saveNote(shouldCollapse = false)
                }
            }
            is NotesEvent.OnChecklistItemValueChange -> {
                if (event.value.text.length > 2000) return
                val updatedInputValues = editState.value.checklistInputValues.toMutableMap()
                updatedInputValues[event.itemId] = event.value

                // Check for styles at selection to update toolbar state
                 val selection = event.value.selection
                 val styles = if (selection.collapsed) {
                    if (selection.start > 0) {
                        event.value.annotatedString.spanStyles.filter {
                            it.start <= selection.start - 1 && it.end >= selection.start
                        }
                    } else {
                        persistentListOf()
                    }
                } else {
                    event.value.annotatedString.spanStyles.filter {
                        maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                    }
                }

                editorDelegate.updateState { it.copy(
                    checklistInputValues = updatedInputValues.toImmutableMap(),
                     isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                     isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                     isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                ) }
                scheduleAutoSave()
                
                // Async update for persistence model
                viewModelScope.launch {
                    val updatedText = HtmlConverter.annotatedStringToHtml(event.value.annotatedString).let {
                        com.suvojeet.notenext.data.MarkdownExporter.convertHtmlToMarkdown(it)
                    }

                    val updatedChecklist = editState.value.editingChecklist.map {
                        if (it.id == event.itemId) it.copy(text = updatedText) else it
                    }
                    editorDelegate.updateState { it.copy(editingChecklist = updatedChecklist.toImmutableList()) }
                }
            }
            is NotesEvent.OnChecklistItemFocus -> {
                editorDelegate.updateState { it.copy(focusedChecklistItemId = event.itemId) }
                // Update active styles based on the focused item's cursor position handled in ValueChange or just reset/check here
                val value = editState.value.checklistInputValues[event.itemId]
                if (value != null) {
                     val selection = value.selection
                     val styles = value.annotatedString.spanStyles.filter {
                        maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                    }
                     editorDelegate.updateState { it.copy(
                         isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                         isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                         isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                     ) }
                }
            }
            is NotesEvent.ApplyStyleToContent -> {
                if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                    val focusedId = editState.value.focusedChecklistItemId
                    if (focusedId != null) {
                        val currentValue = editState.value.checklistInputValues[focusedId]
                        if (currentValue != null) {
                             val result = richTextController.toggleStyle(
                                currentValue,
                                event.style,
                                persistentSetOf(), // We don't track activeStyles per item easily yet, relies on result
                                editState.value.isBoldActive,
                                editState.value.isItalicActive,
                                editState.value.isUnderlineActive
                            )
                            if (result.updatedContent != null) {
                                onEvent(NotesEvent.OnChecklistItemValueChange(focusedId, result.updatedContent))
                            }
                        }
                    }
                } else {
                    editorDelegate.applyStyle(event.style)
                }
            }
            is NotesEvent.ApplyBulletedList -> {
                val updatedContent = richTextController.toggleBulletedList(editState.value.editingContent)
                editorDelegate.reset(editState.value.editingTitle, updatedContent)
                editorDelegate.updateState { it.copy(
                    editingContent = updatedContent
                ) }
                scheduleAutoSave()
            }
            is NotesEvent.ApplyHeadingStyle -> {
                val updatedContent = richTextController.applyHeading(editState.value.editingContent, event.level)

                if (updatedContent == null) {
                    // Selection is collapsed, update active styles for future typing
                    val newActiveStyles = mutableSetOf<SpanStyle>()
                    if (event.level != 0) {
                        newActiveStyles.add(richTextController.getHeadingStyle(event.level))
                    }
                    editorDelegate.updateState { it.copy(
                        activeHeadingStyle = event.level,
                        activeStyles = newActiveStyles.toImmutableSet(),
                        isBoldActive = false,
                        isItalicActive = false,
                        isUnderlineActive = false
                    ) }
                } else {
                    // Applied to selection
                    editorDelegate.reset(editState.value.editingTitle, updatedContent)

                    editorDelegate.updateState { it.copy(
                        editingContent = updatedContent,
                        activeHeadingStyle = event.level
                    ) }
                }
            }
            is NotesEvent.OnColorChange -> {
                editorDelegate.updateState { it.copy(editingColor = event.color) }
                scheduleAutoSave()
            }
            is NotesEvent.OnLabelChange -> {
                viewModelScope.launch {
                    repository.insertLabel(Label(event.label))
                    editorDelegate.updateState { it.copy(editingLabel = event.label) }
                }
            }
            is NotesEvent.OnTogglePinClick -> {
                val newPinnedState = !editState.value.isPinned
                editorDelegate.updateState { it.copy(isPinned = newPinnedState) }
                viewModelScope.launch {
                    saveNote(shouldCollapse = false)
                    val message = if (newPinnedState) "Note pinned" else "Note unpinned"
                    _events.emit(NotesUiEvent.ShowToast(message))
                }
            }
            is NotesEvent.OnToggleArchiveClick -> {
                viewModelScope.launch {
                    editState.value.expandedNoteId?.let { noteId ->
                        repository.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isArchived = !note.note.isArchived)
                            repository.updateNote(updatedNote)
                            val updatedNotesList = listState.value.notes.map { if (it.note.id == updatedNote.id) it.copy(note = updatedNote.toNoteSummary()) else it }.toImmutableList()
                            editorDelegate.updateState { it.copy(
                                isArchived = updatedNote.isArchived,
                            ) }
                            listDelegate.updateState { it.copy(
                                notes = updatedNotesList
                            ) }
                            updateWidgets()
                        }
                    }
                }
            }
            is NotesEvent.OnUndoClick -> {
                editorDelegate.undo()
            }
            is NotesEvent.OnRedoClick -> {
                editorDelegate.redo()
            }
            is NotesEvent.OnSaveNoteClick -> {
                viewModelScope.launch {
                    saveNote(shouldCollapse = true)
                }
            }
            is NotesEvent.OnDeleteNoteClick -> {
                viewModelScope.launch {
                    editState.value.expandedNoteId?.let {
                        if (it != -1) {
                            repository.getNoteById(it)?.let { note ->
                                repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                                _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
                                updateWidgets()
                            }
                        }
                    }
                    editorDelegate.updateState { it.copy(expandedNoteId = null) }
                }
            }
            is NotesEvent.OnCopyCurrentNoteClick -> {
                viewModelScope.launch {
                    editState.value.expandedNoteId?.let {
                        repository.getNoteById(it)?.let { noteWithAttachments ->
                            val copiedNote = noteWithAttachments.note.copy(id = 0, title = "${noteWithAttachments.note.title} (Copy)", createdAt = System.currentTimeMillis(), lastEdited = System.currentTimeMillis())
                            val newNoteId = repository.insertNote(copiedNote)
                            require(newNoteId <= Int.MAX_VALUE) { "Note ID overflow" }
                            noteWithAttachments.attachments.forEach { attachment ->
                                repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId.toInt()))
                            }
                            _events.emit(NotesUiEvent.ShowToast("Note copied"))
                        }
                    }
                }
            }
            is NotesEvent.OnAddLabelsToCurrentNoteClick -> {
                editorDelegate.updateState { it.copy(showLabelDialog = true) }
            }
            is NotesEvent.DismissLabelDialog -> {
                editorDelegate.updateState { it.copy(showLabelDialog = false) }
            }
            is NotesEvent.FilterByLabel -> {
                listDelegate.updateState { it.copy(filteredLabel = event.label) }
            }
            is NotesEvent.FilterByProject -> {
                listDelegate.setProjectId(event.projectId)
            }
            is NotesEvent.ToggleLayout -> {
                val newLayout = if (listState.value.layoutType == LayoutType.GRID) LayoutType.LIST else LayoutType.GRID
                listDelegate.updateState { it.copy(layoutType = newLayout) }
            }
            is NotesEvent.SortNotes -> {
                listDelegate.setSortType(event.sortType)
            }
            is NotesEvent.OnRemoveLinkPreview -> {
                val updatedLinkPreviews = editState.value.linkPreviews.filter { it.url != event.url }.toImmutableList()
                editorDelegate.updateState { it.copy(linkPreviews = updatedLinkPreviews) }
                viewModelScope.launch {
                    _events.emit(NotesUiEvent.LinkPreviewRemoved)
                }
            }
            is NotesEvent.OnInsertLink -> {
                val content = editState.value.editingContent
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
                    editorDelegate.updateState { it.copy(editingContent = newTextFieldValue) }
                }
            }
            is NotesEvent.ClearNewlyAddedChecklistItemId -> {
                editorDelegate.updateState { it.copy(newlyAddedChecklistItemId = null) }
            }
            is NotesEvent.AddAttachment -> {
                val type = when {
                    event.mimeType.startsWith("image") -> AttachmentType.IMAGE
                    event.mimeType.startsWith("video") -> AttachmentType.VIDEO
                    event.mimeType.startsWith("audio") -> AttachmentType.AUDIO
                    else -> AttachmentType.FILE
                }
                val attachment = Attachment(
                    noteId = editState.value.expandedNoteId ?: -1,
                    uri = event.uri,
                    type = type,
                    mimeType = event.mimeType,
                    tempId = java.util.UUID.randomUUID().toString()
                )
                editorDelegate.updateState { it.copy(editingAttachments = (editState.value.editingAttachments + attachment).toImmutableList()) }
                scheduleAutoSave()
            }
            is NotesEvent.OnLinkDetected -> {
                linkDetectionJob?.cancel()
                linkDetectionJob = viewModelScope.launch {
                    delay(1000L) // 1s delay - only fetch when user stops typing
                    
                    val existingLinkPreviews = editState.value.linkPreviews
                    if (existingLinkPreviews.none { it.url == event.url }) {
                        val preview = linkPreviewRepository.getLinkPreview(event.url)
                        onEvent(NotesEvent.OnLinkPreviewFetched(
                            url = preview.url,
                            title = preview.title,
                            description = preview.description,
                            imageUrl = preview.imageUrl
                        ))
                    }
                }
            }
            is NotesEvent.OnLinkPreviewFetched -> {
                val newPreview = LinkPreview(event.url, event.title, event.description, event.imageUrl)
                val updatedPreviews = (editState.value.linkPreviews + newPreview).distinctBy { it.url }.toImmutableList()
                editorDelegate.updateState { it.copy(linkPreviews = updatedPreviews) }
                scheduleAutoSave()
            }
            is NotesEvent.RemoveAttachment -> {
                viewModelScope.launch {
                    val attachmentToRemove = editState.value.editingAttachments.firstOrNull { it.tempId == event.tempId }
                    attachmentToRemove?.let {
                        if (it.id != 0) { // Only delete from DB if it has a real ID
                            repository.deleteAttachmentById(it.id)
                        }
                        val updatedAttachments = editState.value.editingAttachments.filter { attachment -> attachment.tempId != event.tempId }.toImmutableList()
                        editorDelegate.updateState { it.copy(editingAttachments = updatedAttachments) }
                        scheduleAutoSave()
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
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(projectId = event.projectId))
                    }
                    listDelegate.updateState { it.copy(selectedNoteIds = persistentListOf()) }
                    _events.emit(NotesUiEvent.ShowToast("${selectedNotes.size} notes moved to project"))
                }
            }
            is NotesEvent.OnToggleNoteType -> {
                val currentType = editState.value.editingNoteType
                if (currentType == NoteType.TEXT) {
                    // Convert TEXT to CHECKLIST
                    val lines = editState.value.editingContent.text.split("\n")
                    val checklistItems = lines.filter { it.isNotBlank() }.mapIndexed { index, text ->
                        ChecklistItem(text = text.trim(), isChecked = false, position = index)
                    }
                    // If empty, add one empty item
                    val finalItems = if (checklistItems.isEmpty()) listOf(ChecklistItem(text = "", isChecked = false, position = 0)) else checklistItems
                    
                    savedStateHandle[KEY_NOTE_TYPE] = NoteType.CHECKLIST.name
                    savedStateHandle[KEY_EDITING_CONTENT] = ""

                    editorDelegate.updateState { it.copy(
                        editingNoteType = NoteType.CHECKLIST,
                        editingChecklist = finalItems.toImmutableList(),
                        editingContent = TextFieldValue("") // Clear text content
                    ) }
                } else {
                    // Convert CHECKLIST to TEXT
                    val textContent = editState.value.editingChecklist.joinToString("\n") { it.text }
                    savedStateHandle[KEY_NOTE_TYPE] = NoteType.TEXT.name
                    savedStateHandle[KEY_EDITING_CONTENT] = textContent

                    editorDelegate.updateState { it.copy(
                        editingNoteType = NoteType.TEXT,
                        editingContent = TextFieldValue(textContent),
                        editingChecklist = persistentListOf()
                    ) }
                }
            }
            is NotesEvent.ConvertToTodo -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    val title = editState.value.editingTitle
                    val content = editState.value.editingContent.text
                    val noteType = editState.value.editingNoteType
                    val projectId = editState.value.editingProjectId
                    
                    val maxPos = todoRepository.getMaxPosition()
                    val todo = com.suvojeet.notenext.data.TodoItem(
                        title = if (title.isBlank()) "Converted Note" else title,
                        description = if (noteType == NoteType.TEXT) content else "",
                        projectId = projectId,
                        position = maxPos + 1,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    val todoId = todoRepository.insertTodo(todo).toInt()
                    
                    if (noteType == NoteType.CHECKLIST) {
                        val subtasks = editState.value.editingChecklist.map { item ->
                            com.suvojeet.notenext.data.TodoSubtask(
                                todoId = todoId,
                                text = item.text,
                                isChecked = item.isChecked,
                                position = item.position
                            )
                        }
                        todoRepository.insertSubtasks(subtasks)
                    }
                    
                    // Bin the note after conversion
                    val currentNoteId = editState.value.expandedNoteId
                    if (currentNoteId != null && currentNoteId != -1) {
                        repository.getNoteById(currentNoteId)?.let { noteWithAttachments ->
                            repository.updateNote(noteWithAttachments.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                        }
                    }
                    
                    onEvent(NotesEvent.CollapseNote)
                    _events.emit(NotesUiEvent.ShowToast("Converted to Todo successfully"))
                }
            }
            is NotesEvent.ToggleCheckedItemsExpanded -> {
                editorDelegate.updateState { it.copy(
                    isCheckedItemsExpanded = !editState.value.isCheckedItemsExpanded
                ) }
            }
            is NotesEvent.SummarizeNote -> {
                val content = if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                    editState.value.editingChecklist.joinToString("\n") { it.text }
                } else {
                    editState.value.editingContent.text
                }

                if (content.isNotBlank()) {
                    if (editState.value.summaryResult != null) {
                         editorDelegate.updateState { it.copy(showSummaryDialog = true) }
                    } else {
                         viewModelScope.launch {
                             if (!aiFeatureGate.isEnabled(com.suvojeet.notenext.data.ai.AIFeature.SUMMARIZE)) {
                                 _events.emit(NotesUiEvent.ShowToast("Summarize is disabled in AI Settings"))
                                 return@launch
                             }
                             aiDelegate.summarize(content, viewModelScope, _events) { transform ->
                                 editorDelegate.updateState(transform)
                             }
                         }
                    }
                }
            }
            is NotesEvent.ClearSummary -> {
                editorDelegate.updateState { it.copy(showSummaryDialog = false) }
            }
            is NotesEvent.DeleteAllCheckedItems -> {
                val updatedChecklist = ChecklistManager.deleteAllCheckedItems(editState.value.editingChecklist)
                editorDelegate.updateState { it.copy(editingChecklist = updatedChecklist.toImmutableList()) }
            }
            is NotesEvent.CreateNoteFromSharedText -> {
                editorDelegate.reset("", TextFieldValue(event.text))
                editorDelegate.updateState { it.copy(
                    expandedNoteId = -1,
                    editingTitle = "",
                    editingContent = TextFieldValue(event.text),
                    editingColor = NoteGradients.NO_COLOR,
                    editingIsNewNote = true,
                    editingLastEdited = 0,
                    editingLabel = null,
                    linkPreviews = persistentListOf(),
                    editingNoteType = NoteType.TEXT,
                    editingChecklist = persistentListOf(),
                    editingAttachments = persistentListOf()
                ) }
            }
            is NotesEvent.SetInitialTitle -> {
                val safeTitle = if (event.title.length > 100) event.title.take(100) else event.title
                editorDelegate.updateState { it.copy(
                    editingTitle = safeTitle
                ) }
            }
            is NotesEvent.OnReminderChange -> {
                editorDelegate.updateState { it.copy(
                    editingReminderTime = event.time,
                    editingRepeatOption = event.repeatOption
                ) }
                scheduleAutoSave()
            }
            is NotesEvent.OnExpiryChange -> {
                editorDelegate.updateState { it.copy(
                    editingExpiryTime = event.expiryTime
                ) }
                scheduleAutoSave()
            }
            is NotesEvent.ExportNote -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val contentToExport = if (event.format == "MD") {
                            if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                                editState.value.editingChecklist.joinToString("\n") { 
                                    (if (it.isChecked) "- [x] " else "- [ ] ") + it.text 
                                }
                            } else {
                                HtmlConverter.annotatedStringToHtml(editState.value.editingContent.annotatedString).let {
                                    com.suvojeet.notenext.data.MarkdownExporter.convertHtmlToMarkdown(it)
                                }
                            }
                        } else {
                            // TXT (Plain Text)
                            if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                                editState.value.editingChecklist.joinToString("\n") { 
                                    (if (it.isChecked) "[x] " else "[ ] ") + it.text 
                                }
                            } else {
                                editState.value.editingContent.text
                            }
                        }

                        context.contentResolver.openOutputStream(event.uri)?.use { outputStream ->
                            outputStream.write(contentToExport.toByteArray())
                        }
                        _events.emit(NotesUiEvent.ShowToast("Exported successfully"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _events.emit(NotesUiEvent.ShowToast("Export failed: ${e.message}"))
                    }
                }
            }
            // ─── AI advanced features (Tone Rewriter, Auto-tag, Smart Reminder, Linked Notes) ──
            is NotesEvent.PickToneRewrite -> {
                runToneRewrite(event.tone)
            }
            is NotesEvent.RetryToneRewrite -> {
                editState.value.toneRewriteSelectedTone?.let { runToneRewrite(it) }
            }
            is NotesEvent.AcceptToneRewrite -> {
                val rewritten = editState.value.toneRewriteResult ?: return
                editorDelegate.updateState { st ->
                    st.copy(
                        editingContent = TextFieldValue(rewritten),
                        toneRewriteResult = null,
                        toneRewriteSelectedTone = null
                    )
                }
                scheduleAutoSave()
            }
            is NotesEvent.AcceptSuggestedLabel -> {
                val label = event.label
                viewModelScope.launch {
                    runCatching { repository.insertLabel(Label(name = label)) }
                    editorDelegate.updateState { st ->
                        st.copy(
                            editingLabel = label,
                            suggestedLabels = persistentListOf()
                        )
                    }
                    aiSuggestionsDelegate.recordLabelSuggestionAccepted(true)
                    scheduleAutoSave()
                }
            }
            is NotesEvent.DismissSuggestedLabels -> {
                viewModelScope.launch {
                    aiSuggestionsDelegate.recordLabelSuggestionAccepted(false)
                }
                editorDelegate.updateState { it.copy(suggestedLabels = persistentListOf()) }
            }
            is NotesEvent.AcceptExtractedReminder -> {
                val r = editState.value.extractedReminder ?: return
                editorDelegate.updateState { st ->
                    st.copy(
                        editingReminderTime = r.timestampMs,
                        extractedReminder = null
                    )
                }
                viewModelScope.launch {
                    aiSuggestionsDelegate.recordReminderSuggestionAccepted(true)
                }
                scheduleAutoSave()
            }
            is NotesEvent.DismissExtractedReminder -> {
                viewModelScope.launch {
                    aiSuggestionsDelegate.recordReminderSuggestionAccepted(false)
                }
                editorDelegate.updateState { it.copy(extractedReminder = null) }
            }
            is NotesEvent.OpenLinkedNote -> {
                onEvent(NotesEvent.ExpandNote(event.noteId))
            }
            else -> {
                // Handle any other events or do nothing
            }
        }
    }

    private fun runToneRewrite(tone: com.suvojeet.notenext.data.ai.ToneOption) {
        val source = editState.value.editingContent.text
        if (source.isBlank()) {
            viewModelScope.launch { _events.emit(NotesUiEvent.ShowToast("Nothing to rewrite")) }
            return
        }
        editorDelegate.updateState { it.copy(
            toneRewriteSelectedTone = tone,
            isToneRewriting = true,
            toneRewriteError = null,
            toneRewriteResult = null
        ) }
        viewModelScope.launch {
            when (val result = aiSuggestionsDelegate.rewriteTone(source, tone)) {
                is com.suvojeet.notenext.data.ai.AIResult.Success -> {
                    editorDelegate.updateState { it.copy(
                        isToneRewriting = false,
                        toneRewriteResult = result.data
                    ) }
                }
                is com.suvojeet.notenext.data.ai.AIResult.AuthError ->
                    editorDelegate.updateState { it.copy(isToneRewriting = false, toneRewriteError = result.message) }
                is com.suvojeet.notenext.data.ai.AIResult.NetworkError ->
                    editorDelegate.updateState { it.copy(isToneRewriting = false, toneRewriteError = "Network: ${result.message}") }
                is com.suvojeet.notenext.data.ai.AIResult.RateLimited ->
                    editorDelegate.updateState { it.copy(isToneRewriting = false, toneRewriteError = "Rate limited. Try again in ${result.retryAfterSeconds}s.") }
                is com.suvojeet.notenext.data.ai.AIResult.ProviderError ->
                    editorDelegate.updateState { it.copy(isToneRewriting = false, toneRewriteError = result.message) }
                is com.suvojeet.notenext.data.ai.AIResult.AllProvidersFailed ->
                    editorDelegate.updateState { it.copy(isToneRewriting = false, toneRewriteError = "All providers failed") }
            }
        }
    }

    /**
     * Fires after every successful note save (called from saveNote()).
     * Kicks off auto-tag and smart-reminder suggestions in the background.
     * Both gated by AIFeatureGate inside the delegate.
     */
    private fun fireAISuggestionsAfterSave(noteId: Int, title: String, content: String) {
        viewModelScope.launch {
            // Auto-tag
            val labelsResult = aiSuggestionsDelegate.suggestLabels("$title\n$content")
            if (labelsResult is com.suvojeet.notenext.data.ai.AIResult.Success && labelsResult.data.isNotEmpty()) {
                editorDelegate.updateState { it.copy(suggestedLabels = labelsResult.data.toImmutableList()) }
            }
            // Smart reminder
            val reminderResult = aiSuggestionsDelegate.extractReminders(content)
            if (reminderResult is com.suvojeet.notenext.data.ai.AIResult.Success && reminderResult.data.isNotEmpty()) {
                editorDelegate.updateState { it.copy(extractedReminder = reminderResult.data.first()) }
            }
            // Linked notes
            val linked = aiSuggestionsDelegate.findLinkedNotes(noteId, title, content)
            editorDelegate.updateState { it.copy(linkedNotes = linked.toImmutableList()) }
        }
    }


    private suspend fun getSelectedNotes(): List<NoteWithAttachments> {
        val selectedIds = listState.value.selectedNoteIds
        return selectedIds.mapNotNull { id ->
            repository.getNoteById(id)
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
        val expandedId = editState.value.expandedNoteId
        val externalUri = editState.value.externalUri
        
        if (expandedId == null) return
        
        // Handle external files separately
        if (externalUri != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val content = if (editState.value.editingNoteType == NoteType.TEXT) {
                         editState.value.editingContent.text
                    } else {
                        editState.value.editingChecklist.joinToString("\n") { (if (it.isChecked) "[x] " else "[ ] ") + it.text }
                    }
                    context.contentResolver.openOutputStream(externalUri, "rwt")?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _events.emit(NotesUiEvent.ShowToast("Failed to save external file: ${e.message}"))
                }
            }
            if (shouldCollapse) {
                 editorDelegate.updateState { it.copy(
                    expandedNoteId = null,
                    externalUri = null,
                    editingTitle = "",
                    editingContent = TextFieldValue()
                ) }
            }
            return
        }

        // If it's a new note (-1), check if we already have a real ID from a previous auto-save
        val noteId = if (expandedId == -1 && lastCreatedNoteId != null) lastCreatedNoteId!! else expandedId

        val title = editState.value.editingTitle
        val content = if (editState.value.editingNoteType == NoteType.TEXT) {
            HtmlConverter.annotatedStringToHtml(editState.value.editingContent.annotatedString)
        } else {
            ""
        }

        if (title.isBlank() && (editState.value.editingNoteType == NoteType.TEXT && content.isBlank() || editState.value.editingNoteType == NoteType.CHECKLIST && editState.value.editingChecklist.all { it.text.isBlank() })) {
            if (noteId != -1) { // It's an existing note, so delete it
                repository.getNoteById(noteId)?.let { repository.updateNote(it.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())) }
            }
        } else {
            val currentTime = System.currentTimeMillis()
            val note = if (noteId == -1) { // New note (truly new, first save)
                Note(
                    title = title,
                    content = content,
                    createdAt = currentTime,
                    lastEdited = currentTime,
                    color = editState.value.editingColor,
                    isPinned = editState.value.isPinned,
                    isArchived = editState.value.isArchived,
                    label = editState.value.editingLabel,
                    linkPreviews = editState.value.linkPreviews,
                    noteType = editState.value.editingNoteType,
                    isLocked = editState.value.editingIsLocked,
                    reminderTime = editState.value.editingReminderTime,
                    repeatOption = editState.value.editingRepeatOption,
                    expiryTime = editState.value.editingExpiryTime,
                    isDecoy = isDecoySession
                )
            } else { // Existing note
                repository.getNoteById(noteId)?.let { existingNote ->
                    existingNote.note.copy(
                        title = title,
                        content = content,
                        lastEdited = currentTime,
                        color = editState.value.editingColor,
                        isPinned = editState.value.isPinned,
                        isArchived = editState.value.isArchived,
                        label = editState.value.editingLabel,
                        linkPreviews = editState.value.linkPreviews,
                        noteType = editState.value.editingNoteType,
                        isLocked = editState.value.editingIsLocked,
                        reminderTime = editState.value.editingReminderTime,
                        repeatOption = editState.value.editingRepeatOption,
                        expiryTime = editState.value.editingExpiryTime,
                        aiSummary = editState.value.summaryResult,
                        isEncrypted = false,
                        iv = null
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
                    check(!note.isEncrypted) { "Attempting to save encrypted note from ViewModel — decrypt first." }
                    repository.updateNote(note)
                    noteId.toLong() // Convert Int to Long for consistency
                }
                require(currentNoteId <= Int.MAX_VALUE) { "Note ID overflow" }

                if (editState.value.editingReminderTime != null) {
                    alarmScheduler.schedule(note.copy(id = currentNoteId.toInt()))
                } else if (noteId != -1) {
                    alarmScheduler.cancel(note.copy(id = currentNoteId.toInt()))
                }

                // Handle Checklist Items
                if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                    val checklistItems = editState.value.editingChecklist.mapIndexed { index, item ->
                        item.copy(noteId = currentNoteId.toInt(), position = index)
                    }
                    repository.deleteChecklistForNote(currentNoteId.toInt())
                    repository.insertChecklistItems(checklistItems)
                }

                // Handle attachments
                val existingAttachmentsInDb = if (noteId != -1) {
                    repository.getNoteById(noteId)?.attachments ?: persistentListOf()
                } else {
                    persistentListOf()
                }

                val attachmentsToAdd = editState.value.editingAttachments.filter { uiAttachment ->
                    existingAttachmentsInDb.none { dbAttachment ->
                        dbAttachment.uri == uiAttachment.uri && dbAttachment.type == uiAttachment.type
                    }
                }

                val attachmentsToRemove = existingAttachmentsInDb.filter { dbAttachment ->
                    editState.value.editingAttachments.none { uiAttachment ->
                        uiAttachment.uri == dbAttachment.uri && uiAttachment.type == dbAttachment.type
                    }
                }

                attachmentsToRemove.forEach { attachment ->
                    repository.deleteAttachment(attachment)
                }

                attachmentsToAdd.forEach { attachment ->
                    repository.insertAttachment(attachment.copy(noteId = currentNoteId.toInt()))
                }

                // If it was a new note, we now have a real ID. 
                // We update editingIsNewNote to false so next saves know it's not new anymore.
                // But we DO NOT update expandedNoteId yet if it was -1, to avoid the 'jolt' in AnimatedContent.
                if (expandedId == -1 && lastCreatedNoteId == null) {
                    editorDelegate.updateState { it.copy(
                        editingIsNewNote = false,
                        expandedNoteId = -1, // Keep it -1 to avoid triggering NoteTransition in NotesScreen
                        editingLastEdited = currentTime
                    ) }
                    lastCreatedNoteId = currentNoteId.toInt()
                    savedStateHandle[KEY_EXPANDED_NOTE_ID] = lastCreatedNoteId
                } else {
                    // Update lastEdited time so UI (MoreOptionsSheet) shows it
                    editorDelegate.updateState { it.copy(
                        editingLastEdited = currentTime
                    ) }
                }

                // Fire AI suggestions (auto-tag, smart reminder, linked notes) — gated inside delegate.
                // Skipped for empty-collapse and for new-note-with-no-content cases above.
                fireAISuggestionsAfterSave(
                    noteId = currentNoteId.toInt(),
                    title = title,
                    content = content
                )
            }
        }

        if (shouldCollapse) {
            lastCreatedNoteId = null
            // Reset editing state and collapse
            editorDelegate.updateState { it.copy(
                expandedNoteId = null,
                editingTitle = "",
                editingContent = TextFieldValue(),
                editingColor = NoteGradients.NO_COLOR,
                editingIsNewNote = true,
                editingLastEdited = 0,
                canUndo = false,
                canRedo = false,
                isPinned = false,
                isArchived = false,
                editingLabel = null,
                editingProjectId = null,
                isBoldActive = false,
                isItalicActive = false,
                isUnderlineActive = false,
                activeStyles = persistentSetOf(),
                linkPreviews = persistentListOf(),
                editingChecklist = persistentListOf(),
                editingAttachments = persistentListOf(),
                editingReminderTime = null,
                editingRepeatOption = null
            ) }
        }
        updateWidgets()
    }
}

