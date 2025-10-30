
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.SavedStateHandle
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
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.ProjectDao
import com.suvojeet.notenext.ui.notes.SortType
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

class ProjectNotesViewModel(
    private val noteDao: NoteDao,
    private val projectDao: ProjectDao,
    private val labelDao: LabelDao,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val alarmScheduler: AlarmScheduler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectNotesState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ProjectNotesUiEvent>()
    val events = _events.asSharedFlow()

    private var recentlyDeletedNote: Note? = null

    private val projectId: Int = savedStateHandle.get<String>("projectId")?.toIntOrNull() ?: -1

    init {
        if (projectId != -1) {
            viewModelScope.launch {
                projectDao.getProjectById(projectId)?.let { project ->
                    _state.value = _state.value.copy(projectName = project.name)
                }
            }

            combine(noteDao.getNotesByProjectId(projectId), labelDao.getLabels(), _state) { notes, labels, state ->
                val sortedNotes = when (state.sortType) {
                    SortType.DATE_CREATED -> notes.sortedByDescending { it.note.createdAt }
                    SortType.DATE_MODIFIED -> notes.sortedByDescending { it.note.lastEdited }
                    SortType.TITLE -> notes.sortedBy { it.note.title }
                }
                _state.value = state.copy(
                    notes = sortedNotes,
                    labels = labels.map { it.name }
                )
            }.launchIn(viewModelScope)
        }
    }

    fun onEvent(event: ProjectNotesEvent) {
        when (event) {
            is ProjectNotesEvent.DeleteNote -> {
                viewModelScope.launch {
                    val noteToBin = event.note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())
                    noteDao.updateNote(noteToBin)
                    recentlyDeletedNote = event.note.note
                    _events.emit(ProjectNotesUiEvent.ShowToast("Note moved to Bin"))
                }
            }
            is ProjectNotesEvent.RestoreNote -> {
                viewModelScope.launch {
                    recentlyDeletedNote?.let { restoredNote ->
                        noteDao.updateNote(restoredNote.copy(isBinned = false))
                        recentlyDeletedNote = null
                    }
                }
            }
            is ProjectNotesEvent.ToggleNoteSelection -> {
                val selectedIds = state.value.selectedNoteIds.toMutableList()
                if (selectedIds.contains(event.noteId)) {
                    selectedIds.remove(event.noteId)
                } else {
                    selectedIds.add(event.noteId)
                }
                _state.value = state.value.copy(selectedNoteIds = selectedIds)
            }
            is ProjectNotesEvent.ClearSelection -> {
                _state.value = state.value.copy(selectedNoteIds = emptyList())
            }
            is ProjectNotesEvent.TogglePinForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    val areNotesBeingPinned = selectedNotes.firstOrNull()?.note?.isPinned == false
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.note.copy(isPinned = areNotesBeingPinned))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    val message = if (areNotesBeingPinned) {
                        if (selectedNotes.size > 1) "${selectedNotes.size} notes pinned" else "Note pinned"
                    } else {
                        if (selectedNotes.size > 1) "${selectedNotes.size} notes unpinned" else "Note unpinned"
                    }
                    _events.emit(ProjectNotesUiEvent.ShowToast(message))
                }
            }
            is ProjectNotesEvent.DeleteSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        noteDao.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    _events.emit(ProjectNotesUiEvent.ShowToast("${selectedNotes.size} notes moved to Bin"))
                }
            }
            is ProjectNotesEvent.ArchiveSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.note.copy(isArchived = !note.note.isArchived))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is ProjectNotesEvent.ToggleImportantForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.note.copy(isImportant = !note.note.isImportant))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is ProjectNotesEvent.ChangeColorForSelectedNotes -> {
                // TODO: Implement color picker
            }
            is ProjectNotesEvent.CopySelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.note.copy(id = 0, title = "${note.note.title} (Copy)"))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    val message = if (selectedNotes.size > 1) "${selectedNotes.size} notes copied" else "Note copied"
                    _events.emit(ProjectNotesUiEvent.ShowToast(message))
                }
            }
            is ProjectNotesEvent.SendSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    if (selectedNotes.isNotEmpty()) {
                        val title = if (selectedNotes.size == 1) selectedNotes.first().note.title else "Multiple Notes"
                        val content = selectedNotes.joinToString("\n\n---\n\n") { "Title: ${it.note.title}\n\n${HtmlConverter.htmlToPlainText(it.note.content)}" }
                        _events.emit(ProjectNotesUiEvent.SendNotes(title, content))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is ProjectNotesEvent.SetReminderForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    val reminderDateTime = LocalDateTime.of(event.date, event.time)
                    val reminderMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    for (noteWithAttachments in selectedNotes) {
                        val updatedNote = noteWithAttachments.note.copy(
                            reminderTime = reminderMillis,
                            repeatOption = event.repeatOption.name // Store enum name as string
                        )
                        noteDao.updateNote(updatedNote)
                        alarmScheduler.schedule(updatedNote)
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                    _events.emit(ProjectNotesUiEvent.ShowToast("Reminder set for ${selectedNotes.size} notes"))
                }
            }
            is ProjectNotesEvent.SetLabelForSelectedNotes -> {
                viewModelScope.launch {
                    if (event.label.isNotBlank()) {
                        labelDao.insertLabel(Label(event.label))
                    }
                    val selectedNotes = state.value.notes.filter { state.value.selectedNoteIds.contains(it.note.id) }
                    for (note in selectedNotes) {
                        noteDao.insertNote(note.note.copy(label = event.label))
                    }
                    _state.value = state.value.copy(selectedNoteIds = emptyList())
                }
            }
            is ProjectNotesEvent.ExpandNote -> {
                viewModelScope.launch {
                    if (event.noteId != -1) {
                        noteDao.getNoteById(event.noteId)?.let { noteWithAttachments ->
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
            is ProjectNotesEvent.CollapseNote -> {
                onEvent(ProjectNotesEvent.OnSaveNoteClick)
            }
            is ProjectNotesEvent.AddChecklistItem -> {
                val newItem = ChecklistItem(text = "", isChecked = false)
                val updatedChecklist = state.value.editingChecklist + newItem
                _state.value = state.value.copy(
                    editingChecklist = updatedChecklist,
                    newlyAddedChecklistItemId = newItem.id
                )
            }
            is ProjectNotesEvent.DeleteChecklistItem -> {
                val updatedChecklist = state.value.editingChecklist.filterNot { it.id == event.itemId }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
            }
            is ProjectNotesEvent.OnChecklistItemCheckedChange -> {
                val updatedChecklist = state.value.editingChecklist.map {
                    if (it.id == event.itemId) it.copy(isChecked = event.isChecked) else it
                }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
            }
            is ProjectNotesEvent.OnChecklistItemTextChange -> {
                val updatedChecklist = state.value.editingChecklist.map {
                    if (it.id == event.itemId) it.copy(text = event.text) else it
                }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
            }
            is ProjectNotesEvent.OnTitleChange -> {
                val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (event.title to state.value.editingContent)
                _state.value = state.value.copy(
                    editingTitle = event.title,
                    editingHistory = newHistory,
                    editingHistoryIndex = newHistory.lastIndex
                )
            }
            is ProjectNotesEvent.OnContentChange -> {
                if (state.value.editingNoteType == "TEXT") {
                    val newContent = event.content
                    val oldContent = state.value.editingContent

                    val finalContent = if (newContent.text != oldContent.text) {
                        val oldText = oldContent.text
                        val newText = newContent.text

                        // 1. Find common prefix
                        val prefixLength = commonPrefixWith(oldText, newText).length

                        // 2. Find common suffix of the remainder of the strings
                        val oldRemainder = oldText.substring(prefixLength)
                        val newRemainder = newText.substring(prefixLength)
                        val suffixLength = commonSuffixWith(oldRemainder, newRemainder).length

                        // 3. Determine the middle (changed) part of the new text
                        val newChangedPart = newRemainder.substring(0, newRemainder.length - suffixLength)

                        val newAnnotatedString = buildAnnotatedString {
                            // Append the styled prefix from the original string
                            append(oldContent.annotatedString.subSequence(0, prefixLength))

                            // Append the newly typed text with the active styles
                            val styleToApply = state.value.activeStyles.reduceOrNull { a, b -> a.merge(b) } ?: SpanStyle()
                            withStyle(styleToApply) {
                                append(newChangedPart)
                            }

                            // Append the styled suffix from the original string
                            append(oldContent.annotatedString.subSequence(oldText.length - suffixLength, oldText.length))
                        }
                        newContent.copy(annotatedString = newAnnotatedString)
                    } else {
                        // When only the selection changes, trust the old AnnotatedString from our state
                        // and just update the selection from the new value. This prevents the TextField
                        // from stripping styles on selection/deselection.
                        oldContent.copy(selection = newContent.selection)
                    }

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
            is ProjectNotesEvent.ApplyStyleToContent -> {
                val selection = state.value.editingContent.selection
                if (selection.collapsed) {
                    val styleToAddOrRemove = event.style
                    val activeStyles = state.value.activeStyles.toMutableSet()

                    val isBold = styleToAddOrRemove.fontWeight == FontWeight.Bold
                    val isItalic = styleToAddOrRemove.fontStyle == FontStyle.Italic
                    val isUnderline = styleToAddOrRemove.textDecoration == TextDecoration.Underline

                    val wasBold = activeStyles.any { it.fontWeight == FontWeight.Bold }
                    val wasItalic = activeStyles.any { it.fontStyle == FontStyle.Italic }
                    val wasUnderline = activeStyles.any { it.textDecoration == TextDecoration.Underline }

                    if (isBold) {
                        if (wasBold) activeStyles.removeAll { it.fontWeight == FontWeight.Bold }
                        else activeStyles.add(SpanStyle(fontWeight = FontWeight.Bold))
                    }
                    if (isItalic) {
                        if (wasItalic) activeStyles.removeAll { it.fontStyle == FontStyle.Italic }
                        else activeStyles.add(SpanStyle(fontStyle = FontStyle.Italic))
                    }
                    if (isUnderline) {
                        if (wasUnderline) activeStyles.removeAll { it.textDecoration == TextDecoration.Underline }
                        else activeStyles.add(SpanStyle(textDecoration = TextDecoration.Underline))
                    }

                    _state.value = state.value.copy(
                        activeStyles = activeStyles,
                        isBoldActive = activeStyles.any { it.fontWeight == FontWeight.Bold },
                        isItalicActive = activeStyles.any { it.fontStyle == FontStyle.Italic },
                        isUnderlineActive = activeStyles.any { it.textDecoration == TextDecoration.Underline }
                    )
                } else {
                    val selection = state.value.editingContent.selection
                    val newAnnotatedString = AnnotatedString.Builder(state.value.editingContent.annotatedString).apply {
                        val style = event.style
                        val isApplyingBold = style.fontWeight == FontWeight.Bold
                        val isApplyingItalic = style.fontStyle == FontStyle.Italic
                        val isApplyingUnderline = style.textDecoration == TextDecoration.Underline

                        val selectionIsAlreadyBold = state.value.isBoldActive
                        val selectionIsAlreadyItalic = state.value.isItalicActive
                        val selectionIsAlreadyUnderline = state.value.isUnderlineActive

                        val styleToApply = when {
                            isApplyingBold -> if (selectionIsAlreadyBold) SpanStyle(fontWeight = FontWeight.Normal) else SpanStyle(fontWeight = FontWeight.Bold)
                            isApplyingItalic -> if (selectionIsAlreadyItalic) SpanStyle(fontStyle = FontStyle.Normal) else SpanStyle(fontStyle = FontStyle.Italic)
                            isApplyingUnderline -> if (selectionIsAlreadyUnderline) SpanStyle(textDecoration = TextDecoration.None) else SpanStyle(textDecoration = TextDecoration.Underline)
                            else -> style
                        }
                        addStyle(styleToApply, selection.start, selection.end)
                    }.toAnnotatedString()

                    val newTextFieldValue = state.value.editingContent.copy(annotatedString = newAnnotatedString)
                    val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to newTextFieldValue)

                    _state.value = state.value.copy(
                        editingContent = newTextFieldValue,
                        editingHistory = newHistory,
                        editingHistoryIndex = newHistory.lastIndex
                    )
                }
            }
            is ProjectNotesEvent.OnColorChange -> {
                _state.value = state.value.copy(editingColor = event.color)
            }
            is ProjectNotesEvent.OnLabelChange -> {
                viewModelScope.launch {
                    labelDao.insertLabel(Label(event.label))
                    _state.value = state.value.copy(editingLabel = event.label)
                }
            }
            is ProjectNotesEvent.OnTogglePinClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let { noteId ->
                        noteDao.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isPinned = !note.note.isPinned)
                            noteDao.insertNote(updatedNote)
                            val updatedNotesList = state.value.notes.map { if (it.note.id == updatedNote.id) it.copy(note = updatedNote) else it }
                            _state.value = state.value.copy(
                                isPinned = updatedNote.isPinned,
                                notes = updatedNotesList
                            )
                            val message = if (updatedNote.isPinned) "Note pinned" else "Note unpinned"
                            _events.emit(ProjectNotesUiEvent.ShowToast(message))
                        }
                    }
                }
            }
            is ProjectNotesEvent.OnToggleArchiveClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let { noteId ->
                        noteDao.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isArchived = !note.note.isArchived)
                            noteDao.insertNote(updatedNote)
                            val updatedNotesList = state.value.notes.map { if (it.note.id == updatedNote.id) it.copy(note = updatedNote) else it }
                            _state.value = state.value.copy(
                                isArchived = updatedNote.isArchived,
                                notes = updatedNotesList
                            )
                        }
                    }
                }
            }
            is ProjectNotesEvent.OnUndoClick -> {
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
            is ProjectNotesEvent.OnRedoClick -> {
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
            is ProjectNotesEvent.OnSaveNoteClick -> {
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
                            noteDao.getNoteById(noteId)?.let { noteDao.updateNote(it.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())) }
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
                                projectId = projectId
                            )
                        } else { // Existing note
                            noteDao.getNoteById(noteId)?.let { existingNote ->
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
                                    projectId = projectId
                                )
                            }
                        }
                        if (note != null) {
                            val currentNoteId = if (noteId == -1) { // New note
                                noteDao.insertNote(note)
                            } else { // Existing note
                                noteDao.updateNote(note)
                                noteId.toLong() // Convert Int to Long for consistency
                            }

                            // Handle attachments
                            val existingAttachmentsInDb = if (noteId != -1) {
                                noteDao.getNoteById(noteId)?.attachments ?: emptyList()
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
                                noteDao.deleteAttachment(attachment)
                            }

                            attachmentsToAdd.forEach { attachment ->
                                noteDao.insertAttachment(attachment.copy(noteId = currentNoteId.toInt()))
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
            is ProjectNotesEvent.OnDeleteNoteClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let {
                        if (it != -1) {
                            noteDao.getNoteById(it)?.let { note ->
                                noteDao.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                                _events.emit(ProjectNotesUiEvent.ShowToast("Note moved to Bin"))
                            }
                        }
                    }
                    _state.value = state.value.copy(expandedNoteId = null)
                }
            }
            is ProjectNotesEvent.OnCopyCurrentNoteClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let {
                        noteDao.getNoteById(it)?.let { note ->
                            val copiedNote = note.note.copy(id = 0, title = "${note.note.title} (Copy)", createdAt = System.currentTimeMillis(), lastEdited = System.currentTimeMillis())
                            noteDao.insertNote(copiedNote)
                            _events.emit(ProjectNotesUiEvent.ShowToast("Note copied"))
                        }
                    }
                }
            }
            is ProjectNotesEvent.OnAddLabelsToCurrentNoteClick -> {
                _state.value = state.value.copy(showLabelDialog = true)
            }
            is ProjectNotesEvent.DismissLabelDialog -> {
                _state.value = state.value.copy(showLabelDialog = false)
            }
            is ProjectNotesEvent.ToggleLayout -> {
                val newLayout = if (state.value.layoutType == com.suvojeet.notenext.ui.notes.LayoutType.GRID) com.suvojeet.notenext.ui.notes.LayoutType.LIST else com.suvojeet.notenext.ui.notes.LayoutType.GRID
                _state.value = state.value.copy(layoutType = newLayout)
            }
            is ProjectNotesEvent.SortNotes -> {
                _state.value = state.value.copy(sortType = event.sortType)
            }
            is ProjectNotesEvent.OnRemoveLinkPreview -> {
                val updatedLinkPreviews = state.value.linkPreviews.filter { it.url != event.url }
                _state.value = state.value.copy(linkPreviews = updatedLinkPreviews)
                viewModelScope.launch {
                    _events.emit(ProjectNotesUiEvent.LinkPreviewRemoved)
                }
            }
            is ProjectNotesEvent.OnInsertLink -> {
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
            is ProjectNotesEvent.ClearNewlyAddedChecklistItemId -> {
                _state.value = state.value.copy(newlyAddedChecklistItemId = null)
            }
            is ProjectNotesEvent.AddAttachment -> {
                val type = when {
                    event.mimeType.startsWith("image") -> "IMAGE"
                    event.mimeType.startsWith("video") -> "VIDEO"
                    event.mimeType.startsWith("audio") -> "AUDIO"
                    else -> "FILE"
                }
                val attachment = com.suvojeet.notenext.data.Attachment(
                    noteId = state.value.expandedNoteId ?: -1,
                    uri = event.uri,
                    type = type,
                    mimeType = event.mimeType,
                    tempId = java.util.UUID.randomUUID().toString()
                )
                _state.value = state.value.copy(editingAttachments = state.value.editingAttachments + attachment)
            }
            is ProjectNotesEvent.OnLinkDetected -> { /* TODO: Handle link detection */ }
            is ProjectNotesEvent.OnLinkPreviewFetched -> { /* TODO: Handle link preview fetched */ }
            is ProjectNotesEvent.RemoveAttachment -> {
                viewModelScope.launch {
                    val attachmentToRemove = _state.value.editingAttachments.firstOrNull { it.tempId == event.tempId }
                    attachmentToRemove?.let {
                        if (it.id != 0) { // Only delete from DB if it has a real ID
                            noteDao.deleteAttachmentById(it.id)
                        }
                        val updatedAttachments = _state.value.editingAttachments.filter { attachment -> attachment.tempId != event.tempId }
                        _state.value = _state.value.copy(editingAttachments = updatedAttachments)
                    }
                }
            }
        }
    }

    private fun commonPrefixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[i] != b[i]) {
                return a.substring(0, i)
            }
        }
        return a.substring(0, minLength)
    }

    private fun commonSuffixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[a.length - 1 - i] != b[b.length - 1 - i]) {
                return a.substring(a.length - i)
            }
        }
        return a.substring(a.length - minLength)
    }
}