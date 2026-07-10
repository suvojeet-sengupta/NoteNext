package com.suvojeet.notenext.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupDataStore by preferencesDataStore(
    name = "backup_prefs_datastore",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "backup_prefs"))
    }
)

@Singleton
class BackupSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        private val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        private val INCLUDE_ATTACHMENTS = booleanPreferencesKey("include_backup_attachments")
        private val SD_CARD_ENABLED = booleanPreferencesKey("sd_card_backup_enabled")
        private val BACKUP_LOCATION_URI = stringPreferencesKey("sd_card_folder_uri")
        private val ENCRYPTION_ENABLED = booleanPreferencesKey("backup_encryption_enabled")
        private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        private val LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
        private val INCREMENTAL_ENABLED = booleanPreferencesKey("incremental_backup_enabled")
        private val SMART_BACKUP_ENABLED = booleanPreferencesKey("smart_backup_enabled")
        private val CHARGING_CONSTRAINT = booleanPreferencesKey("backup_on_charging_only")
        private val EDITS_THRESHOLD = intPreferencesKey("edits_before_backup")
        private val EDIT_COUNTER = intPreferencesKey("edit_counter")
        private val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
    }

    val autoBackupEnabled: Flow<Boolean> = context.backupDataStore.data.map { it[AUTO_BACKUP_ENABLED] ?: false }
    val backupFrequency: Flow<String> = context.backupDataStore.data.map { it[BACKUP_FREQUENCY] ?: "Daily" }
    val includeAttachments: Flow<Boolean> = context.backupDataStore.data.map { it[INCLUDE_ATTACHMENTS] ?: true }
    val sdCardEnabled: Flow<Boolean> = context.backupDataStore.data.map { it[SD_CARD_ENABLED] ?: false }
    val backupLocationUri: Flow<String?> = context.backupDataStore.data.map { it[BACKUP_LOCATION_URI] }
    val encryptionEnabled: Flow<Boolean> = context.backupDataStore.data.map { it[ENCRYPTION_ENABLED] ?: false }
    val lastBackupTime: Flow<Long> = context.backupDataStore.data.map { it[LAST_BACKUP_TIME] ?: 0L }
    val lastBackupStatus: Flow<String?> = context.backupDataStore.data.map { it[LAST_BACKUP_STATUS] }
    val incrementalEnabled: Flow<Boolean> = context.backupDataStore.data.map { it[INCREMENTAL_ENABLED] ?: false }
    val smartBackupEnabled: Flow<Boolean> = context.backupDataStore.data.map { it[SMART_BACKUP_ENABLED] ?: false }
    val chargingConstraint: Flow<Boolean> = context.backupDataStore.data.map { it[CHARGING_CONSTRAINT] ?: false }
    val editsThreshold: Flow<Int> = context.backupDataStore.data.map { it[EDITS_THRESHOLD] ?: 10 }
    val editCounter: Flow<Int> = context.backupDataStore.data.map { it[EDIT_COUNTER] ?: 0 }
    val googleAccountEmail: Flow<String?> = context.backupDataStore.data.map { it[GOOGLE_ACCOUNT_EMAIL] }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setBackupFrequency(frequency: String) {
        context.backupDataStore.edit { it[BACKUP_FREQUENCY] = frequency }
    }

    suspend fun setIncludeAttachments(include: Boolean) {
        context.backupDataStore.edit { it[INCLUDE_ATTACHMENTS] = include }
    }

    suspend fun setSdCardEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[SD_CARD_ENABLED] = enabled }
    }

    suspend fun setBackupLocationUri(uri: String?) {
        context.backupDataStore.edit {
            if (uri != null) it[BACKUP_LOCATION_URI] = uri else it.remove(BACKUP_LOCATION_URI)
        }
    }

    suspend fun setEncryptionEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[ENCRYPTION_ENABLED] = enabled }
    }

    suspend fun setLastBackupTime(time: Long) {
        context.backupDataStore.edit { it[LAST_BACKUP_TIME] = time }
    }

    suspend fun setLastBackupStatus(status: String?) {
        context.backupDataStore.edit {
            if (status != null) it[LAST_BACKUP_STATUS] = status else it.remove(LAST_BACKUP_STATUS)
        }
    }

    suspend fun setIncrementalEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[INCREMENTAL_ENABLED] = enabled }
    }

    suspend fun setSmartBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[SMART_BACKUP_ENABLED] = enabled }
    }

    suspend fun setChargingConstraint(enabled: Boolean) {
        context.backupDataStore.edit { it[CHARGING_CONSTRAINT] = enabled }
    }

    suspend fun setEditsThreshold(threshold: Int) {
        context.backupDataStore.edit { it[EDITS_THRESHOLD] = threshold }
    }

    suspend fun setEditCounter(count: Int) {
        context.backupDataStore.edit { it[EDIT_COUNTER] = count }
    }

    suspend fun setGoogleAccountEmail(email: String?) {
        context.backupDataStore.edit {
            if (email != null) it[GOOGLE_ACCOUNT_EMAIL] = email else it.remove(GOOGLE_ACCOUNT_EMAIL)
        }
    }
}
