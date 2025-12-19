package com.suvojeet.notenext.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY position ASC")
    fun getChecklistForNote(noteId: Int): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY position ASC")
    suspend fun getChecklistForNoteSync(noteId: Int): List<ChecklistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItem>)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)
    
    @Update
    suspend fun updateChecklistItems(items: List<ChecklistItem>)

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItem)
    
    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteChecklistForNote(noteId: Int)
}
