
package com.suvojeet.notenext.ui.notes.delegate

import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class BulkActionDelegate @Inject constructor(
    private val repository: NoteRepository
) {
    fun archiveSelected(
        selectedIds: List<Int>,
        scope: CoroutineScope,
        events: MutableSharedFlow<NotesUiEvent>,
        onComplete: () -> Unit
    ) {
        scope.launch {
            val notes = repository.getPinnedNoteSummaries("", null).first().filter { it.note.id in selectedIds } +
                        repository.getArchivedNoteSummaries().first().filter { it.note.id in selectedIds }
            // Note: This logic is a bit flawed because it only looks at pinned/archived. 
            // Better to have a getNotesByIds in repository.
            
            selectedIds.forEach { id ->
                repository.getNoteById(id)?.let { noteWithAttachments ->
                    repository.updateNote(noteWithAttachments.note.copy(isArchived = true, isPinned = false))
                }
            }
            events.emit(NotesUiEvent.ShowSnackbar("${selectedIds.size} notes archived"))
            onComplete()
        }
    }

    fun deleteSelected(
        selectedIds: List<Int>,
        scope: CoroutineScope,
        events: MutableSharedFlow<NotesUiEvent>,
        onComplete: () -> Unit
    ) {
        scope.launch {
            selectedIds.forEach { id ->
                repository.getNoteById(id)?.let { noteWithAttachments ->
                    repository.updateNote(noteWithAttachments.note.copy(isBinned = true, isPinned = false))
                }
            }
            events.emit(NotesUiEvent.ShowSnackbar("${selectedIds.size} notes moved to Bin"))
            onComplete()
        }
    }
    
    fun setLabelSelected(
        selectedIds: List<Int>,
        label: String,
        scope: CoroutineScope,
        events: MutableSharedFlow<NotesUiEvent>,
        onComplete: () -> Unit
    ) {
        scope.launch {
            selectedIds.forEach { id ->
                repository.getNoteById(id)?.let { noteWithAttachments ->
                    repository.updateNote(noteWithAttachments.note.copy(label = label))
                }
            }
            events.emit(NotesUiEvent.ShowSnackbar("Label updated for ${selectedIds.size} notes"))
            onComplete()
        }
    }
}
