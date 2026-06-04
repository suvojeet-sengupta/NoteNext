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
        val isIncrementalEnabled = sharedPrefs.getBoolean("incremental_backup_enabled", false)
        val lastBackupTime = if (isIncrementalEnabled) sharedPrefs.getLong("last_backup_time", 0L) else 0L
        
        val isEncryptionEnabled = sharedPrefs.getBoolean("backup_encryption_enabled", false) || sharedPrefs.getBoolean("auto_backup_encryption_enabled", false)
        val encryptionPassword = SecurityUtils.getBackupPassword(applicationContext)
        
        val backupPassword = if (isEncryptionEnabled && !encryptionPassword.isNullOrBlank()) encryptionPassword else null

        if (email == null && !isSdCardBackupEnabled) {
            return@withContext Result.failure()
        }

        // On Android 12+ a foreground-service start can be disallowed (background start
        // limits) and on 13+ the notification can be blocked. Neither should abort the
        // backup itself — degrade to a normal background worker if promotion fails.
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var success = true
        var errorMessage = ""
        val startTime = System.currentTimeMillis()

        try {
            // 1. Google Drive Backup
            if (email != null) {
                try {
                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(applicationContext)
                    if (account != null) {
                         backupRepository.backupToDrive(
                             account = account,
                             password = backupPassword,
                             includeAttachments = includeAttachments,
                             since = lastBackupTime
                         )
                    } else {
                         throw Exception("Google Account not signed in")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    success = false
                    errorMessage = e.message ?: "Drive backup failed"
                }
            }

            // 2. SD Card Backup
            if (isSdCardBackupEnabled && sdCardFolderUri != null) {
                 try {
                     if (backupPassword != null) {
                         backupRepository.backupToEncryptedFolder(android.net.Uri.parse(sdCardFolderUri), backupPassword, includeAttachments, since = lastBackupTime)
                     } else {
                         backupRepository.backupToUri(android.net.Uri.parse(sdCardFolderUri), includeAttachments, since = lastBackupTime)
                     }
                 } catch (e: Exception) {
                     e.printStackTrace()
                     success = false
                     errorMessage = e.message ?: "SD Card backup failed"
                 }
            }
            
            if (success) {
                sharedPrefs.edit()
                    .putLong("last_backup_time", startTime)
                    .putInt("edit_counter", 0) // Reset edit counter on success
                    .apply()
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

        // POST_NOTIFICATIONS may be denied on Android 13+; don't let that crash the worker.
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
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
