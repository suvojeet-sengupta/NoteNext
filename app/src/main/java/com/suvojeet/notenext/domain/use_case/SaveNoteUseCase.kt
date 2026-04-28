package com.suvojeet.notenext.domain.use_case

import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.data.*
import javax.inject.Inject

/**
 * Encapsulates the logic for saving a note, including handling versions, 
 * checklists, attachments, and reminders.
 */
class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(
        note: Note,
        checklist: List<ChecklistItem>,
        attachments: List<Attachment>,
        isNewNote: Boolean,
        isDecoy: Boolean = false
    ): SaveNoteResult {
        // Logic to check if the note is empty
        val isNoteEmpty = note.title.isBlank() && (
            (note.noteType == NoteType.TEXT && note.content.isBlank()) ||
            (note.noteType == NoteType.CHECKLIST && checklist.all { it.text.isBlank() })
        )

        if (isNoteEmpty) {
            return if (isNewNote || note.id == -1) {
                SaveNoteResult.Ignored
            } else {
                // Move existing empty note to bin
                repository.getNoteById(note.id)?.let {
                    repository.updateNote(it.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                }
                SaveNoteResult.Deleted
            }
        }

        val currentTime = System.currentTimeMillis()
        val noteToSave = if (isNewNote || note.id == -1) {
            note.copy(createdAt = currentTime, lastEdited = currentTime, isDecoy = isDecoy)
        } else {
            // Before updating, save current state as a version
            repository.getNoteById(note.id)?.let { oldNoteWithAttachments ->
                val oldNote = oldNoteWithAttachments.note
                if (oldNote.title != note.title || oldNote.content != note.content) {
                    repository.insertNoteVersion(
                        NoteVersion(
                            noteId = note.id,
                            title = oldNote.title,
                            content = oldNote.content,
                            timestamp = oldNote.lastEdited,
                            noteType = oldNote.noteType
                        )
                    )
                    repository.limitNoteVersions(note.id, 10)
                }
            }
            note.copy(lastEdited = currentTime)
        }

        val savedNoteId = if (isNewNote || note.id == -1) {
            repository.insertNote(noteToSave).toInt()
        } else {
            repository.updateNote(noteToSave)
            note.id
        }

        // Sync Reminders
        if (noteToSave.reminderTime != null) {
            alarmScheduler.schedule(noteToSave.copy(id = savedNoteId))
        } else if (!isNewNote && note.id != -1) {
            alarmScheduler.cancel(noteToSave.copy(id = savedNoteId))
        }

        // Sync Checklist Items
        if (noteToSave.noteType == NoteType.CHECKLIST) {
            val updatedChecklist = checklist.mapIndexed { index, item ->
                item.copy(noteId = savedNoteId, position = index)
            }
            repository.deleteChecklistForNote(savedNoteId)
            repository.insertChecklistItems(updatedChecklist)
        }

        // Sync Attachments
        val existingAttachmentsInDb = if (!isNewNote && note.id != -1) {
            repository.getNoteById(savedNoteId)?.attachments ?: emptyList()
        } else {
            emptyList()
        }

        val attachmentsToAdd = attachments.filter { uiAttachment ->
            existingAttachmentsInDb.none { dbAttachment ->
                dbAttachment.uri == uiAttachment.uri && dbAttachment.type == uiAttachment.type
            }
        }

        val attachmentsToRemove = existingAttachmentsInDb.filter { dbAttachment ->
            attachments.none { uiAttachment ->
                uiAttachment.uri == dbAttachment.uri && uiAttachment.type == dbAttachment.type
            }
        }

        attachmentsToRemove.forEach { repository.deleteAttachment(it) }
        attachmentsToAdd.forEach { repository.insertAttachment(it.copy(noteId = savedNoteId)) }

        return SaveNoteResult.Success(savedNoteId)
    }
}
