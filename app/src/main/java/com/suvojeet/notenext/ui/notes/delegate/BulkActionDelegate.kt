package com.suvojeet.notenext.ui.notes.delegate

import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.ReminderScheduler
import com.suvojeet.notenext.util.CryptoUtils
import com.suvojeet.notenext.util.HtmlConverter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The bulk actions that operate on a multi-selection of notes.
 *
 * These used to be written out twice — once in NotesViewModel and once in
 * ProjectNotesViewModel — and the two copies had drifted apart, so fixes only
 * ever landed on one side (the project screen never decrypted a note on unlock,
 * never tagged copies with the current decoy flag, and never refreshed widgets).
 * Both view models now route through this one implementation; each keeps its own
 * event type and just emits the message it gets back.
 *
 * Every action takes note ids rather than already-loaded notes: the two screens
 * hold different projections (full notes vs. summaries), and the row has to be
 * re-read before updating it anyway.
 */
@Singleton
class BulkActionDelegate @Inject constructor(
    private val repository: NoteRepository,
    private val reminderScheduler: ReminderScheduler
) {

    /** Outcome of [toggleLock], which can fail on biometric/keystore errors. */
    sealed interface LockResult {
        val message: String

        data class Success(override val message: String) : LockResult
        data class Failure(override val message: String) : LockResult
    }

    /** Title + body for a share/send intent, built from a selection. */
    data class SharePayload(val title: String, val content: String)

    private suspend fun loadNotes(ids: List<Int>): List<NoteWithAttachments> =
        ids.mapNotNull { repository.getNoteById(it) }

    private fun plural(count: Int, one: String, many: String): String =
        if (count > 1) "$count $many" else one

    /**
     * Pins the selection, or unpins it when everything is already pinned.
     *
     * A mixed selection pins — matching what a user means by tapping "pin" on a
     * set where some are already pinned.
     */
    suspend fun togglePin(ids: List<Int>): String? {
        val notes = loadNotes(ids)
        if (notes.isEmpty()) return null

        val pinning = notes.any { !it.note.isPinned }
        notes.forEach { repository.updateNote(it.note.copy(isPinned = pinning)) }

        return if (pinning) plural(notes.size, "Note pinned", "notes pinned")
        else plural(notes.size, "Note unpinned", "notes unpinned")
    }

    /**
     * Locks the selection, or unlocks it when everything is already locked.
     *
     * Unlocking also decrypts: a locked note may be stored as ciphertext, and
     * clearing `isLocked` without decrypting would leave the body unreadable.
     */
    suspend fun toggleLock(ids: List<Int>): LockResult? {
        val notes = loadNotes(ids)
        if (notes.isEmpty()) return null

        val locking = notes.firstOrNull()?.note?.isLocked == false
        return try {
            notes.forEach { noteWithAttachments ->
                val note = noteWithAttachments.note
                val updated = if (!locking && note.isEncrypted) {
                    CryptoUtils.decryptNote(note).copy(isLocked = false)
                } else {
                    note.copy(isLocked = locking)
                }
                repository.updateNote(updated)
            }
            LockResult.Success(
                if (locking) plural(notes.size, "Note locked", "notes locked")
                else plural(notes.size, "Note unlocked", "notes unlocked")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            LockResult.Failure(
                if (locking) "Failed to lock notes"
                else "Failed to unlock notes: Authentication may be required"
            )
        }
    }

    suspend fun moveToBin(ids: List<Int>): String {
        val notes = loadNotes(ids)
        val binnedOn = System.currentTimeMillis()
        notes.forEach {
            repository.updateNote(it.note.copy(isBinned = true, binnedOn = binnedOn))
        }
        return "${notes.size} notes moved to Bin"
    }

    suspend fun toggleArchive(ids: List<Int>) {
        loadNotes(ids).forEach {
            repository.updateNote(it.note.copy(isArchived = !it.note.isArchived))
        }
    }

    suspend fun toggleImportant(ids: List<Int>) {
        loadNotes(ids).forEach {
            repository.updateNote(it.note.copy(isImportant = !it.note.isImportant))
        }
    }

    suspend fun changeColor(ids: List<Int>, color: Int): String {
        val notes = loadNotes(ids)
        notes.forEach { repository.updateNote(it.note.copy(color = color)) }
        return "Color updated for ${notes.size} notes"
    }

    /**
     * Duplicates the selection along with its attachments and tick boxes.
     *
     * [isDecoy] carries the current session's decoy flag onto the copies —
     * without it, duplicating inside a decoy session would surface the copy in
     * the real note list.
     */
    suspend fun copyNotes(ids: List<Int>, isDecoy: Boolean): String {
        val notes = loadNotes(ids)
        notes.forEach { source ->
            val copy = source.note.copy(
                id = 0,
                title = "${source.note.title} (Copy)",
                isDecoy = isDecoy
            )
            val newId = repository.insertNote(copy)
            require(newId <= Int.MAX_VALUE) { "Note ID overflow" }
            val newNoteId = newId.toInt()

            source.attachments.forEach { attachment ->
                repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId))
            }
            if (source.checklistItems.isNotEmpty()) {
                repository.insertChecklistItems(
                    source.checklistItems.map {
                        it.copy(id = UUID.randomUUID().toString(), noteId = newNoteId)
                    }
                )
            }
        }
        return plural(notes.size, "Note copied", "notes copied")
    }

    suspend fun setReminder(ids: List<Int>, reminderMillis: Long, repeatOption: String): String {
        val notes = loadNotes(ids)
        notes.forEach {
            val updated = it.note.copy(
                reminderTime = reminderMillis,
                repeatOption = repeatOption
            )
            repository.updateNote(updated)
            reminderScheduler.scheduleNoteReminder(updated)
        }
        return "Reminder set for ${notes.size} notes"
    }

    suspend fun setLabel(ids: List<Int>, label: String) {
        if (label.isNotBlank()) {
            repository.insertLabel(Label(label))
        }
        loadNotes(ids).forEach {
            repository.updateNote(it.note.copy(label = label))
        }
    }

    /**
     * Flattens the selection into shareable plain text.
     *
     * Tick boxes are appended for every note type, not just checklists — a text
     * note can carry them below its body, and those used to be dropped.
     */
    suspend fun buildSharePayload(ids: List<Int>): SharePayload? {
        val notes = loadNotes(ids)
        if (notes.isEmpty()) return null

        val title = if (notes.size == 1) notes.first().note.title else "Multiple Notes"
        val body = StringBuilder()

        notes.forEachIndexed { index, noteWithAttachments ->
            val note = noteWithAttachments.note
            body.append("Title: ${note.title}\n\n")

            if (note.noteType != NoteType.CHECKLIST) {
                body.append(HtmlConverter.htmlToPlainText(note.content))
            }
            if (noteWithAttachments.checklistItems.isNotEmpty()) {
                if (note.noteType != NoteType.CHECKLIST) body.append("\n")
                noteWithAttachments.checklistItems
                    .sortedBy { it.position }
                    .forEach { item ->
                        body.append(if (item.isChecked) "[x] " else "[ ] ").append(item.text).append("\n")
                    }
            }
            if (index < notes.size - 1) {
                body.append("\n\n---\n\n")
            }
        }
        return SharePayload(title, body.toString())
    }
}
