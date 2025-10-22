package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.ui.notes.HtmlConverter
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.ui.notes.SortType
import com.suvojeet.notenext.ui.notes.LayoutType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class NotesViewModel(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val linkPreviewRepository: LinkPreviewRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotesState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events = _events.asSharedFlow()

    private var recentlyDeletedNote: Note? = null

    init {
        combine(noteDao.getNotes(), labelDao.getLabels(), _state) { notes, labels, state ->
            val sortedNotes = when (state.sortType) {
                SortType.DATE_CREATED -> notes.sortedByDescending { it.createdAt }
                SortType.DATE_MODIFIED -> notes.sortedByDescending { it.lastEdited }
                SortType.TITLE -> notes.sortedBy { it.title }
            }
            _state.value = state.copy(
                notes = sortedNotes,
                labels = labels.map { it.name }
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: NotesEvent) {
    when (event) {
        is NotesEvent.DeleteNote -> {
            viewModelScope.launch {
                val noteToBin = event.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())
                noteDao.updateNote(noteToBin)
                recentlyDeletedNote = event.note
                _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
            }
        }
        is NotesEvent.RestoreNote -> {
            viewModelScope.launch {
                recentlyDeletedNote?.let { restoredNote ->
                    noteDao.updateNote(restoredNote.copy(isBinned = false))
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
                    noteDao.updateNote(note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                }
                _state.value = state.value.copy(selectedNoteIds = emptyList())
                _events.emit(NotesUiEvent.ShowToast("${selectedNotes.size} notes moved to Bin"))
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
                if (event.label.isNotBlank()) {
                    labelDao.insertLabel(Label(event.label))
                }
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
                        val content = HtmlConverter.htmlToAnnotatedString(note.content)
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
                            linkPreviews = note.linkPreviews
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
                        linkPreviews = emptyList()
                    )
                }
            }
        }
        is NotesEvent.CollapseNote -> {
            onEvent(NotesEvent.OnSaveNoteClick)
        }
        is NotesEvent.OnCopyText -> {
            viewModelScope.launch {
                _events.emit(NotesUiEvent.ShowToast("Copied to clipboard"))
            }
        }
        is NotesEvent.OnPasteText -> {
            val currentContent = state.value.editingContent
            val selection = currentContent.selection
            val newText = currentContent.text.replaceRange(selection.min, selection.max, event.text)
            val newSelection = selection.min + event.text.length
            val newTextFieldValue = TextFieldValue(
                annotatedString = AnnotatedString(newText),
                selection = currentContent.selection.copy(start = newSelection, end = newSelection)
            )
            val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to newTextFieldValue)
            _state.value = state.value.copy(
                editingContent = newTextFieldValue,
                editingHistory = newHistory,
                editingHistoryIndex = newHistory.lastIndex
            )
        }
        is NotesEvent.OnCutText -> {
            val currentContent = state.value.editingContent
            val selection = currentContent.selection
            val newText = currentContent.text.replaceRange(selection.min, selection.max, "")
            val newSelection = selection.min
            val newTextFieldValue = TextFieldValue(
                annotatedString = AnnotatedString(newText),
                selection = currentContent.selection.copy(start = newSelection, end = newSelection)
            )
            val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to newTextFieldValue)
            _state.value = state.value.copy(
                editingContent = newTextFieldValue,
                editingHistory = newHistory,
                editingHistoryIndex = newHistory.lastIndex
            )
            viewModelScope.launch {
                _events.emit(NotesUiEvent.ShowToast("Cut to clipboard"))
            }
        }
        is NotesEvent.OnContentChange -> {
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
        is NotesEvent.ApplyStyleToContent -> {
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
        is NotesEvent.OnColorChange -> {
            _state.value = state.value.copy(editingColor = event.color)
        }
        is NotesEvent.OnLabelChange -> {
            viewModelScope.launch {
                labelDao.insertLabel(Label(event.label))
                _state.value = state.value.copy(editingLabel = event.label)
            }
        }
        is NotesEvent.OnTogglePinClick -> {
            viewModelScope.launch {
                state.value.expandedNoteId?.let { noteId ->
                    noteDao.getNoteById(noteId)?.let { note ->
                        val updatedNote = note.copy(isPinned = !note.isPinned)
                        noteDao.insertNote(updatedNote)
                        val updatedNotesList = state.value.notes.map { if (it.id == updatedNote.id) updatedNote else it }
                        _state.value = state.value.copy(
                            isPinned = updatedNote.isPinned,
                            notes = updatedNotesList
                        )
                    }
                }
            }
        }
        is NotesEvent.OnToggleArchiveClick -> {
            viewModelScope.launch {
                state.value.expandedNoteId?.let { noteId ->
                    noteDao.getNoteById(noteId)?.let { note ->
                        val updatedNote = note.copy(isArchived = !note.isArchived)
                        noteDao.insertNote(updatedNote)
                        val updatedNotesList = state.value.notes.map { if (it.id == updatedNote.id) updatedNote else it }
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
                val content = HtmlConverter.annotatedStringToHtml(state.value.editingContent.annotatedString)

                if (title.isBlank() && content.isBlank()) {
                    if (noteId != -1) { // It's an existing note, so delete it
                        noteDao.getNoteById(noteId)?.let { noteDao.updateNote(it.copy(isBinned = true, binnedOn = System.currentTimeMillis())) }
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
                            linkPreviews = state.value.linkPreviews
                        )
                    } else { // Existing note
                        noteDao.getNoteById(noteId)?.let { existingNote ->
                            existingNote.copy(
                                title = title,
                                content = content,
                                lastEdited = currentTime,
                                color = state.value.editingColor,
                                isPinned = state.value.isPinned,
                                isArchived = state.value.isArchived,
                                label = state.value.editingLabel,
                                linkPreviews = state.value.linkPreviews
                            )
                        }
                    }
                    if (note != null) {
                        noteDao.insertNote(note)
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
                    linkPreviews = emptyList()
                )
            }
        }
        is NotesEvent.OnDeleteNoteClick -> {
            viewModelScope.launch {
                state.value.expandedNoteId?.let {
                    if (it != -1) {
                        noteDao.getNoteById(it)?.let { note ->
                            noteDao.updateNote(note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
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
                    noteDao.getNoteById(it)?.let { note ->
                        val copiedNote = note.copy(id = 0, title = "${note.title} (Copy)", createdAt = System.currentTimeMillis(), lastEdited = System.currentTimeMillis())
                        noteDao.insertNote(copiedNote)
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
            _state.value = state.value.copy(sortType = event.sortType)
        }
        is NotesEvent.OnRemoveLinkPreview -> {
            val updatedLinkPreviews = state.value.linkPreviews.filter { it.url != event.url }
            _state.value = state.value.copy(linkPreviews = updatedLinkPreviews)
        }
        else -> {}
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
