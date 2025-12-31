package com.suvojeet.notenext.data.backup

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveManager @Inject constructor() {

    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
        .setApplicationName("NoteNext")
        .build()
    }

    private fun getDriveService(context: Context, email: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccountName = email

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
        .setApplicationName("NoteNext")
        .build()
    }

    suspend fun uploadBackup(context: Context, account: GoogleSignInAccount, dbFile: File, onProgress: ((Long, Long) -> Unit)? = null): String = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        uploadToDrive(driveService, dbFile, onProgress)
    }

    suspend fun uploadBackup(context: Context, email: String, dbFile: File): String = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, email)
        uploadToDrive(driveService, dbFile)
    }

    private fun uploadToDrive(driveService: Drive, dbFile: File, onProgress: ((Long, Long) -> Unit)? = null): String {
        // Filename: notenext_backup_yyyyMMdd_HHmmss.zip
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val fileName = "notenext_backup_$timeStamp.zip"

        val fileMetadata = com.google.api.services.drive.model.File()
        fileMetadata.name = fileName
        fileMetadata.parents = listOf("appDataFolder")

        val mediaContent = FileContent("application/zip", dbFile)

        // Create new file (Always new file for versioning)
        val createRequest = driveService.files().create(fileMetadata, mediaContent)
            .setFields("id")
        
        createRequest.mediaHttpUploader.isDirectUploadEnabled = false
        createRequest.mediaHttpUploader.chunkSize = 1 * 1024 * 1024

        if (onProgress != null) {
            createRequest.mediaHttpUploader.setProgressListener { uploader ->
                    when (uploader.uploadState) {
                        com.google.api.client.googleapis.media.MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                            onProgress(uploader.numBytesUploaded, dbFile.length())
                        }
                        com.google.api.client.googleapis.media.MediaHttpUploader.UploadState.MEDIA_COMPLETE -> {
                            onProgress(dbFile.length(), dbFile.length())
                        }
                        else -> {}
                    }
            }
        }
        
        val file = createRequest.execute()
        
        // Prune old backups
        pruneOldBackups(driveService)
        
        return file.id
    }

    private fun pruneOldBackups(driveService: Drive, maxBackups: Int = 7) {
        try {
            val query = "name contains 'notenext_backup_' and 'appDataFolder' in parents and trashed = false"
            val fileList = driveService.files().list()
                .setQ(query)
                .setSpaces("appDataFolder")
                .setFields("files(id, name, createdTime)")
                .execute()

            if (fileList.files.size > maxBackups) {
                // Sort by createdTime ascending (oldest first)
                val sortedFiles = fileList.files.sortedBy { it.createdTime.value }
                val filesToDelete = sortedFiles.take(fileList.files.size - maxBackups)

                filesToDelete.forEach { file ->
                    try {
                        driveService.files().delete(file.id).execute()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getBackups(context: Context, account: GoogleSignInAccount): List<DriveBackupMetadata> = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        val query = "name contains 'notenext_backup_' and 'appDataFolder' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id, name, size, createdTime)")
            .execute()

        fileList.files.map { file ->
            DriveBackupMetadata(
                id = file.id,
                name = file.name,
                size = file.getSize() ?: 0L,
                modifiedTime = file.createdTime  // Use createdTime as essential timestamp
            )
        }.sortedByDescending { it.modifiedTime?.value ?: 0L }
    }

     suspend fun deleteBackupFile(context: Context, account: GoogleSignInAccount, fileId: String) = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        driveService.files().delete(fileId).execute()
    }

    suspend fun downloadBackup(context: Context, account: GoogleSignInAccount, targetFile: File, fileId: String? = null) = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)

        val targetFileId = if (fileId != null) {
            fileId
        } else {
             // Find latest
            val query = "name contains 'notenext_backup_' and 'appDataFolder' in parents and trashed = false"
            val fileList = driveService.files().list()
                .setQ(query)
                .setSpaces("appDataFolder")
                .setFields("files(id, createdTime)")
                .execute()
            
             val latest = fileList.files.maxByOrNull { it.createdTime.value }
             latest?.id ?: run {
                 // Fallback to legacy
                 val legacyQuery = "name = 'notenext_backup.zip' and 'appDataFolder' in parents and trashed = false"
                 val legacyList = driveService.files().list()
                    .setQ(legacyQuery)
                    .setSpaces("appDataFolder")
                    .setFields("files(id)")
                    .execute()
                 if(legacyList.files.isEmpty()) throw Exception("No backup found")
                 legacyList.files[0].id
             }
        }

        val outputStream = FileOutputStream(targetFile)
        driveService.files().get(targetFileId).executeMediaAndDownloadTo(outputStream)
        outputStream.close()
    }

    data class DriveBackupMetadata(
        val id: String,
        val name: String,
        val size: Long,
        val modifiedTime: com.google.api.client.util.DateTime?
    )

    suspend fun getBackupMetadata(context: Context, account: GoogleSignInAccount): DriveBackupMetadata? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        val query = "name contains 'notenext_backup_' and 'appDataFolder' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id, name, size, createdTime)")
            .execute()
        
        // Find latest by createdTime
        val latestFile = fileList.files.maxByOrNull { it.createdTime.value }
        
        if (latestFile != null) {
            DriveBackupMetadata(
                id = latestFile.id,
                name = latestFile.name,
                size = latestFile.getSize() ?: 0L,
                modifiedTime = latestFile.createdTime
            )
        } else {
             // Fallback check for legacy non-timestamped file
             val legacyQuery = "name = 'notenext_backup.zip' and 'appDataFolder' in parents and trashed = false"
             val legacyList = driveService.files().list()
                .setQ(legacyQuery)
                .setSpaces("appDataFolder")
                .setFields("files(id, name, size, modifiedTime)")
                .execute()
             
             if(legacyList.files.isNotEmpty()) {
                 val file = legacyList.files[0]
                 DriveBackupMetadata(
                    id = file.id,
                    name = file.name,
                    size = file.getSize() ?: 0L,
                    modifiedTime = file.modifiedTime
                 )
             } else {
                 null
             }
        }
    }

    suspend fun checkForBackup(context: Context, account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        try {
             getBackupMetadata(context, account) != null
        } catch(e: Exception) {
            false
        }
    }

    suspend fun deleteBackup(context: Context, account: GoogleSignInAccount) = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        // Delete ALL backups
        val query = "name contains 'notenext_backup' and 'appDataFolder' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id)")
            .execute()

        fileList.files.forEach { file ->
             try {
                driveService.files().delete(file.id).execute()
             } catch(e: Exception) {
                 e.printStackTrace()
             }
        }
    }
}
