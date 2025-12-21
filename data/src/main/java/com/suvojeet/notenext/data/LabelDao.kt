package com.suvojeet.notenext.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLabel(label: Label)

    @Update
    suspend fun updateLabel(label: Label)

    @Delete
    suspend fun deleteLabel(label: Label)

    @Query("SELECT * FROM labels ORDER BY name ASC")
    fun getLabels(): Flow<List<Label>>

    @Query("SELECT * FROM labels WHERE parentName = :parentName ORDER BY name ASC")
    fun getLabelsWithParent(parentName: String): Flow<List<Label>>

    @Query("SELECT * FROM labels WHERE parentName IS NULL ORDER BY name ASC")
    fun getRootLabels(): Flow<List<Label>>
}