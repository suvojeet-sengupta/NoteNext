package com.suvojeet.notenext.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 ORDER BY isPinned DESC, lastEdited DESC")
    fun getNotes(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY lastEdited DESC")
    fun getArchivedNotes(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isBinned = 1 ORDER BY lastEdited DESC")
    fun getBinnedNotes(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteWithAttachments?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET label = :newName WHERE label = :oldName")
    suspend fun updateLabelName(oldName: String, newName: String)

    @Query("UPDATE notes SET label = NULL WHERE label = :labelName")
    suspend fun removeLabelFromNotes(labelName: String)

    @Query("DELETE FROM notes WHERE isBinned = 1")
    suspend fun emptyBin()

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteAttachmentsForNote(noteId: Int)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: Int)

    @Query("SELECT * FROM notes WHERE reminderTime IS NOT NULL AND reminderTime > :currentTime ORDER BY reminderTime ASC")
    fun getNotesWithReminders(currentTime: Long): Flow<List<Note>>
}
