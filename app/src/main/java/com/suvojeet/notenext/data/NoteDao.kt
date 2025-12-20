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

    @Query("DELETE FROM notes WHERE isBinned = 1 AND binnedOn IS NOT NULL AND binnedOn < :threshold")
    suspend fun deleteBinnedNotesOlderThan(threshold: Long)

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteAttachmentsForNote(noteId: Int)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: Int)

    @Query("SELECT * FROM notes WHERE reminderTime IS NOT NULL AND reminderTime > :currentTime ORDER BY reminderTime ASC")
    fun getNotesWithReminders(currentTime: Long): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM notes WHERE projectId = :projectId AND isBinned = 0 ORDER BY isPinned DESC, lastEdited DESC")
    fun getNotesByProjectId(projectId: Int): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0
        ORDER BY notes.isPinned DESC, notes.lastEdited DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteWithAttachments>>

    // Optimized Queries for NotesViewModel

    // 1. DATE_MODIFIED
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 AND projectId IS NULL ORDER BY isPinned DESC, lastEdited DESC")
    fun getNotesOrderedByDateModified(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0 AND projectId IS NULL
        ORDER BY notes.isPinned DESC, notes.lastEdited DESC
    """)
    fun searchNotesOrderedByDateModified(query: String): Flow<List<NoteWithAttachments>>

    // 2. DATE_CREATED
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 AND projectId IS NULL ORDER BY isPinned DESC, createdAt DESC")
    fun getNotesOrderedByDateCreated(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0 AND projectId IS NULL
        ORDER BY notes.isPinned DESC, notes.createdAt DESC
    """)
    fun searchNotesOrderedByDateCreated(query: String): Flow<List<NoteWithAttachments>>

    // 3. TITLE
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 AND projectId IS NULL ORDER BY isPinned DESC, title ASC")
    fun getNotesOrderedByTitle(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0 AND projectId IS NULL
        ORDER BY notes.isPinned DESC, notes.title ASC
    """)
    fun searchNotesOrderedByTitle(query: String): Flow<List<NoteWithAttachments>>
}
