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
    val backupResult: String? = null,
    val restoreResult: String? = null,
    val driveBackupExists: Boolean = false,
    val isCheckingBackup: Boolean = false,
    val isDeleting: Boolean = false

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val application: Application,
    private val googleDriveManager: GoogleDriveManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupRestoreState())
    val state = _state.asStateFlow()

    fun getBackupDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // ... (Existing logic for stats calculation)
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

    private suspend fun writeBackupToZip(zos: ZipOutputStream) {
        // Backup notes
        val notes = repository.getNotes().first()
        val notesJson = Gson().toJson(notes)
        zos.putNextEntry(ZipEntry("notes.json"))
        zos.write(notesJson.toByteArray())
        zos.closeEntry()

        // Backup labels
        val labels = repository.getLabels().first()
        val labelsJson = Gson().toJson(labels)
        zos.putNextEntry(ZipEntry("labels.json"))
        zos.write(labelsJson.toByteArray())
        zos.closeEntry()

        // Backup projects
        val projects = repository.getProjects().first()
        val projectsJson = Gson().toJson(projects)
        zos.putNextEntry(ZipEntry("projects.json"))
        zos.write(projectsJson.toByteArray())
        zos.closeEntry()

        // Backup attachments
        val attachments = notes.flatMap { it.attachments }
        attachments.forEach { attachment ->
            try {
                val attachmentUri = Uri.parse(attachment.uri)
                application.contentResolver.openInputStream(attachmentUri)?.use { inputStream ->
                    val fileName = File(attachmentUri.path!!).name
                    zos.putNextEntry(ZipEntry("attachments/$fileName"))
                    inputStream.copyTo(zos)
                    zos.closeEntry()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBackingUp = true, backupResult = null)
            withContext(Dispatchers.IO) {
                try {
                    application.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            ZipOutputStream(fos).use { zos ->
                                writeBackupToZip(zos)
                            }
                        }
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
            _state.value = _state.value.copy(isBackingUp = true, backupResult = "Uploading to Drive...")
            withContext(Dispatchers.IO) {
                try {
                    val tempFile = File(application.cacheDir, "temp_backup.zip")
                    FileOutputStream(tempFile).use { fos ->
                        ZipOutputStream(fos).use { zos ->
                            writeBackupToZip(zos)
                        }
                    }
                    
                    googleDriveManager.uploadBackup(application, account, tempFile)
                    tempFile.delete()
                    
                    _state.value = _state.value.copy(
                        isBackingUp = false,
                        backupResult = "Drive Backup successful",
                        driveBackupExists = true
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isBackingUp = false, backupResult = "Drive Backup failed: ${e.message}")
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
                repository.insertNote(newNote)
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
                        driveBackupExists = exists
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
}
