package com.suvojeet.notenext.ui.notes.delegate

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.notes.RichTextController
import com.suvojeet.notenext.ui.notes.SaveStatus
import com.suvojeet.notenext.ui.util.UndoRedoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toImmutableSet
import javax.inject.Inject

class NoteEditorDelegate @Inject constructor(
    private val richTextController: RichTextController,
    private val savedStateHandle: SavedStateHandle
) {
    companion object {
        private const val KEY_EDITING_TITLE = "editing_title"
        private const val KEY_EDITING_CONTENT = "editing_content"
        private const val KEY_EXPANDED_NOTE_ID = "expanded_note_id"
        private const val KEY_NOTE_TYPE = "note_type"
    }

    private val _editState = MutableStateFlow(
        NotesEditState(
            editingTitle = savedStateHandle.get<String>(KEY_EDITING_TITLE) ?: "",
            editingContent = TextFieldValue(richTextController.parseMarkdownToAnnotatedString(savedStateHandle.get<String>(KEY_EDITING_CONTENT) ?: "")),
            expandedNoteId = savedStateHandle.get<Int>(KEY_EXPANDED_NOTE_ID),
            editingNoteType = savedStateHandle.get<String>(KEY_NOTE_TYPE)?.let { NoteType.valueOf(it) } ?: NoteType.TEXT
        )
    )
    val editState = _editState.asStateFlow()

    private val undoRedoManager = UndoRedoManager<Pair<String, TextFieldValue>>("" to TextFieldValue())
    private var autoSaveJob: Job? = null

    fun onTitleChange(newTitle: String) {
        val safeTitle = if (newTitle.length > 100) newTitle.take(100) else newTitle
        _editState.update { it.copy(editingTitle = safeTitle, saveStatus = SaveStatus.UNSAVED) }
        savedStateHandle[KEY_EDITING_TITLE] = safeTitle
    }

    fun onContentChange(newContent: TextFieldValue, scope: CoroutineScope, onSave: suspend () -> Unit) {
        val oldContent = _editState.value.editingContent
        val processedContent = richTextController.processContentChange(
            oldContent = oldContent,
            newContent = newContent,
            activeStyles = _editState.value.activeStyles,
            activeHeadingStyle = _editState.value.activeHeadingStyle
        )

        _editState.update { 
            it.copy(
                editingContent = processedContent,
                saveStatus = SaveStatus.UNSAVED,
                canUndo = true 
            ) 
        }
        
        savedStateHandle[KEY_EDITING_CONTENT] = processedContent.text
        
        if (processedContent.text != oldContent.text) {
             undoRedoManager.addState(_editState.value.editingTitle to processedContent)
             updateUndoRedoFlags()
        }
        
        scheduleAutoSave(scope, onSave)
    }

    fun applyStyle(style: SpanStyle) {
        val result = richTextController.toggleStyle(
            content = _editState.value.editingContent,
            styleToToggle = style,
            currentActiveStyles = _editState.value.activeStyles,
            isBoldActive = _editState.value.isBoldActive,
            isItalicActive = _editState.value.isItalicActive,
            isUnderlineActive = _editState.value.isUnderlineActive
        )
        
        _editState.update { state ->
            state.copy(
                editingContent = result.updatedContent ?: state.editingContent,
                activeStyles = result.updatedActiveStyles?.toImmutableSet() ?: state.activeStyles,
                isBoldActive = result.updatedActiveStyles?.any { s -> s.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold } ?: state.isBoldActive,
                isItalicActive = result.updatedActiveStyles?.any { s -> s.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic } ?: state.isItalicActive,
                isUnderlineActive = result.updatedActiveStyles?.any { s -> s.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline } ?: state.isUnderlineActive
            )
        }
    }

    fun undo() {
        undoRedoManager.undo()?.let { (title, content) ->
            _editState.update { it.copy(editingTitle = title, editingContent = content) }
            updateUndoRedoFlags()
        }
    }

    fun redo() {
        undoRedoManager.redo()?.let { (title, content) ->
            _editState.update { it.copy(editingTitle = title, editingContent = content) }
            updateUndoRedoFlags()
        }
    }

    private fun updateUndoRedoFlags() {
        _editState.update { 
            it.copy(
                canUndo = undoRedoManager.canUndo.value,
                canRedo = undoRedoManager.canRedo.value
            ) 
        }
    }

    fun scheduleAutoSave(scope: CoroutineScope, onSave: suspend () -> Unit) {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(2000L) 
            _editState.update { it.copy(saveStatus = SaveStatus.SAVING) }
            try {
                onSave()
                _editState.update { it.copy(saveStatus = SaveStatus.SAVED) }
            } catch (e: Exception) {
                _editState.update { it.copy(saveStatus = SaveStatus.ERROR) }
            }
        }
    }
    
    fun setEditState(newState: NotesEditState) {
        _editState.value = newState
        undoRedoManager.reset(newState.editingTitle to newState.editingContent)
        updateUndoRedoFlags()
    }

    fun reset(title: String, content: TextFieldValue) {
        undoRedoManager.reset(title to content)
        updateUndoRedoFlags()
    }
    
    fun cancelAutoSave() {
        autoSaveJob?.cancel()
    }

    fun updateState(transform: (NotesEditState) -> NotesEditState) {
        _editState.update(transform)
    }
}
