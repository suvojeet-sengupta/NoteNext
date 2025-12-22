package com.suvojeet.notenext.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suvojeet.notenext.ui.theme.ShapeFamily
import com.suvojeet.notenext.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
    val ENABLE_RICH_LINK_PREVIEW = booleanPreferencesKey("enable_rich_link_preview")
    val SHAPE_FAMILY = stringPreferencesKey("shape_family")
    val ENABLE_APP_LOCK = booleanPreferencesKey("enable_app_lock")
    val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
    val LANGUAGE = stringPreferencesKey("language")
    val LAST_SEEN_VERSION = intPreferencesKey("last_seen_version")
}

class SettingsRepository(private val context: Context) {

    val lastSeenVersion: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_VERSION] ?: 0
        }

    suspend fun saveLastSeenVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_VERSION] = version
        }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map {
            preferences ->
            ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val autoDeleteDays: Flow<Int> = context.dataStore.data
        .map {
            preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 7
        }

    suspend fun saveAutoDeleteDays(days: Int) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = days
        }
    }

    val enableRichLinkPreview: Flow<Boolean> = context.dataStore.data
        .map {
            preferences ->
            preferences[PreferencesKeys.ENABLE_RICH_LINK_PREVIEW] ?: true
        }

    suspend fun saveEnableRichLinkPreview(enable: Boolean) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.ENABLE_RICH_LINK_PREVIEW] = enable
        }
    }

    val shapeFamily: Flow<ShapeFamily> = context.dataStore.data
        .map { preferences ->
            ShapeFamily.valueOf(preferences[PreferencesKeys.SHAPE_FAMILY] ?: ShapeFamily.ROUNDED.name)
        }

    suspend fun saveShapeFamily(shapeFamily: ShapeFamily) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHAPE_FAMILY] = shapeFamily.name
        }
    }

    val enableAppLock: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ENABLE_APP_LOCK] ?: false
        }

    suspend fun saveEnableAppLock(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_APP_LOCK] = enable
        }
    }

    val appLockPin: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LOCK_PIN]
        }

    suspend fun saveAppLockPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_PIN] = pin
        }
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] ?: false
        }

    suspend fun setSetupComplete(isComplete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] = isComplete
        }
    }

    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "en"
        }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }
}