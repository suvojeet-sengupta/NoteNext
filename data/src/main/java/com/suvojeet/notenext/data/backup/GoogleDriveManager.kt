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

    suspend fun uploadBackup(context: Context, account: GoogleSignInAccount, dbFile: File): String = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)

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
            driveService.files().update(fileId, null, mediaContent).execute()
        } else {
            // Create new file
            val file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            fileId = file.id
        }
        return@withContext fileId
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

    suspend fun checkForBackup(context: Context, account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        val query = "name = 'notenext_backup.zip' and 'appDataFolder' in parents and trashed = false"
        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id)")
            .execute()
        return@withContext fileList.files.isNotEmpty()
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
