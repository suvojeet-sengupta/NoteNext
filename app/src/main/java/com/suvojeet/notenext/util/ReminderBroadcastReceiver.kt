package com.suvojeet.notenext.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suvojeet.notenext.MainActivity
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.notes.HtmlConverter

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val noteId = intent?.getIntExtra("NOTE_ID", -1) ?: -1
            val noteTitle = intent?.getStringExtra("NOTE_TITLE") ?: "Reminder"
            val noteContent = intent?.getStringExtra("NOTE_CONTENT") ?: ""

            val plainTextContent = HtmlConverter.htmlToPlainText(noteContent)
            val truncatedContent = if (plainTextContent.length > 150) {
                plainTextContent.substring(0, 150) + "..."
            } else {
                plainTextContent
            }

            createNotificationChannel(context)
            showNotification(context, noteId, noteTitle, truncatedContent)
        }
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
            putExtra("NOTE_ID", noteId) // Pass note ID to open the specific note
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, noteId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "reminder_channel_id")
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Replaced with generic alert icon
            .setContentTitle(title)
            .setContentText(HtmlConverter.htmlToPlainText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // For Android 14 (API 34) and above, USE_FULL_SCREEN_INTENT might not be pre-granted.
        // Check if the app can use full-screen intent before setting it.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && notificationManager.canUseFullScreenIntent()) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(noteId, builder.build()) // Use noteId as notification ID
        }
    }


}
