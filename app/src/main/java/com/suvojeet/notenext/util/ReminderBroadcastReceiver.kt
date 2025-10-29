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

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val noteId = intent?.getIntExtra("NOTE_ID", -1) ?: -1
            val noteTitle = intent?.getStringExtra("NOTE_TITLE") ?: "Reminder"
            val noteContent = intent?.getStringExtra("NOTE_CONTENT") ?: ""

            createNotificationChannel(context)
            showNotification(context, noteId, noteTitle, noteContent)
            playAlarmSound(context)
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
            .setSmallIcon(R.drawable.ic_notifications_black_24dp) // TODO: Replace with actual notification icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(noteId, builder.build()) // Use noteId as notification ID
        }
    }

    private fun playAlarmSound(context: Context) {
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val r = RingtoneManager.getRingtone(context, alarmSound)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
