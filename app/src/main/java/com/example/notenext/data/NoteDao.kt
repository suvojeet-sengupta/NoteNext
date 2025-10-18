package com.example.notenext.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 ORDER BY isPinned DESC, lastEdited DESC")
    fun getNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY lastEdited DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isBinned = 1 ORDER BY lastEdited DESC")
    fun getBinnedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

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
}
