package com.suvojeet.notenext.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.NoteWithAttachments
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.google.android.gms.auth.api.signin.GoogleSignInAccount


@Singleton
class BackupRepository @Inject constructor(
    private val repository: NoteRepository,
    @ApplicationContext private val context: Context,
    private val googleDriveManager: GoogleDriveManager
) {

    suspend fun createBackupZip(targetFile: File) {
        FileOutputStream(targetFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                writeBackupToZip(zos)
            }
        }
    }
    
    suspend fun createBackupZip(outputStream: java.io.OutputStream) {
         ZipOutputStream(outputStream).use { zos ->
            writeBackupToZip(zos)
        }
    }

    suspend fun backupToDrive(account: GoogleSignInAccount, onProgress: ((Long, Long) -> Unit)? = null): String {
        val dbFile = File(context.cacheDir, "temp_backup.zip")
        createBackupZip(dbFile)
        return try {
            googleDriveManager.uploadBackup(context, account, dbFile, onProgress)
        } finally {
            if (dbFile.exists()) {
                dbFile.delete()
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
                context.contentResolver.openInputStream(attachmentUri)?.use { inputStream ->
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


    suspend fun readProjectsFromZip(uri: Uri): List<Project> {
        var projects: List<Project> = emptyList()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    if (zipEntry.name == "projects.json") {
                        val projectsJson = InputStreamReader(zis).readText()
                        val projectsType = object : TypeToken<List<Project>>() {}.type
                        projects = Gson().fromJson(projectsJson, projectsType)
                        break
                    }
                    zipEntry = zis.nextEntry
                }
            }
        }
        return projects
    }

    suspend fun restoreSelectedProjects(uri: Uri, selectedProjectIds: List<Int>) {
        val oldToNewProjectIds = mutableMapOf<Int, Int>()
        var notesJson: String? = null
        var projectsJson: String? = null
        var labelsJson: String? = null 

        // Pass 1: Read JSON Data
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                   when {
                        zipEntry.name == "notes.json" -> notesJson = InputStreamReader(zis).readText()
                        zipEntry.name == "labels.json" -> labelsJson = InputStreamReader(zis).readText()
                        zipEntry.name == "projects.json" -> projectsJson = InputStreamReader(zis).readText()
                    }
                    zipEntry = zis.nextEntry
                }
            }
        }

        // 1. Restore Labels (All)
        labelsJson?.let {
            val labelsType = object : TypeToken<List<Label>>() {}.type
            val labels: List<Label> = Gson().fromJson(it, labelsType)
            labels.forEach { repository.insertLabel(it) }
        }

        // 2. Restore Selected Projects
        projectsJson?.let {
            val projectsType = object : TypeToken<List<Project>>() {}.type
            val allProjects: List<Project> = Gson().fromJson(it, projectsType)
            val selectedProjects = allProjects.filter { project -> selectedProjectIds.contains(project.id) }
            
            selectedProjects.forEach { project ->
                val oldId = project.id
                val newId = repository.insertProject(project.copy(id = 0)).toInt()
                oldToNewProjectIds[oldId] = newId
            }
        }

        // 3. Restore Notes & Prepare Attachment Extraction
        val attachmentsToExtract = mutableListOf<Pair<String, File>>() // ZipEntryName -> TargetFile

        notesJson?.let {
            val notesType = object : TypeToken<List<NoteWithAttachments>>() {}.type
            val notesWithAttachments: List<NoteWithAttachments> = Gson().fromJson(it, notesType)
            
            notesWithAttachments.forEach { noteWithAttachments ->
                val oldProjectId = noteWithAttachments.note.projectId
                // Only restore if the note belongs to a selected project
                if (oldToNewProjectIds.containsKey(oldProjectId)) {
                    val newProjectId = oldToNewProjectIds[oldProjectId]!!
                    val newNote = noteWithAttachments.note.copy(id = 0, projectId = newProjectId)
                    val newNoteId = repository.insertNote(newNote).toInt()

                    // Handle Attachments
                    noteWithAttachments.attachments.forEach { attachment ->
                        try {
                            val originalUri = Uri.parse(attachment.uri)
                            // We assume the filename in the zip matches the original filename
                            val fileName = File(originalUri.path ?: "unknown_${System.currentTimeMillis()}").name
                            val zipEntryName = "attachments/$fileName"
                            
                            // Create target file in internal storage
                            // Use a timestamp to avoid overwriting existing files with same name but different content
                            val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
                            val attachmentsDir = File(context.filesDir, "attachments")
                            if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
                            
                            val targetFile = File(attachmentsDir, uniqueFileName)
                            
                            val newUri = Uri.fromFile(targetFile).toString()
                            
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

        // Pass 2: Extract Attachment Files
        if (attachmentsToExtract.isNotEmpty()) {
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
}
