package com.suvojeet.notenext.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suvojeet.notenext.MainActivity
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.AlarmScheduler
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.RepeatOption
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: NoteRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context?, intent: Intent?) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {
                if (context != null) {
                    val noteId = intent?.getIntExtra("NOTE_ID", -1) ?: -1
                    val noteTitle = intent?.getStringExtra("NOTE_TITLE") ?: "Reminder"
                    val noteContent = intent?.getStringExtra("NOTE_CONTENT") ?: ""

                    // Show Notification
                    val plainTextContent = HtmlConverter.htmlToPlainText(noteContent)
                    val truncatedContent = if (plainTextContent.length > 150) {
                        plainTextContent.substring(0, 150) + "..."
                    } else {
                        plainTextContent
                    }

                    createNotificationChannel(context)
                    showNotification(context, noteId, noteTitle, truncatedContent)

                    // Handle Repeat Logic
                    if (noteId != -1) {
                        val noteWithAttachments = repository.getNoteById(noteId)
                        noteWithAttachments?.note?.let { note ->
                            val repeatOptionStr = note.repeatOption
                            if (repeatOptionStr != null && repeatOptionStr != RepeatOption.NEVER.name) {
                                try {
                                    val repeatOption = RepeatOption.valueOf(repeatOptionStr)
                                    val nextTime = calculateNextReminderTime(note.reminderTime ?: System.currentTimeMillis(), repeatOption)
                                    
                                    if (nextTime > System.currentTimeMillis()) {
                                        val updatedNote = note.copy(reminderTime = nextTime)
                                        repository.updateNote(updatedNote)
                                        alarmScheduler.schedule(updatedNote)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun calculateNextReminderTime(currentMillis: Long, repeatOption: RepeatOption): Long {
        val currentDateTime = Instant.ofEpochMilli(currentMillis).atZone(ZoneId.systemDefault())
        
        val nextDateTime = when (repeatOption) {
            RepeatOption.DAILY -> currentDateTime.plusDays(1)
            RepeatOption.WEEKLY -> currentDateTime.plusWeeks(1)
            RepeatOption.MONTHLY -> currentDateTime.plusMonths(1)
            RepeatOption.YEARLY -> currentDateTime.plusYears(1)
            RepeatOption.NEVER -> currentDateTime
        }
        
        return nextDateTime.toInstant().toEpochMilli()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminder Channel"
            val descriptionText = "Channel for Note Reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("reminder_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, noteId: Int, title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTE_ID", noteId)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, noteId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "reminder_channel_id")
            .setSmallIcon(R.mipmap.ic_launcher) // Try using app icon or fallback
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && notificationManager.canUseFullScreenIntent()) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        with(NotificationManagerCompat.from(context)) {
            // Check permission for Android 13+
            try {
                 notify(noteId, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }
}
