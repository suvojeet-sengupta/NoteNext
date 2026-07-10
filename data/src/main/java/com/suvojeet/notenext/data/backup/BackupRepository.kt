package com.suvojeet.notenext.data.backup

import android.content.Context
import android.net.Uri
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.TodoRepository
import com.suvojeet.notenext.data.TodoSubtask
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async


data class BackupScanResult(
    val projects: List<Project>,
    val labelsCount: Int,
    val notesCount: Int,
    val attachmentsCount: Int,
    val backupTimestamp: Long? = null
)

@Singleton
class BackupRepository @Inject constructor(
    private val repository: NoteRepository,
    private val todoRepository: TodoRepository,
    @ApplicationContext private val context: Context,
    private val googleDriveManager: GoogleDriveManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    suspend fun createBackupZip(targetFile: File, includeAttachments: Boolean = true, since: Long = 0) {
        FileOutputStream(targetFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                writeBackupToZip(zos, includeAttachments, since)
            }
        }
    }
    
    suspend fun createBackupZip(outputStream: java.io.OutputStream, includeAttachments: Boolean = true, since: Long = 0) {
         ZipOutputStream(outputStream).use { zos ->
            writeBackupToZip(zos, includeAttachments, since)
        }
    }

    suspend fun backupToUri(folderUri: Uri, includeAttachments: Boolean = true, since: Long = 0): String {
        return try {
            val validUri = if (folderUri.toString().endsWith("%3A")) {
                 folderUri
            } else folderUri

            val dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, validUri)
            if (dir == null || !dir.isDirectory || !dir.canWrite()) {
                 throw Exception("Cannot write to selected folder. Please select a valid directory.")
            }

            val prefix = if (since > 0) "NoteNext_Incremental_" else "NoteNext_Backup_"
            val fileName = "${prefix}${System.currentTimeMillis()}.zip"
            val file = dir.createFile("application/zip", fileName) 
                ?: throw Exception("Failed to create file in selected directory.")

            context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                createBackupZip(outputStream, includeAttachments, since)
            }
            "Backup successful: $fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to save backup: ${e.message}")
        }
    }

    suspend fun backupToEncryptedFolder(folderUri: Uri, password: String, includeAttachments: Boolean = true, since: Long = 0): String {
        return try {
             val validUri = if (folderUri.toString().endsWith("%3A")) {
                 folderUri
            } else folderUri

            val dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, validUri)
            if (dir == null || !dir.isDirectory || !dir.canWrite()) {
                 throw Exception("Cannot write to selected folder. Please select a valid directory.")
            }

            val prefix = if (since > 0) "NoteNext_Incremental_Encrypted_" else "NoteNext_Backup_Encrypted_"
            val fileName = "${prefix}${System.currentTimeMillis()}.enc"
            val file = dir.createFile("application/octet-stream", fileName) 
                ?: throw Exception("Failed to create file in selected directory.")

            backupToEncryptedStream(context.contentResolver.openOutputStream(file.uri), password, includeAttachments, since)

            "Encrypted Backup successful: $fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to save encrypted backup: ${e.message}")
        }
    }

    suspend fun backupToEncryptedStream(outputStream: java.io.OutputStream?, password: String, includeAttachments: Boolean = true, since: Long = 0) {
        if (outputStream == null) throw Exception("Output stream is null")
        
        // Use Piped streams to avoid writing plain-text temp files to disk for better security
        val pipedInputStream = java.io.PipedInputStream()
        val pipedOutputStream = java.io.PipedOutputStream(pipedInputStream)
        
        coroutineScope {
            // Launch zip writing in a separate coroutine
            val zipJob = launch(Dispatchers.IO) {
                try {
                    createBackupZip(pipedOutputStream, includeAttachments, since)
                } finally {
                    pipedOutputStream.close()
                }
            }
            
            // Encrypt and write to outputStream in the current coroutine
            try {
                outputStream.use { out ->
                    EncryptionUtils.encryptStream(pipedInputStream, out, password)
                }
            } finally {
                zipJob.join()
            }
        }
    }

    fun checkIsEncrypted(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                EncryptionUtils.isEncrypted(inputStream)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    // Helper to decrypt and provide a temp file (caller must delete)
    suspend fun decryptBackupToTempFile(uri: Uri, password: String): File {
        val tempZipFile = File(context.cacheDir, "temp_decrypted_restore_${System.currentTimeMillis()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                EncryptionUtils.decryptFile(inputStream, tempZipFile, password)
            }
            return tempZipFile
        } catch (e: Exception) {
            if (tempZipFile.exists()) tempZipFile.delete()
            throw Exception("Decryption failed. Incorrect password?")
        }
    }

    suspend fun createEncryptedBackupZip(targetFile: File, password: String, includeAttachments: Boolean = true, since: Long = 0) {
        FileOutputStream(targetFile).use { fos ->
            backupToEncryptedStream(fos, password, includeAttachments, since)
        }
    }

    suspend fun backupToDrive(
        account: GoogleSignInAccount, 
        password: String? = null,
        includeAttachments: Boolean = true, 
        since: Long = 0,
        onProgress: ((Long, Long) -> Unit)? = null
    ): String {
        val dbFile = File(context.cacheDir, "temp_backup_${System.currentTimeMillis()}.zip") // Unique filename (Fix 6)
        try {
            if (password.isNullOrBlank()) {
                createBackupZip(dbFile, includeAttachments, since)
            } else {
                createEncryptedBackupZip(dbFile, password, includeAttachments, since)
            }
            return googleDriveManager.uploadBackup(context, account, dbFile, onProgress)
        } finally {
            if (dbFile.exists()) {
                dbFile.delete()
            }
        }
    }

    private suspend fun writeBackupToZip(zos: ZipOutputStream, includeAttachments: Boolean, since: Long = 0) {
        val manifest = mutableMapOf<String, String>()
        val attachmentUriMap = mutableMapOf<String, String>() // Fix 1: Map originalUri to zipEntryName
        val md = MessageDigest.getInstance("SHA-256")
        var missingAttachmentsCount = 0

        fun writeEntryWithHash(name: String, data: ByteArray) {
            zos.putNextEntry(ZipEntry(name))
            zos.write(data)
            zos.closeEntry()
            
            val hashBytes = md.digest(data)
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }
            manifest[name] = hashString
        }

        // Backup core data - Use methods that include archived/binned notes (Fix 9)
        val notes = if (since > 0) {
            repository.getAllNotesModifiedSince(since).first()
        } else {
            repository.getAllNotes().first()
        }
        val labels = repository.getLabels().first()
        val projects = repository.getProjects().first()

        writeEntryWithHash("notes.json", json.encodeToString(ListSerializer(NoteWithAttachments.serializer()), notes).toByteArray())
        writeEntryWithHash("labels.json", json.encodeToString(ListSerializer(Label.serializer()), labels).toByteArray())
        writeEntryWithHash("projects.json", json.encodeToString(ListSerializer(Project.serializer()), projects).toByteArray())

        // Include Todos and their subtasks in FULL backups only.
        // TodoItem has no modifiedAt column and the restore flow has no dedup for
        // todos, so exporting them in incremental backups would double them on each
        // incremental restore. Incremental = notes-only delta; a periodic full backup
        // remains the source of truth for todos.
        if (since == 0L) {
            val todos = todoRepository.getAllTodosList()
            val subtasks = todoRepository.getAllSubtasksList()
            writeEntryWithHash("todos.json", json.encodeToString(ListSerializer(TodoItem.serializer()), todos).toByteArray())
            writeEntryWithHash("todo_subtasks.json", json.encodeToString(ListSerializer(TodoSubtask.serializer()), subtasks).toByteArray())
        }

        // Backup attachments with deduplication
        if (includeAttachments && notes.isNotEmpty()) {
            val processedHashes = mutableSetOf<String>()
            val attachments = notes.flatMap { it.attachments }
            
            attachments.forEach { attachment ->
                try {
                    val attachmentUri = Uri.parse(attachment.uri)
                    context.contentResolver.openInputStream(attachmentUri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                        
                        val documentFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, attachmentUri)
                        val fileName = documentFile?.name ?: File(attachmentUri.path ?: "attachment").name
                        val ext = fileName.substringAfterLast('.', "")
                        val entryName = if (ext.isNotEmpty()) "attachments/$hash.$ext" else "attachments/$hash"
                        
                        // Map original URI to the ZIP entry name for later restoration (Fix 1)
                        attachmentUriMap[attachment.uri] = entryName

                        if (hash !in processedHashes) {
                            zos.putNextEntry(ZipEntry(entryName))
                            zos.write(bytes)
                            zos.closeEntry()
                            
                            manifest[entryName] = hash
                            processedHashes.add(hash)
                        }
                    } ?: run { missingAttachmentsCount++ }
                } catch (e: Exception) {
                    missingAttachmentsCount++
                    e.printStackTrace()
                }
            }
        }
        
        // Finalize manifest
        manifest["backup_timestamp"] = System.currentTimeMillis().toString()
        manifest["missing_attachments"] = missingAttachmentsCount.toString()
        manifest["is_incremental"] = (since > 0).toString()
        manifest["since_timestamp"] = since.toString()
        manifest["attachment_uri_map"] = json.encodeToString(attachmentUriMap) // Fix 1
        
        val manifestJson = json.encodeToString(manifest)
        zos.putNextEntry(ZipEntry("manifest.json"))
        zos.write(manifestJson.toByteArray())
        zos.closeEntry()
    }


    suspend fun scanBackupContent(uri: Uri): BackupScanResult {
        if (checkIsEncrypted(uri)) {
            // Can't scan encrypted content without password
            return BackupScanResult(emptyList(), 0, 0, 0)
        }
        var projects: List<Project> = emptyList()
        var labelsCount = 0
        var notesCount = 0
        var attachmentsCount = 0
        var timestamp: Long? = null

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    when (zipEntry.name) {
                        "projects.json" -> {
                            val jsonStr = InputStreamReader(zis).readText()
                            projects = json.decodeFromString(ListSerializer(Project.serializer()), jsonStr)
                        }
                        "labels.json" -> {
                            val jsonStr = InputStreamReader(zis).readText()
                            val labels = json.decodeFromString(ListSerializer(Label.serializer()), jsonStr)
                            labelsCount = labels.size
                        }
                        "notes.json" -> {
                            val jsonStr = InputStreamReader(zis).readText()
                            val notes = json.decodeFromString(ListSerializer(NoteWithAttachments.serializer()), jsonStr)
                            notesCount = notes.size
                            attachmentsCount = notes.sumOf { it.attachments.size }
                        }
                        "manifest.json" -> {
                            val jsonStr = InputStreamReader(zis).readText()
                            val manifest: Map<String, String> = json.decodeFromString(jsonStr)
                            timestamp = manifest["backup_timestamp"]?.toLongOrNull()
                        }
                    }
                    zipEntry = zis.nextEntry
                }
            }
        }
        return BackupScanResult(projects, labelsCount, notesCount, attachmentsCount, timestamp)
    }

    suspend fun restoreSelectedProjects(uri: Uri, selectedProjectIds: List<Int>) {
        val oldToNewProjectIds = mutableMapOf<Int, Int>()
        var notesJson: String? = null
        var projectsJson: String? = null
        var labelsJson: String? = null
        var manifestJson: String? = null
        var todosJson: String? = null
        var todoSubtasksJson: String? = null

        // Pass 1: Read JSON Data
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
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
            }
        }

        val manifest: Map<String, String> = manifestJson?.let { json.decodeFromString(it) } ?: emptyMap()
        val attachmentUriMap: Map<String, String> = manifest["attachment_uri_map"]?.let { json.decodeFromString(it) } ?: emptyMap()

        val attachmentsToExtract = mutableListOf<Pair<String, File>>() // ZipEntryName -> TargetFile

        repository.runInTransaction {
            // 1. Restore Labels (All)
            labelsJson?.let {
                val labels: List<Label> = json.decodeFromString(ListSerializer(Label.serializer()), it)
                labels.forEach { repository.insertLabel(it) }
            }

            // 2. Restore Selected Projects
            projectsJson?.let {
                val allProjects: List<Project> = json.decodeFromString(ListSerializer(Project.serializer()), it)
                val selectedProjects = allProjects.filter { project -> selectedProjectIds.contains(project.id) }
                
                selectedProjects.forEach { project ->
                    val oldId = project.id
                    val newId = repository.insertProject(project.copy(id = 0)).toInt()
                    oldToNewProjectIds[oldId] = newId
                }
            }

            // 3. Restore Notes & Prepare Attachment Extraction
            notesJson?.let {
                val notesWithAttachments: List<NoteWithAttachments> = json.decodeFromString(ListSerializer(NoteWithAttachments.serializer()), it)
                
                notesWithAttachments.forEach { noteWithAttachments ->
                    val oldProjectId = noteWithAttachments.note.projectId
                    // Only restore if the note belongs to a selected project
                    if (oldToNewProjectIds.containsKey(oldProjectId)) {
                        val newProjectId = oldToNewProjectIds[oldProjectId] ?: return@forEach
                        val newNote = noteWithAttachments.note.copy(id = 0, projectId = newProjectId)
                        val newNoteId = repository.insertNote(newNote).toInt()

                        // Restore Checklist Items (Bug C2 fix: partial restore flow was skipping these)
                        if (noteWithAttachments.checklistItems.isNotEmpty()) {
                            val remappedChecklist = noteWithAttachments.checklistItems.map { item ->
                                item.copy(
                                    id = java.util.UUID.randomUUID().toString(),
                                    noteId = newNoteId
                                )
                            }
                            repository.insertChecklistItems(remappedChecklist)
                        }

                        // Handle Attachments
                        noteWithAttachments.attachments.forEach { attachment ->
                            try {
                                val originalUri = Uri.parse(attachment.uri)
                                // Use the map from manifest to find the correct ZIP entry name (Fix 1)
                                val zipEntryName = attachmentUriMap[attachment.uri] ?: run {
                                    // Fallback to old behavior for backward compatibility with old backups (though they were broken)
                                    val fileName = File(originalUri.path ?: "unknown_${System.currentTimeMillis()}").name
                                    "attachments/$fileName"
                                }
                                
                                val fileName = File(originalUri.path ?: "attachment").name
                                // Create target file in internal storage
                                val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
                                val attachmentsDir = File(context.filesDir, "attachments")
                                if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
                                
                                val targetFile = File(attachmentsDir, uniqueFileName)

                                // Use FileProvider to avoid FileUriExposedException when
                                // the attachment URI is later shared across app boundaries.
                                val newUri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    targetFile
                                ).toString()
                                
                                val newAttachment = attachment.copy(id = 0, noteId = newNoteId, uri = newUri)
                                repository.insertAttachment(newAttachment)
                                
                                attachmentsToExtract.add(zipEntryName to targetFile)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            // 4. Restore Todos + Subtasks (Bug C1 fix). Todos without a projectId are
            // always restored; todos tied to a project are only restored when that
            // project was selected. Parent IDs are remapped to the new autogen IDs.
            val todos: List<TodoItem> = todosJson?.let {
                json.decodeFromString(ListSerializer(TodoItem.serializer()), it)
            } ?: emptyList()
            val subtasks: List<TodoSubtask> = todoSubtasksJson?.let {
                json.decodeFromString(ListSerializer(TodoSubtask.serializer()), it)
            } ?: emptyList()

            if (todos.isNotEmpty()) {
                val oldToNewTodoIds = mutableMapOf<Int, Int>()
                todos.forEach { todo ->
                    val oldProjectId = todo.projectId
                    val include = oldProjectId == null || oldToNewProjectIds.containsKey(oldProjectId)
                    if (!include) return@forEach

                    val remappedProjectId = oldProjectId?.let { oldToNewProjectIds[it] }
                    val newId = todoRepository.insertTodo(
                        todo.copy(id = 0, projectId = remappedProjectId)
                    ).toInt()
                    oldToNewTodoIds[todo.id] = newId
                }

                val remappedSubtasks = subtasks.mapNotNull { subtask ->
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

        // Pass 2: Extract Attachment Files
        if (attachmentsToExtract.isNotEmpty()) {
            extractAttachmentsFromZip(uri, attachmentsToExtract)
        }
    }

    suspend fun extractAttachmentsFromZip(uri: Uri, attachmentsToExtract: List<Pair<String, File>>) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    val entryName = zipEntry.name
                    // Find all targets that need this entry
                    val targets = attachmentsToExtract.filter { it.first == entryName }
                    targets.forEach { (_, targetFile) ->
                        try {
                            FileOutputStream(targetFile).use { fos ->
                                // Copy without closing the ZIS
                                val buffer = ByteArray(8192)
                                var length: Int
                                while (zis.read(buffer).also { length = it } > 0) {
                                    fos.write(buffer, 0, length)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    zipEntry = zis.nextEntry
                }
            }
        }
    }
}
