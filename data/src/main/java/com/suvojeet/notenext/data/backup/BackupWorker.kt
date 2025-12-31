package com.suvojeet.notenext.data.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val googleDriveManager: GoogleDriveManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val email = inputData.getString("email")
        
        val sharedPrefs = applicationContext.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        val isSdCardBackupEnabled = sharedPrefs.getBoolean("sd_card_backup_enabled", false)
        val sdCardFolderUri = sharedPrefs.getString("sd_card_folder_uri", null)
        val includeAttachments = sharedPrefs.getBoolean("include_backup_attachments", true)

        if (email == null && !isSdCardBackupEnabled) {
            return@withContext Result.failure()
        }

        setForeground(createForegroundInfo())

        var success = true
        var errorMessage = ""

        try {
            // 1. Google Drive Backup
            if (email != null) {
                try {
                    val tempFile = File(applicationContext.cacheDir, "auto_backup.zip")
                    backupRepository.createBackupZip(tempFile, includeAttachments) // Respect content selection
                    googleDriveManager.uploadBackup(applicationContext, email, tempFile)
                    tempFile.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                    success = false
                    errorMessage = e.message ?: "Drive backup failed"
                }
            }

            // 2. SD Card Backup
            if (isSdCardBackupEnabled && sdCardFolderUri != null) {
                 try {
                     backupRepository.backupToUri(android.net.Uri.parse(sdCardFolderUri), includeAttachments)
                 } catch (e: Exception) {
                     e.printStackTrace()
                     success = false
                     errorMessage = e.message ?: "SD Card backup failed"
                 }
            }
            
            showCompletionNotification(success, errorMessage)

            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            showCompletionNotification(false, e.message ?: "Unknown error")
            Result.retry()
        }
    }

    private fun showCompletionNotification(success: Boolean, message: String) {
        val notificationId = 2 // Different ID from foreground service
        val channelId = "backup_status_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Backup Status", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (success) "Backup Successful" else "Backup Failed"
        val text = if (success) "Your NoteNext data is safe." else message
        val icon = if (success) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_notify_error

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(notificationId, notification)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notificationId = 1
        val channelId = "backup_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Backup Service", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Backing up NoteNext")
            .setContentText("Uploading data to Google Drive...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()
            
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}
