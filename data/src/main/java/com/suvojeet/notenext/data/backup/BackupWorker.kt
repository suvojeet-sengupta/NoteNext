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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val googleDriveManager: GoogleDriveManager,
    private val backupSettingsRepository: com.suvojeet.notenext.data.repository.BackupSettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val email = inputData.getString("email")

        val isSdCardBackupEnabled = backupSettingsRepository.sdCardEnabled.first()
        val sdCardFolderUri = backupSettingsRepository.backupLocationUri.first()
        val includeAttachments = backupSettingsRepository.includeAttachments.first()
        val isIncrementalEnabled = backupSettingsRepository.incrementalEnabled.first()
        val lastBackupTime = if (isIncrementalEnabled) backupSettingsRepository.lastBackupTime.first() else 0L

        val isEncryptionEnabled = backupSettingsRepository.encryptionEnabled.first()
        val encryptionPassword = SecurityUtils.getBackupPassword(applicationContext)
        val backupPassword = if (isEncryptionEnabled && !encryptionPassword.isNullOrBlank()) encryptionPassword else null

        if (email == null && !isSdCardBackupEnabled) {
            return@withContext Result.failure()
        }

        setForeground(createForegroundInfo())

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
                backupSettingsRepository.setLastBackupTime(startTime)
                backupSettingsRepository.setEditCounter(0)
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
