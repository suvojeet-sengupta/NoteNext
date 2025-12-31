package com.suvojeet.notenext.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.notenext.data.*
import com.suvojeet.notenext.data.backup.GoogleDriveManager
import com.suvojeet.notenext.data.backup.KeepNote
import com.suvojeet.notenext.data.backup.KeepLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class BackupDetails(
    val notesCount: Int,
    val labelsCount: Int,
    val projectsCount: Int,
    val attachmentsCount: Int,
    val totalSize: Long,
    val notesSize: Long,
    val labelsSize: Long,
    val projectsSize: Long,
    val attachmentsSize: Long
)

data class BackupRestoreState(
    val backupDetails: BackupDetails? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupResult: String? = null,
    val restoreResult: String? = null,
    val driveBackupExists: Boolean = false,
    val isCheckingBackup: Boolean = false,
    val isDeleting: Boolean = false,
    val isAutoBackupEnabled: Boolean = false,
    val backupFrequency: String = "Daily",
    val foundProjects: List<com.suvojeet.notenext.data.Project> = emptyList(),
    val isScanning: Boolean = false,
    val googleAccountEmail: String? = null,
    val uploadProgress: String? = null,
    val driveBackupMetadata: com.suvojeet.notenext.data.backup.GoogleDriveManager.DriveBackupMetadata? = null,
    val isSdCardAutoBackupEnabled: Boolean = false,
    val sdCardFolderUri: String? = null,
    val includeAttachments: Boolean = true,
    val backupVersions: List<com.suvojeet.notenext.data.backup.GoogleDriveManager.DriveBackupMetadata> = emptyList(),
    val isLoadingVersions: Boolean = false,
    val isPasswordRequired: Boolean = false,
    val pendingRestoreUri: String? = null,
    val isAutoBackupEncryptionEnabled: Boolean = false
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val backupRepository: com.suvojeet.notenext.data.backup.BackupRepository,
    private val application: Application,
    private val googleDriveManager: GoogleDriveManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupRestoreState())
    val state = _state.asStateFlow()

    init {
        val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
        val enabled = sharedPrefs.getBoolean("auto_backup_enabled", false)
        val frequency = sharedPrefs.getString("backup_frequency", "Daily") ?: "Daily"
        val includeAttachments = sharedPrefs.getBoolean("include_backup_attachments", true)
        
        val sdCardEnabled = sharedPrefs.getBoolean("sd_card_backup_enabled", false)
        val sdCardUri = sharedPrefs.getString("sd_card_folder_uri", null)
        
        _state.value = _state.value.copy(
            isAutoBackupEnabled = enabled, 
            backupFrequency = frequency,
            isSdCardAutoBackupEnabled = sdCardEnabled,
            sdCardFolderUri = sdCardUri,
            includeAttachments = includeAttachments,
            isAutoBackupEncryptionEnabled = sharedPrefs.getBoolean("auto_backup_encryption_enabled", false)
        )
    }

    fun setGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        _state.value = _state.value.copy(googleAccountEmail = account?.email)
        if (account != null) {
            checkDriveBackupStatus(account)
            refreshBackupVersions(account)
        } else {
            _state.value = _state.value.copy(driveBackupExists = false, backupVersions = emptyList())
        }
    }

    fun signOut(context: android.content.Context) {
         com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).signOut().addOnCompleteListener {
             _state.value = _state.value.copy(googleAccountEmail = null, driveBackupExists = false, backupVersions = emptyList())
        }
    }

    fun getBackupDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // ... (Existing implementation for calculating size) ...
                val notes = repository.getNotes().first()
                val labels = repository.getLabels().first()
                val projects = repository.getProjects().first()
                val attachments = notes.flatMap { it.attachments }

                val notesJson = Gson().toJson(notes)
                val labelsJson = Gson().toJson(labels)
                val projectsJson = Gson().toJson(projects)

                var attachmentsSize = 0L
                attachments.forEach { attachment ->
                    try {
                        val attachmentUri = Uri.parse(attachment.uri)
                        application.contentResolver.openFileDescriptor(attachmentUri, "r")?.use {
                            attachmentsSize += it.statSize
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val notesSize = notesJson.toByteArray().size.toLong()
                val labelsSize = labelsJson.toByteArray().size.toLong()
                val projectsSize = projectsJson.toByteArray().size.toLong()
                val totalSize = notesSize + labelsSize + projectsSize + attachmentsSize

                _state.value = _state.value.copy(
                    backupDetails = BackupDetails(
                        notesCount = notes.size,
                        labelsCount = labels.size,
                        projectsCount = projects.size,
                        attachmentsCount = attachments.size,
                        totalSize = totalSize,
                        notesSize = notesSize,
                        labelsSize = labelsSize,
                        projectsSize = projectsSize,
                        attachmentsSize = attachmentsSize
                    )
                )
            }
        }
    }

    fun createBackup(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBackingUp = true, backupResult = null)
            withContext(Dispatchers.IO) {
                try {
                    if (password.isNullOrBlank()) {
                        application.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            backupRepository.createBackupZip(outputStream, state.value.includeAttachments)
                        }
                        _state.value = _state.value.copy(isBackingUp = false, backupResult = "Local Backup successful")
                    } else {
                        // For CreateDocument (Save to File), we have a File URI, so we use stream based backup
                        backupRepository.backupToEncryptedStream(
                            application.contentResolver.openOutputStream(uri), 
                            password, 
                            state.value.includeAttachments
                        )
                        _state.value = _state.value.copy(isBackingUp = false, backupResult = "Encrypted Local Backup successful")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isBackingUp = false, backupResult = "Local Backup failed: ${e.message}")
                }
            }
        }
    }

    fun backupToDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isBackingUp = true, 
                backupResult = "Uploading to Drive...",
                uploadProgress = "Starting..."
            )
            withContext(Dispatchers.IO) {
                try {
                    backupRepository.backupToDrive(account, null, state.value.includeAttachments) { uploaded, total ->
                        val progress = if (total > 0) {
                            val percent = (uploaded * 100) / total
                            val uploadedMb = String.format("%.2f", uploaded / (1024.0 * 1024.0))
                            val totalMb = String.format("%.2f", total / (1024.0 * 1024.0))
                            "$uploadedMb MB / $totalMb MB ($percent%)"
                        } else {
                            "Uploading..."
                        }
                        _state.value = _state.value.copy(uploadProgress = progress)
                    }
                    
                    _state.value = _state.value.copy(
                        isBackingUp = false,
                        backupResult = "Drive Backup successful",
                        driveBackupExists = true,
                        uploadProgress = null
                    )
                    refreshBackupVersions(account)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(
                        isBackingUp = false, 
                        backupResult = "Drive Backup failed: ${e.message}",
                        uploadProgress = null
                    )
                }
            }
        }
    }

    // ... (readBackupFromZip remains the same) ...

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = null)
            withContext(Dispatchers.IO) {
                // Check for encryption first
                if (backupRepository.checkIsEncrypted(uri)) {
                    _state.value = _state.value.copy(
                        isRestoring = false, 
                        isPasswordRequired = true, 
                        pendingRestoreUri = uri.toString()
                    )
                    return@withContext
                }
                
                try {
                    application.contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zis ->
                            readBackupFromZip(zis)
                        }
                    }
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Local Restore successful")
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Local Restore failed: ${e.message}")
                }
            }
        }
    }

    fun restoreEncryptedBackup(password: String) {
        val uriString = state.value.pendingRestoreUri ?: return
        val uri = Uri.parse(uriString)
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = null, isPasswordRequired = false) // Clear temp state while working
            withContext(Dispatchers.IO) {
                try {
                    // Decrypt to temp file
                    val tempZipFile = backupRepository.decryptBackupToTempFile(uri, password)
                    
                    try {
                        // Restore from temp file
                        java.io.FileInputStream(tempZipFile).use { inputStream ->
                            ZipInputStream(inputStream).use { zis ->
                                readBackupFromZip(zis)
                            }
                        }
                        _state.value = _state.value.copy(isRestoring = false, restoreResult = "Encrypted Restore successful", pendingRestoreUri = null)
                    } finally {
                        if (tempZipFile.exists()) tempZipFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Re-show password dialog on failure if it was likely a password error?
                    // For now just show error.
                    _state.value = _state.value.copy(
                        isRestoring = false, 
                        restoreResult = "Restore failed: ${e.message}"
                        // Could reset state.isPasswordRequired = true here if we want to let them retry immediately
                        // But let's let them click again.
                    )
                }
            }
        }
    }

    fun cancelPasswordEntry() {
        _state.value = _state.value.copy(isPasswordRequired = false, pendingRestoreUri = null)
    }
    
    private suspend fun readBackupFromZip(zis: ZipInputStream) {
         // Clear existing data
        repository.getNotes().first().flatMap { it.attachments }.forEach { repository.deleteAttachment(it) }
        repository.getNotes().first().forEach { repository.deleteNote(it.note) }
        repository.getLabels().first().forEach { repository.deleteLabel(it) }
        repository.getProjects().first().forEach { repository.deleteProject(it.id) }

        val oldToNewProjectIds = mutableMapOf<Int, Int>()
        var notesJson: String? = null
        var labelsJson: String? = null
        var projectsJson: String? = null

        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            when {
                zipEntry.name == "notes.json" -> notesJson = InputStreamReader(zis).readText()
                zipEntry.name == "labels.json" -> labelsJson = InputStreamReader(zis).readText()
                zipEntry.name == "projects.json" -> projectsJson = InputStreamReader(zis).readText()
                zipEntry.name.startsWith("attachments/") -> {
                    // Attachment restoration is complex and will be handled later.
                }
            }
            zipEntry = zis.nextEntry
        }

        // Restore projects and create mapping
        projectsJson?.let {
            val projectsType = object : TypeToken<List<Project>>() {}.type
            val projects: List<Project> = Gson().fromJson(it, projectsType)
            projects.forEach { project ->
                val oldId = project.id
                val newId = repository.insertProject(project.copy(id = 0)).toInt()
                oldToNewProjectIds[oldId] = newId
            }
        }

        // Restore labels
        labelsJson?.let {
            val labelsType = object : TypeToken<List<Label>>() {}.type
            val labels: List<Label> = Gson().fromJson(it, labelsType)
            labels.forEach { repository.insertLabel(it) }
        }

        // Restore notes
        notesJson?.let {
            val notesType = object : TypeToken<List<NoteWithAttachments>>() {}.type
            val notesWithAttachments: List<NoteWithAttachments> = Gson().fromJson(it, notesType)
            notesWithAttachments.forEach { noteWithAttachments ->
                val oldProjectId = noteWithAttachments.note.projectId
                val newProjectId = oldToNewProjectIds[oldProjectId]
                val newNote = noteWithAttachments.note.copy(id = 0, projectId = newProjectId)
                val newNoteId = repository.insertNote(newNote).toInt()
                
                // Restore Checklist Items
                if (noteWithAttachments.checklistItems.isNotEmpty()) {
                    val newChecklistItems = noteWithAttachments.checklistItems.map { checklistItem ->
                        checklistItem.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            noteId = newNoteId
                        )
                    }
                    repository.insertChecklistItems(newChecklistItems)
                }
            }
        }
    }


    fun importFromGoogleKeep(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = "Importing from Google Keep...")
            withContext(Dispatchers.IO) {
                try {
                    application.contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zis ->
                            var zipEntry = zis.nextEntry
                            var importedCount = 0
                            val gson = Gson()
                            
                            while (zipEntry != null) {
                                if (!zipEntry.isDirectory && zipEntry.name.endsWith(".json")) {
                                    try {
                                        // Use helper to read text safely
                                        val jsonString = readZipEntryText(zis)
                                        val keepNote = gson.fromJson(jsonString, KeepNote::class.java)
                                        
                                        if (keepNote != null && !keepNote.isTrashed) {
                                            saveKeepNote(keepNote)
                                            importedCount++
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                zipEntry = zis.nextEntry
                            }
                            _state.value = _state.value.copy(
                                isRestoring = false,
                                restoreResult = "Imported $importedCount notes from Google Keep"
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Import failed: ${e.message}")
                }
            }
        }
    }

    private fun readZipEntryText(zis: ZipInputStream): String {
        // Read all bytes of the current entry and convert to String using UTF-8
        // This avoids splitting multi-byte characters (like Hindi/Emoji) across buffer chunks
        val text = zis.readBytes().toString(Charsets.UTF_8)
        return text
    }

    private suspend fun saveKeepNote(keepNote: KeepNote) {
        val color = mapKeepColor(keepNote.color)
        val noteType = if (!keepNote.listContent.isNullOrEmpty()) "CHECKLIST" else "TEXT"
        val content = keepNote.textContent ?: ""
        
        val newNote = Note(
            title = keepNote.title ?: "",
            content = content,
            createdAt = keepNote.createdTimestampUsec / 1000,
            lastEdited = keepNote.userEditedTimestampUsec / 1000,
            color = color,
            isPinned = keepNote.isPinned,
            isArchived = keepNote.isArchived,
            noteType = noteType,
            label = keepNote.labels?.firstOrNull()?.name 
        )
        
        val noteId = repository.insertNote(newNote).toInt()

        val listContent = keepNote.listContent
        if (!listContent.isNullOrEmpty()) {
            val checklistItems = listContent.mapIndexed { index, item ->
                ChecklistItem(
                    noteId = noteId,
                    text = item.text,
                    isChecked = item.isChecked,
                    position = index
                )
            }
            repository.insertChecklistItems(checklistItems)
        }
        
        keepNote.labels?.forEach { keepLabel ->
             try {
                repository.insertLabel(Label(keepLabel.name))
             } catch (e: Exception) {}
        }
    }
    
    private fun mapKeepColor(keepColor: String?): Int {
        return when (keepColor) {
            "RED" -> android.graphics.Color.parseColor("#F28B82")
            "ORANGE" -> android.graphics.Color.parseColor("#FBBC04")
            "YELLOW" -> android.graphics.Color.parseColor("#FFF475")
            "GREEN" -> android.graphics.Color.parseColor("#CCFF90")
            "TEAL" -> android.graphics.Color.parseColor("#A7FFEB")
            "BLUE" -> android.graphics.Color.parseColor("#CBF0F8")
            "DARK_BLUE" -> android.graphics.Color.parseColor("#AECBFA")
            "PURPLE" -> android.graphics.Color.parseColor("#D7AEFB")
            "PINK" -> android.graphics.Color.parseColor("#FDCFE8")
            "BROWN" -> android.graphics.Color.parseColor("#E6C9A8")
            "GRAY" -> android.graphics.Color.parseColor("#E8EAED")
             else -> 0 
        }
    }

    fun restoreFromDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, fileId: String? = null) {
        viewModelScope.launch {
             _state.value = _state.value.copy(isRestoring = true, restoreResult = "Downloading from Drive...")
             val backupName = if (fileId != null) "selected version" else "latest backup"
            withContext(Dispatchers.IO) {
                try {
                     val tempFile = File(application.cacheDir, "temp_restore.zip")
                     googleDriveManager.downloadBackup(application, account, tempFile, fileId)
                     
                     java.io.FileInputStream(tempFile).use { inputStream ->
                         ZipInputStream(inputStream).use { zis ->
                             readBackupFromZip(zis)
                         }
                     }
                     tempFile.delete()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Drive Restore ($backupName) successful")
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Drive Restore failed: ${e.message}")
                }
            }
        }
    }

    fun checkDriveBackupStatus(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isCheckingBackup = true
            )
            withContext(Dispatchers.IO) {
                try {
                    val metadata = googleDriveManager.getBackupMetadata(application, account)
                    _state.value = _state.value.copy(
                        isCheckingBackup = false,
                        driveBackupExists = metadata != null,
                        driveBackupMetadata = metadata,
                        googleAccountEmail = account.email
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                   _state.value = _state.value.copy(isCheckingBackup = false, driveBackupExists = false, driveBackupMetadata = null)
                }
            }
        }
    }

    fun refreshBackupVersions(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingVersions = true)
            withContext(Dispatchers.IO) {
                try {
                    val versions = googleDriveManager.getBackups(application, account)
                    _state.value = _state.value.copy(isLoadingVersions = false, backupVersions = versions)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isLoadingVersions = false)
                }
            }
        }
    }

    fun deleteBackupVersion(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, fileId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true)
            withContext(Dispatchers.IO) {
                try {
                    googleDriveManager.deleteBackupFile(application, account, fileId)
                    refreshBackupVersions(account) // Refresh list
                    checkDriveBackupStatus(account) // Refresh latest status
                    _state.value = _state.value.copy(isDeleting = false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isDeleting = false, backupResult = "Failed to delete: ${e.message}")
                }
            }
        }
    }

    fun deleteDriveBackup(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        // Keeps legacy behavior of deleting ALL backups
        viewModelScope.launch {
             _state.value = _state.value.copy(isDeleting = true, backupResult = "Deleting Drive Backups...")
            withContext(Dispatchers.IO) {
                try {
                     googleDriveManager.deleteBackup(application, account)
                    _state.value = _state.value.copy(
                        isDeleting = false, 
                        backupResult = "Drive Backups deleted successfully",
                        driveBackupExists = false,
                        backupVersions = emptyList()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isDeleting = false, backupResult = "Failed to delete backup: ${e.message}")
                }
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean, email: String? = null, frequency: String = "Daily") {
        viewModelScope.launch {
            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
            sharedPrefs.edit().putString("backup_frequency", frequency).apply()

            _state.value = _state.value.copy(isAutoBackupEnabled = enabled, backupFrequency = frequency)

            if (enabled && email != null) {
                scheduleWorker(email, frequency)
            } else {
                cancelWorker()
            }
        }
    }

    fun toggleSdCardAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("sd_card_backup_enabled", enabled).apply()
            
            _state.value = _state.value.copy(isSdCardAutoBackupEnabled = enabled)
            
            refreshWorkerSchedule()
        }
    }

    fun toggleIncludeAttachments(enabled: Boolean) {
         viewModelScope.launch {
            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("include_backup_attachments", enabled).apply()
            _state.value = _state.value.copy(includeAttachments = enabled)
        }
    }

    fun toggleAutoBackupEncryption(enabled: Boolean, password: String? = null) {
        viewModelScope.launch {
            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().apply {
                putBoolean("auto_backup_encryption_enabled", enabled)
                if (enabled && password != null) {
                    putString("auto_backup_password", password)
                } else if (!enabled) {
                    remove("auto_backup_password")
                }
                apply()
            }
            _state.value = _state.value.copy(isAutoBackupEncryptionEnabled = enabled)
        }
    }

    fun setSdCardLocation(uri: Uri) {
        viewModelScope.launch {
            // Take persistable permission
            val contentResolver = application.contentResolver
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("sd_card_folder_uri", uri.toString()).apply()
            
            _state.value = _state.value.copy(sdCardFolderUri = uri.toString())
            refreshWorkerSchedule()
        }
    }

    fun backupToSdCard(password: String? = null) {
        val uriString = state.value.sdCardFolderUri ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBackingUp = true, backupResult = "Backing up to SD Card...")
            withContext(Dispatchers.IO) {
                try {
                    val result = if (password.isNullOrBlank()) {
                         backupRepository.backupToUri(Uri.parse(uriString), state.value.includeAttachments)
                    } else {
                         backupRepository.backupToEncryptedFolder(Uri.parse(uriString), password, state.value.includeAttachments)
                    }
                     _state.value = _state.value.copy(isBackingUp = false, backupResult = result)
                } catch (e: Exception) {
                    e.printStackTrace()
                     _state.value = _state.value.copy(isBackingUp = false, backupResult = "SD Card Backup failed: ${e.message}")
                }
            }
        }
    }

    private fun refreshWorkerSchedule() {
        // ... (No changes needed) ...
        val currentState = state.value
        val email = currentState.googleAccountEmail
        val frequency = currentState.backupFrequency
        
        if ((currentState.isAutoBackupEnabled && email != null) || (currentState.isSdCardAutoBackupEnabled && currentState.sdCardFolderUri != null)) {
            scheduleWorker(email, frequency)
        } else {
            cancelWorker()
        }
    }

    private fun scheduleWorker(email: String?, frequency: String) {
        // ... (No changes needed) ...
        val workManager = androidx.work.WorkManager.getInstance(application)
        
        val repeatInterval = if (frequency == "Daily") 1L else 7L
        val timeUnit = java.util.concurrent.TimeUnit.DAYS

        val inputDataBuilder = androidx.work.Data.Builder()
        if (email != null) {
            inputDataBuilder.putString("email", email)
        }
        val inputData = inputDataBuilder.build()

        val constraints = androidx.work.Constraints.Builder()
            //.setRequiredNetworkType(androidx.work.NetworkType.CONNECTED) // Only needed if Drive backup is active? 
            // Better to keep it if we might do Drive backup. If only local, network not strictly needed but harmless.
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.suvojeet.notenext.data.backup.BackupWorker>(repeatInterval, timeUnit)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
        
        // Use REPLACE policy to update constraints/data
        workManager.enqueueUniquePeriodicWork(
            "auto_backup",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE, 
            workRequest
        )
    }

    private fun cancelWorker() {
        // ... (No changes included in replace for brevity if unchanged logic, but here I must provide full replacement or matching context.
        androidx.work.WorkManager.getInstance(application).cancelUniqueWork("auto_backup")
    }

    // ... (rest of the file like scanBackup, restoreSelectedProjects etc. which I will include to keep file valid) ...
     fun scanBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true, restoreResult = null, foundProjects = emptyList())
            withContext(Dispatchers.IO) {
                try {
                    val projects = backupRepository.readProjectsFromZip(uri)
                    _state.value = _state.value.copy(isScanning = false, foundProjects = projects)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isScanning = false, restoreResult = "Failed to scan backup: ${e.message}")
                }
            }
        }
    }

    fun restoreSelectedProjects(uri: Uri, selectedProjectIds: List<Int>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = "Restoring selected projects...")
            withContext(Dispatchers.IO) {
                try {
                    backupRepository.restoreSelectedProjects(uri, selectedProjectIds)
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Selected projects restored successfully", foundProjects = emptyList())
                } catch (e: Exception) {
                     e.printStackTrace()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Restore failed: ${e.message}")
                }
            }
        }
    }

    fun clearFoundProjects() {
        _state.value = _state.value.copy(foundProjects = emptyList())
    }
}
