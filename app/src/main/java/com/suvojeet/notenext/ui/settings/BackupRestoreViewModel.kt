package com.suvojeet.notenext.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.notenext.data.*
import com.suvojeet.notenext.data.backup.GoogleDriveManager
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
    val isSdCardAutoBackupEnabled: Boolean = false,
    val sdCardFolderUri: String? = null
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
        _state.value = _state.value.copy(isAutoBackupEnabled = enabled, backupFrequency = frequency)
        val sdCardEnabled = sharedPrefs.getBoolean("sd_card_backup_enabled", false)
        val sdCardUri = sharedPrefs.getString("sd_card_folder_uri", null)
        _state.value = _state.value.copy(
            isAutoBackupEnabled = enabled, 
            backupFrequency = frequency,
            isSdCardAutoBackupEnabled = sdCardEnabled,
            sdCardFolderUri = sdCardUri
        )
    }

    fun setGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        _state.value = _state.value.copy(googleAccountEmail = account?.email)
        if (account != null) {
            checkDriveBackupStatus(account)
        } else {
            _state.value = _state.value.copy(driveBackupExists = false)
        }
    }

    fun signOut(context: android.content.Context) {
         com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).signOut().addOnCompleteListener {
             _state.value = _state.value.copy(googleAccountEmail = null, driveBackupExists = false)
        }
    }

    fun getBackupDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
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

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBackingUp = true, backupResult = null)
            withContext(Dispatchers.IO) {
                try {
                    application.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        backupRepository.createBackupZip(outputStream)
                    }
                    _state.value = _state.value.copy(isBackingUp = false, backupResult = "Local Backup successful")
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
                    backupRepository.backupToDrive(account) { uploaded, total ->
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

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = null)
            withContext(Dispatchers.IO) {
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

    fun restoreFromDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
             _state.value = _state.value.copy(isRestoring = true, restoreResult = "Downloading from Drive...")
            withContext(Dispatchers.IO) {
                try {
                     val tempFile = File(application.cacheDir, "temp_restore.zip")
                     googleDriveManager.downloadBackup(application, account, tempFile)
                     
                     java.io.FileInputStream(tempFile).use { inputStream ->
                         ZipInputStream(inputStream).use { zis ->
                             readBackupFromZip(zis)
                         }
                     }
                     tempFile.delete()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Drive Restore successful")
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
                    val exists = googleDriveManager.checkForBackup(application, account)
                    _state.value = _state.value.copy(
                        isCheckingBackup = false,
                        driveBackupExists = exists,
                        googleAccountEmail = account.email
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                   _state.value = _state.value.copy(isCheckingBackup = false, driveBackupExists = false)
                }
            }
        }
    }

    fun deleteDriveBackup(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
             _state.value = _state.value.copy(isDeleting = true, backupResult = "Deleting Drive Backup...")
            withContext(Dispatchers.IO) {
                try {
                     googleDriveManager.deleteBackup(application, account)
                    _state.value = _state.value.copy(
                        isDeleting = false, 
                        backupResult = "Drive Backup deleted successfully",
                        driveBackupExists = false
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

    fun backupToSdCard() {
        val uriString = state.value.sdCardFolderUri ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBackingUp = true, backupResult = "Backing up to SD Card...")
            withContext(Dispatchers.IO) {
                try {
                    val result = backupRepository.backupToUri(Uri.parse(uriString))
                     _state.value = _state.value.copy(isBackingUp = false, backupResult = result)
                } catch (e: Exception) {
                    e.printStackTrace()
                     _state.value = _state.value.copy(isBackingUp = false, backupResult = "SD Card Backup failed: ${e.message}")
                }
            }
        }
    }

    private fun refreshWorkerSchedule() {
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
        androidx.work.WorkManager.getInstance(application).cancelUniqueWork("auto_backup")
    }

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
