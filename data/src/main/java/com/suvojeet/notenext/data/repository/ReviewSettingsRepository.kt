package com.suvojeet.notenext.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reviewDataStore by preferencesDataStore(
    name = "app_review_prefs_datastore",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "app_review_prefs"))
    }
)

@Singleton
class ReviewSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val FIRST_OPEN_TIME = longPreferencesKey("first_open_time")
        private val APP_OPENS_COUNT = intPreferencesKey("app_opens_count")
        private val REVIEW_REQUESTED = booleanPreferencesKey("review_requested")
    }

    suspend fun getReviewSettings(): ReviewSettings {
        val prefs = context.reviewDataStore.data.first()
        return ReviewSettings(
            firstOpenTime = prefs[FIRST_OPEN_TIME] ?: 0L,
            appOpensCount = prefs[APP_OPENS_COUNT] ?: 0,
            isReviewRequested = prefs[REVIEW_REQUESTED] ?: false
        )
    }

    suspend fun setFirstOpenTime(time: Long) {
        context.reviewDataStore.edit { it[FIRST_OPEN_TIME] = time }
    }

    suspend fun incrementAppOpensCount() {
        context.reviewDataStore.edit { 
            val current = it[APP_OPENS_COUNT] ?: 0
            it[APP_OPENS_COUNT] = current + 1
        }
    }

    suspend fun setReviewRequested(requested: Boolean) {
        context.reviewDataStore.edit { it[REVIEW_REQUESTED] = requested }
    }
}

data class ReviewSettings(
    val firstOpenTime: Long,
    val appOpensCount: Int,
    val isReviewRequested: Boolean
)
