package com.suvojeet.notenext.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.*
import com.suvojeet.notenext.data.backup.GoogleDriveManager
import com.suvojeet.notenext.data.backup.KeepNote
import com.suvojeet.notenext.data.backup.KeepLabel
import com.suvojeet.notenext.data.backup.SecurityUtils
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.data.repository.BackupSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    val foundBackupDetails: com.suvojeet.notenext.data.backup.BackupScanResult? = null,
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
    val pendingMerge: Boolean = false,
    val isEncryptionEnabled: Boolean = false,
    val hasPasswordSet: Boolean = false,
    val lastBackupTime: Long = 0L,
    val lastBackupStatus: String? = null,
    val isIncrementalEnabled: Boolean = false,
    val isSmartBackupEnabled: Boolean = false,
    val isChargingConstraintEnabled: Boolean = false,
    val editsThreshold: Int = 10,
    val currentEditCount: Int = 0
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val backupRepository: com.suvojeet.notenext.data.backup.BackupRepository,
    private val todoRepository: com.suvojeet.notenext.data.TodoRepository,
    private val application: Application,
    private val googleDriveManager: GoogleDriveManager,
    private val backupSettingsRepository: BackupSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BackupRestoreState())
    val state = _state.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    init {
        observeSettings()
    }

    private fun observeSettings() {
        backupSettingsRepository.autoBackupEnabled.onEach { valEnabled ->
            _state.update { it.copy(isAutoBackupEnabled = valEnabled) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.backupFrequency.onEach { valFreq ->
            _state.update { it.copy(backupFrequency = valFreq) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.includeAttachments.onEach { valInc ->
            _state.update { it.copy(includeAttachments = valInc) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.sdCardEnabled.onEach { valSd ->
            _state.update { it.copy(isSdCardAutoBackupEnabled = valSd) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.backupLocationUri.onEach { valUri ->
            _state.update { it.copy(sdCardFolderUri = valUri) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.encryptionEnabled.onEach { valEnc ->
            _state.update { it.copy(isEncryptionEnabled = valEnc) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.lastBackupTime.onEach { valTime ->
            _state.update { it.copy(lastBackupTime = valTime) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.lastBackupStatus.onEach { valStatus ->
            _state.update { it.copy(lastBackupStatus = valStatus) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.incrementalEnabled.onEach { valInc ->
            _state.update { it.copy(isIncrementalEnabled = valInc) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.smartBackupEnabled.onEach { valSmart ->
            _state.update { it.copy(isSmartBackupEnabled = valSmart) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.chargingConstraint.onEach { valCharge ->
            _state.update { it.copy(isChargingConstraintEnabled = valCharge) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.editsThreshold.onEach { valThresh ->
            _state.update { it.copy(editsThreshold = valThresh) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.editCounter.onEach { valCount ->
            _state.update { it.copy(currentEditCount = valCount) }
        }.launchIn(viewModelScope)

        backupSettingsRepository.googleAccountEmail.onEach { valEmail ->
            _state.update { it.copy(googleAccountEmail = valEmail) }
        }.launchIn(viewModelScope)

        // Password set check
        viewModelScope.launch {
            val hasPassword = SecurityUtils.getBackupPassword(application) != null
            _state.update { it.copy(hasPasswordSet = hasPassword) }
        }
    }

    private fun updateLastBackup(status: String) {
        val time = System.currentTimeMillis()
        viewModelScope.launch {
            backupSettingsRepository.setLastBackupTime(time)
            backupSettingsRepository.setLastBackupStatus(status)
        }
    }

    fun setGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        viewModelScope.launch {
            backupSettingsRepository.setGoogleAccountEmail(account?.email)
            if (account != null) {
                checkDriveBackupStatus(account)
                refreshBackupVersions(account)
            } else {
                _state.update { it.copy(driveBackupExists = false, backupVersions = emptyList()) }
            }
        }
    }

    fun signOut(context: android.content.Context) {
         com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).signOut().addOnCompleteListener {
             viewModelScope.launch {
                 backupSettingsRepository.setGoogleAccountEmail(null)
                 _state.update { it.copy(googleAccountEmail = null, driveBackupExists = false, backupVersions = emptyList()) }
             }
        }
    }

    fun getBackupDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val notes = repository.getNotes().first()
                val labels = repository.getLabels().first()
                val projects = repository.getProjects().first()
                val attachments = notes.flatMap { it.attachments }

                val notesJson = json.encodeToString(ListSerializer(NoteWithAttachments.serializer()), notes)
                val labelsJson = json.encodeToString(ListSerializer(Label.serializer()), labels)
                val projectsJson = json.encodeToString(ListSerializer(Project.serializer()), projects)

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

                _state.update { it.copy(
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
                ) }
            }
        }
    }

    fun setEncryption(password: String) {
        SecurityUtils.saveBackupPassword(application, password)
        viewModelScope.launch {
            backupSettingsRepository.setEncryptionEnabled(true)
            _state.update { it.copy(isEncryptionEnabled = true, hasPasswordSet = true) }
            refreshWorkerSchedule()
        }
    }

    fun disableEncryption() {
        SecurityUtils.saveBackupPassword(application, null)
        viewModelScope.launch {
            backupSettingsRepository.setEncryptionEnabled(false)
            _state.update { it.copy(isEncryptionEnabled = false, hasPasswordSet = false) }
            refreshWorkerSchedule()
        }
    }

    fun changePassword(newPassword: String) {
        setEncryption(newPassword)
    }

    fun createBackup(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isBackingUp = true, backupResult = null) }
            withContext(Dispatchers.IO) {
                try {
                    val storedPassword = SecurityUtils.getBackupPassword(application)
                    val effectivePassword = password ?: if (state.value.isEncryptionEnabled) storedPassword else null

                    if (effectivePassword.isNullOrBlank()) {
                        application.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            backupRepository.createBackupZip(outputStream, state.value.includeAttachments)
                        }
                        _state.update { it.copy(isBackingUp = false, backupResult = "Local Backup successful") }
                        updateLastBackup("Success (Local)")
                    } else {
                        backupRepository.backupToEncryptedStream(
                            application.contentResolver.openOutputStream(uri), 
                            effectivePassword, 
                            state.value.includeAttachments
                        )
                        _state.update { it.copy(isBackingUp = false, backupResult = "Encrypted Local Backup successful") }
                        updateLastBackup("Success (Encrypted Local)")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(isBackingUp = false, backupResult = "Local Backup failed: ${e.message}") }
                    updateLastBackup("Failed (Local)")
                }
            }
        }
    }

    fun backupToDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
         viewModelScope.launch {
            _state.update { it.copy(
                isBackingUp = true, 
                backupResult = "Uploading to Drive...",
                uploadProgress = "Starting..."
            ) }
            withContext(Dispatchers.IO) {
                try {
                    val storedPassword = SecurityUtils.getBackupPassword(application)
                    val password = if (state.value.isEncryptionEnabled) storedPassword else null
                    
                    backupRepository.backupToDrive(account, password, state.value.includeAttachments) { uploaded, total ->
                        val progress = if (total > 0) {
                            val percent = (uploaded * 100) / total
                            val uploadedMb = String.format("%.2f", uploaded / (1024.0 * 1024.0))
                            val totalMb = String.format("%.2f", total / (1024.0 * 1024.0))
                            "$uploadedMb MB / $totalMb MB ($percent%)"
                        } else {
                            "Uploading..."
                        }
                        _state.update { it.copy(uploadProgress = progress) }
                    }
                    
                    _state.update { it.copy(
                        isBackingUp = false,
                        backupResult = "Drive Backup successful",
                        driveBackupExists = true,
                        uploadProgress = null
                    ) }
                    updateLastBackup("Success (Drive)")
                    refreshBackupVersions(account)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(
                        isBackingUp = false, 
                        backupResult = "Drive Backup failed: ${e.message}",
                        uploadProgress = null
                    ) }
                    updateLastBackup("Failed (Drive)")
                }
            }
        }
    }

    fun restoreBackup(uri: Uri, merge: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, restoreResult = null) }
            withContext(Dispatchers.IO) {
                if (backupRepository.checkIsEncrypted(uri)) {
                    _state.update { it.copy(
                        isRestoring = false, 
                        isPasswordRequired = true, 
                        pendingRestoreUri = uri.toString(),
                        pendingMerge = merge
                    ) }
                    return@withContext
                }
                
                try {
                    val missing = application.contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zis ->
                            readBackupFromZip(zis, merge, uri)
                        }
                    } ?: 0
                    val result = if (merge) "Merge successful" else "Local Restore successful"
                    _state.update { it.copy(isRestoring = false, restoreResult = withMissingAttachments(result, missing)) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(isRestoring = false, restoreResult = "Local Restore failed: ${e.message}") }
                }
            }
        }
    }

    fun restoreEncryptedBackup(password: String) {
        val uriString = state.value.pendingRestoreUri ?: return
        val uri = Uri.parse(uriString)
        val merge = state.value.pendingMerge
        
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, restoreResult = null, isPasswordRequired = false) }
            withContext(Dispatchers.IO) {
                try {
                    val tempZipFile = backupRepository.decryptBackupToTempFile(uri, password)
                    try {
                        val missing = java.io.FileInputStream(tempZipFile).use { inputStream ->
                            ZipInputStream(inputStream).use { zis ->
                                readBackupFromZip(zis, merge, Uri.fromFile(tempZipFile))
                            }
                        }
                        val result = if (merge) "Encrypted Merge successful" else "Encrypted Restore successful"
                        _state.update { it.copy(isRestoring = false, restoreResult = withMissingAttachments(result, missing), pendingRestoreUri = null, pendingMerge = false) }
                    } finally {
                        if (tempZipFile.exists()) tempZipFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(
                        isRestoring = false, 
                        restoreResult = "Restore failed: ${e.message}"
                    ) }
                }
            }
        }
    }

    fun cancelPasswordEntry() {
        val uriString = _state.value.pendingRestoreUri
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) file.delete()
            }
        }
        _state.update { it.copy(isPasswordRequired = false, pendingRestoreUri = null) }
    }
    
    /** Returns the number of attachments the archive could not supply. */
    private suspend fun readBackupFromZip(zis: ZipInputStream, merge: Boolean = false, zipUri: Uri): Int {
        val oldToNewProjectIds = mutableMapOf<Int, Int>()
        var notesJson: String? = null
        var labelsJson: String? = null
        var projectsJson: String? = null
        var manifestJson: String? = null
        // Read Todo archives for restoration
        var todosJson: String? = null
        var todoSubtasksJson: String? = null

        // Pass 1: Read JSON Data from ZIP
        try {
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                when (zipEntry.name) {
                    "notes.json" -> notesJson = InputStreamReader(zis).readText()
                    "labels.json" -> labelsJson = InputStreamReader(zis).readText()
                    "projects.json" -> projectsJson = InputStreamReader(zis).readText()
                    "manifest.json" -> manifestJson = InputStreamReader(zis).readText()
                    "todos.json" -> todosJson = InputStreamReader(zis).readText()
                    "todo_subtasks.json" -> todoSubtasksJson = InputStreamReader(zis).readText()
                }
                zipEntry = zis.nextEntry
            }
        } catch (e: Exception) {
            throw Exception("Failed to read backup contents: ${e.message}")
        }

        // Pass 1.5: Verify Checksums
        if (manifestJson != null) {
            val manifest: Map<String, String> = json.decodeFromString(manifestJson)
            val md = java.security.MessageDigest.getInstance("SHA-256")
            
            fun verify(name: String, content: String?) {
                if (content != null && manifest.containsKey(name)) {
                    val hashBytes = md.digest(content.toByteArray())
                    val hashString = hashBytes.joinToString("") { "%02x".format(it) }
                    if (hashString != manifest[name]) {
                        throw Exception("Data corruption detected: $name failed checksum verification.")
                    }
                }
            }
            
            verify("notes.json", notesJson)
            verify("labels.json", labelsJson)
            verify("projects.json", projectsJson)
            verify("todos.json", todosJson)
            verify("todo_subtasks.json", todoSubtasksJson)
        }

        // Pass 2: Deserialize into in-memory objects BEFORE deletion
        if (notesJson == null) {
            throw Exception("Invalid backup: Missing notes.json")
        }

        val projectsToRestore = projectsJson?.let {
            json.decodeFromString(ListSerializer(Project.serializer()), it)
        } ?: emptyList()

        val labelsToRestore = labelsJson?.let {
            json.decodeFromString(ListSerializer(Label.serializer()), it)
        } ?: emptyList()

        val notesToRestore = json.decodeFromString(ListSerializer(NoteWithAttachments.serializer()), notesJson)
        
        val manifest = manifestJson?.let { json.decodeFromString<Map<String, String>>(it) }
        val isIncremental = manifest?.get("is_incremental")?.toBoolean() ?: false
        val effectiveMerge = merge || isIncremental
        val attachmentUriMap: Map<String, String> = manifest?.get("attachment_uri_map")?.let { json.decodeFromString(it) } ?: emptyMap()

        val attachmentsToExtract = mutableListOf<Pair<String, File>>() // ZipEntryName -> TargetFile

        // Pass 2.5: Pre-fetch data needed for deletion outside transaction to minimize lock time
        val existingNotes = if (!effectiveMerge) repository.getNotes().first() else emptyList()
        val existingLabels = if (!effectiveMerge) repository.getLabels().first() else emptyList()
        val existingProjects = if (!effectiveMerge) repository.getProjects().first() else emptyList()

        // Transaction 1: Clear existing data (if not merging)
        if (!effectiveMerge) {
            repository.runInTransaction {
                existingNotes.flatMap { it.attachments }.forEach { repository.deleteAttachment(it) }
                existingNotes.forEach { repository.deleteNote(it.note) }
                existingLabels.forEach { repository.deleteLabel(it) }
                existingProjects.forEach { repository.deleteProject(it.id) }
                // Wipe todos on full restore (subtasks cascade via FK).
                todoRepository.deleteAllTodos()
            }
        }

        // Pass 3: Restore Data in chunks
        
        // Restore Projects
        repository.runInTransaction {
            projectsToRestore.forEach { project ->
                val oldId = project.id
                val newId = repository.insertProject(project.copy(id = 0))
                require(newId <= Int.MAX_VALUE) { "Project ID overflow" }
                oldToNewProjectIds[oldId] = newId.toInt()
            }
        }

        // Restore Labels
        repository.runInTransaction {
            labelsToRestore.forEach { 
                try {
                    repository.insertLabel(it) 
                } catch (e: Exception) {
                    // Label might already exist, ignore in merge mode
                }
            }
        }

        // Restore Notes in chunks of 50 to avoid long DB locks
        notesToRestore.chunked(50).forEach { chunk ->
            repository.runInTransaction {
                chunk.forEach { noteWithAttachments ->
                    val oldProjectId = noteWithAttachments.note.projectId
                    val newProjectId = if (oldProjectId != null) oldToNewProjectIds[oldProjectId] else null
                    
                    // If merging/incremental, check if note with same title + createdAt exists (Fix 5)
                    val existingNoteId = if (effectiveMerge) {
                        repository.getNoteByTitleAndCreatedAt(
                            noteWithAttachments.note.title,
                            noteWithAttachments.note.createdAt
                        )?.id
                    } else null

                    val newNoteId = if (existingNoteId != null) {
                        // Update existing
                        repository.updateNote(noteWithAttachments.note.copy(id = existingNoteId, projectId = newProjectId))
                        existingNoteId.toLong()
                    } else {
                        // Insert new
                        val id = repository.insertNote(noteWithAttachments.note.copy(id = 0, projectId = newProjectId))
                        require(id <= Int.MAX_VALUE) { "Note ID overflow" }
                        id
                    }
                    
                    // Handle checklist items
                    if (noteWithAttachments.checklistItems.isNotEmpty()) {
                        if (existingNoteId != null) {
                            repository.deleteChecklistForNote(existingNoteId)
                        }

                        val newChecklistItems = noteWithAttachments.checklistItems.map { checklistItem ->
                            checklistItem.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                noteId = newNoteId.toInt()
                            )
                        }
                        repository.insertChecklistItems(newChecklistItems)
                    }

                    // Handle Attachments
                    noteWithAttachments.attachments.forEach { attachment ->
                        try {
                            val originalUri = Uri.parse(attachment.uri)
                            val zipEntryName = attachmentUriMap[attachment.uri] ?: run {
                                val fileName = java.io.File(originalUri.path ?: "unknown_${System.currentTimeMillis()}").name
                                "attachments/$fileName"
                            }
                            
                            val fileName = java.io.File(originalUri.path ?: "attachment").name
                            val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
                            val attachmentsDir = java.io.File(application.filesDir, "attachments")
                            if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
                            
                            val targetFile = java.io.File(attachmentsDir, uniqueFileName)
                            // Use FileProvider URI so attachment URIs remain valid across app shares.
                            val newUri = androidx.core.content.FileProvider.getUriForFile(
                                application,
                                "${application.packageName}.fileprovider",
                                targetFile
                            ).toString()
                            
                            val newAttachment = attachment.copy(id = 0, noteId = newNoteId.toInt(), uri = newUri)
                            repository.insertAttachment(newAttachment)
                            
                            attachmentsToExtract.add(zipEntryName to targetFile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // Restore Todos and their Subtasks from the backup archive.
        // Older backups (pre-v1.3.8) did not include these entries — in that case we
        // silently skip. Todos are keyed by (title, createdAt) for merge dedup.
        val todosToRestore: List<TodoItem> = todosJson?.let {
            json.decodeFromString(ListSerializer(TodoItem.serializer()), it)
        } ?: emptyList()
        val subtasksToRestore: List<TodoSubtask> = todoSubtasksJson?.let {
            json.decodeFromString(ListSerializer(TodoSubtask.serializer()), it)
        } ?: emptyList()

        if (todosToRestore.isNotEmpty()) {
            val oldToNewTodoIds = mutableMapOf<Int, Int>()
            repository.runInTransaction {
                todosToRestore.forEach { todo ->
                    val remappedProjectId = todo.projectId?.let { oldToNewProjectIds[it] }
                    val newId = todoRepository.insertTodo(
                        todo.copy(id = 0, projectId = remappedProjectId)
                    ).toInt()
                    oldToNewTodoIds[todo.id] = newId
                }
                val remappedSubtasks = subtasksToRestore.mapNotNull { subtask ->
                    val newParentId = oldToNewTodoIds[subtask.todoId] ?: return@mapNotNull null
                    subtask.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        todoId = newParentId
                    )
                }
                if (remappedSubtasks.isNotEmpty()) {
                    todoRepository.insertSubtasks(remappedSubtasks)
                }
            }
        }

        // Pass 4: Extract Attachment Files (Fix 2)
        return if (attachmentsToExtract.isNotEmpty()) {
            backupRepository.extractAttachmentsFromZip(zipUri, attachmentsToExtract)
        } else {
            0
        }
    }

    /**
     * Appends a note about attachments the archive could not supply. Restoring
     * the text but silently dropping images and voice notes reads as success
     * otherwise, and the user only finds out when they open the note.
     */
    private fun withMissingAttachments(message: String, missing: Int): String =
        if (missing > 0) "$message ($missing attachment(s) missing)" else message

    fun importFromGoogleKeep(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, restoreResult = "Importing from Google Keep...") }
            withContext(Dispatchers.IO) {
                try {
                    var importedCount = 0
                    application.contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zis ->
                            val notesToSave = mutableListOf<KeepNote>()
                            var zipEntry = zis.nextEntry
                            
                            while (zipEntry != null) {
                                if (!zipEntry.isDirectory && zipEntry.name.endsWith(".json")) {
                                    try {
                                        val jsonString = readZipEntryText(zis)
                                        val keepNote: KeepNote = json.decodeFromString(KeepNote.serializer(), jsonString)
                                        if (!keepNote.isTrashed) {
                                            notesToSave.add(keepNote)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                zipEntry = zis.nextEntry
                            }

                            if (notesToSave.isNotEmpty()) {
                                repository.runInTransaction {
                                    notesToSave.forEach { keepNote ->
                                        saveKeepNote(keepNote)
                                        importedCount++
                                    }
                                }
                            }
                        }
                    }
                    _state.update { it.copy(
                        isRestoring = false,
                        restoreResult = "Imported $importedCount notes from Google Keep"
                    ) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(isRestoring = false, restoreResult = "Import failed: ${e.message}") }
                }
            }
        }
    }

    private fun readZipEntryText(zis: ZipInputStream): String {
        return zis.readBytes().toString(Charsets.UTF_8)
    }

    private suspend fun saveKeepNote(keepNote: KeepNote) {
        val color = mapKeepColor(keepNote.color)
        val noteType = if (!keepNote.listContent.isNullOrEmpty()) NoteType.CHECKLIST else NoteType.TEXT
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
        
        val newIdLong = repository.insertNote(newNote)
        require(newIdLong <= Int.MAX_VALUE) { "Note ID overflow" }
        val noteId = newIdLong.toInt()

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
             } catch (e: Exception) {
                // Label already exists (duplicate primary key) — expected during import, skip it.
                android.util.Log.d("BackupRestoreViewModel", "Skipping duplicate label '${keepLabel.name}'")
             }
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

    fun restoreFromDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, fileId: String? = null, merge: Boolean = false) {
        viewModelScope.launch {
             _state.update { it.copy(isRestoring = true, restoreResult = null) }
             val backupName = if (fileId != null) "selected version" else "latest backup"
            withContext(Dispatchers.IO) {
                val tempFile = File(application.cacheDir, "temp_restore.zip")
                var shouldDeleteTempFile = true
                try {
                     googleDriveManager.downloadBackup(application, account, tempFile, fileId)
                     if (backupRepository.checkIsEncrypted(Uri.fromFile(tempFile))) {
                         _state.update { it.copy(
                            isRestoring = false,
                            isPasswordRequired = true,
                            pendingRestoreUri = Uri.fromFile(tempFile).toString(),
                            pendingMerge = merge
                         ) }
                         shouldDeleteTempFile = false
                         return@withContext
                     }

                     java.io.FileInputStream(tempFile).use { inputStream ->
                         ZipInputStream(inputStream).use { zis ->
                             readBackupFromZip(zis, merge, Uri.fromFile(tempFile))
                         }
                     }
                    _state.update { it.copy(isRestoring = false, restoreResult = if (merge) "Drive Merge successful" else "Drive Restore ($backupName) successful") }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(isRestoring = false, restoreResult = "Drive Restore failed: ${e.message}") }
                } finally {
                    if (shouldDeleteTempFile && tempFile.exists()) {
                        tempFile.delete()
                    }
                }
            }
        }
    }

    fun checkDriveBackupStatus(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _state.update { it.copy(isCheckingBackup = true) }
            withContext(Dispatchers.IO) {
                try {
                    val metadata = googleDriveManager.getBackupMetadata(application, account)
                    _state.update { it.copy(
                        isCheckingBackup = false,
                        driveBackupExists = metadata != null,
                        driveBackupMetadata = metadata,
                        googleAccountEmail = account.email
                    ) }
                } catch (e: Exception) {
                    e.printStackTrace()
                   _state.update { it.copy(isCheckingBackup = false, driveBackupExists = false, driveBackupMetadata = null) }
                }
            }
        }
    }

    fun refreshBackupVersions(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingVersions = true) }
            withContext(Dispatchers.IO) {
                try {
                    val versions = googleDriveManager.getBackups(application, account)
                    _state.update { it.copy(isLoadingVersions = false, backupVersions = versions) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(isLoadingVersions = false) }
                }
            }
        }
    }

    fun deleteBackupVersion(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, fileId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            withContext(Dispatchers.IO) {
                try {
                    googleDriveManager.deleteBackupFile(application, account, fileId)
                    refreshBackupVersions(account)
                    checkDriveBackupStatus(account)
                    _state.update { it.copy(isDeleting = false) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(isDeleting = false, backupResult = "Failed to delete: ${e.message}") }
                }
            }
        }
    }

    fun deleteDriveBackup(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
             _state.update { it.copy(isDeleting = true, backupResult = "Deleting Drive Backups...") }
            withContext(Dispatchers.IO) {
                try {
                     googleDriveManager.deleteBackup(application, account)
                    _state.update { it.copy(
                        isDeleting = false, 
                        backupResult = "Drive Backups deleted successfully",
                        driveBackupExists = false,
                        backupVersions = emptyList()
                    ) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(isDeleting = false, backupResult = "Failed to delete backup: ${e.message}") }
                }
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean, email: String? = null, frequency: String = "Daily") {
        viewModelScope.launch {
            backupSettingsRepository.setAutoBackupEnabled(enabled)
            backupSettingsRepository.setBackupFrequency(frequency)
            if (enabled && email != null) {
                scheduleWorker(email, frequency)
            } else {
                cancelWorker()
            }
        }
    }

    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            backupSettingsRepository.setBackupFrequency(frequency)
            refreshWorkerSchedule()
        }
    }

    fun toggleSdCardAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            backupSettingsRepository.setSdCardEnabled(enabled)
            refreshWorkerSchedule()
        }
    }

    fun toggleIncludeAttachments(enabled: Boolean) {
         viewModelScope.launch {
            backupSettingsRepository.setIncludeAttachments(enabled)
        }
    }

    fun setSdCardLocation(uri: Uri) {
        viewModelScope.launch {
            val contentResolver = application.contentResolver
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            backupSettingsRepository.setBackupLocationUri(uri.toString())
            refreshWorkerSchedule()
        }
    }

    fun backupToSdCard() {
        val uriString = state.value.sdCardFolderUri ?: return
        viewModelScope.launch {
            _state.update { it.copy(isBackingUp = true, backupResult = "Backing up to SD Card...") }
            withContext(Dispatchers.IO) {
                try {
                    val storedPassword = SecurityUtils.getBackupPassword(application)
                    val result = if (state.value.isEncryptionEnabled && !storedPassword.isNullOrBlank()) {
                         backupRepository.backupToEncryptedFolder(Uri.parse(uriString), storedPassword, state.value.includeAttachments)
                    } else {
                         backupRepository.backupToUri(Uri.parse(uriString), state.value.includeAttachments)
                    }
                     _state.update { it.copy(isBackingUp = false, backupResult = result) }
                     updateLastBackup("Success (SD Card)")
                } catch (e: Exception) {
                    e.printStackTrace()
                     _state.update { it.copy(isBackingUp = false, backupResult = "SD Card Backup failed: ${e.message}") }
                     updateLastBackup("Failed (SD Card)")
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

    fun toggleIncrementalBackup(enabled: Boolean) {
        viewModelScope.launch {
            backupSettingsRepository.setIncrementalEnabled(enabled)
        }
    }

    fun toggleSmartBackup(enabled: Boolean) {
        viewModelScope.launch {
            backupSettingsRepository.setSmartBackupEnabled(enabled)
        }
    }

    fun toggleChargingConstraint(enabled: Boolean) {
        viewModelScope.launch {
            backupSettingsRepository.setChargingConstraint(enabled)
            refreshWorkerSchedule()
        }
    }

    fun setEditsThreshold(threshold: Int) {
        viewModelScope.launch {
            backupSettingsRepository.setEditsThreshold(threshold)
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
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(state.value.isChargingConstraintEnabled)
            .apply {
                if (email != null) {
                    setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                }
            }
            .build()
            
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.suvojeet.notenext.data.backup.BackupWorker>(repeatInterval, timeUnit)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
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
            _state.update { it.copy(isScanning = true, restoreResult = null, foundBackupDetails = null) }
            withContext(Dispatchers.IO) {
                try {
                    val scanResult = backupRepository.scanBackupContent(uri)
                    _state.update { it.copy(isScanning = false, foundBackupDetails = scanResult) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.update { it.copy(isScanning = false, restoreResult = "Failed to scan backup: ${e.message}") }
                }
            }
        }
    }

    fun restoreSelectedProjects(uri: Uri, selectedProjectIds: List<Int>) {
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, restoreResult = "Restoring selected projects...") }
            withContext(Dispatchers.IO) {
                try {
                    backupRepository.restoreSelectedProjects(uri, selectedProjectIds)
                    _state.update { it.copy(isRestoring = false, restoreResult = "Selected projects restored successfully", foundBackupDetails = null) }
                } catch (e: Exception) {
                     e.printStackTrace()
                    _state.update { it.copy(isRestoring = false, restoreResult = "Restore failed: ${e.message}") }
                }
            }
        }
    }

    fun clearFoundProjects() {
        _state.update { it.copy(foundBackupDetails = null) }
    }
}
