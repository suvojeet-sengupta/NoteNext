package com.suvojeet.notenext.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.draftDataStore by preferencesDataStore(name = "draft_prefs")

@Singleton
class DraftRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val NOTE_DRAFT_KEY = stringPreferencesKey("note_draft")
    private val TODO_DRAFT_KEY = stringPreferencesKey("todo_draft")

    val noteDraft: Flow<String?> = context.draftDataStore.data.map { it[NOTE_DRAFT_KEY] }
    val todoDraft: Flow<String?> = context.draftDataStore.data.map { it[TODO_DRAFT_KEY] }

    suspend fun saveNoteDraft(content: String) {
        context.draftDataStore.edit { it[NOTE_DRAFT_KEY] = content }
    }

    suspend fun clearNoteDraft() {
        context.draftDataStore.edit { it.remove(NOTE_DRAFT_KEY) }
    }

    suspend fun saveTodoDraft(content: String) {
        context.draftDataStore.edit { it[TODO_DRAFT_KEY] = content }
    }

    suspend fun clearTodoDraft() {
        context.draftDataStore.edit { it.remove(TODO_DRAFT_KEY) }
    }
}
