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
        // 1. Search for existing backup folder/file
        val fileMetadata = com.google.api.services.drive.model.File()
        fileMetadata.name = "notenext_backup.zip"
        fileMetadata.parents = listOf("appDataFolder")

        val mediaContent = FileContent("application/zip", dbFile)

        // Check if file exists
        val query = "name = 'notenext_backup.zip' and 'appDataFolder' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id)")
            .execute()

        val fileId: String
        if (fileList.files.isNotEmpty()) {
            // Update existing file
            fileId = fileList.files[0].id
            val updateRequest = driveService.files().update(fileId, null, mediaContent)
            updateRequest.mediaHttpUploader.isDirectUploadEnabled = false
            updateRequest.mediaHttpUploader.chunkSize = 1 * 1024 * 1024
            
            if (onProgress != null) {
                updateRequest.mediaHttpUploader.setProgressListener { uploader ->
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
            updateRequest.execute()
        } else {
            // Create new file
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
            fileId = file.id
        }
        return fileId
    }

    suspend fun downloadBackup(context: Context, account: GoogleSignInAccount, targetFile: File) = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)

        val query = "name = 'notenext_backup.zip' and 'appDataFolder' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id)")
            .execute()

        if (fileList.files.isEmpty()) {
            throw Exception("Backup not found in Drive")
        }

        val fileId = fileList.files[0].id
        val outputStream = FileOutputStream(targetFile)
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.close()
    }

    data class DriveBackupMetadata(
        val size: Long,
        val modifiedTime: com.google.api.client.util.DateTime?
    )

    suspend fun getBackupMetadata(context: Context, account: GoogleSignInAccount): DriveBackupMetadata? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        val query = "name = 'notenext_backup.zip' and 'appDataFolder' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id, size, modifiedTime)")
            .execute()
        
        if (fileList.files.isNotEmpty()) {
            val file = fileList.files[0]
            DriveBackupMetadata(
                size = file.getSize() ?: 0L,
                modifiedTime = file.modifiedTime
            )
        } else {
            null
        }
    }

    suspend fun checkForBackup(context: Context, account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        // Keep existing method for simple checks, or could deprecate.
        // For now, let's just reuse logic or re-query. Re-query is simpler.
        try {
             getBackupMetadata(context, account) != null
        } catch(e: Exception) {
            false
        }
    }
    suspend fun deleteBackup(context: Context, account: GoogleSignInAccount) = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        val query = "name = 'notenext_backup.zip' and 'appDataFolder' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id)")
            .execute()

        if (fileList.files.isNotEmpty()) {
            val fileId = fileList.files[0].id
            driveService.files().delete(fileId).execute()
        }
    }
}
