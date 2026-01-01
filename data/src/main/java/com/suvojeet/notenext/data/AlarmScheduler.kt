package com.suvojeet.notenext.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.suvojeet.notenext.data.RepeatOption
import java.util.Calendar

interface AlarmScheduler {
    fun schedule(note: Note)
    fun cancel(note: Note)
}

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(note: Note) {
        val reminderTime = note.reminderTime ?: return
        if (reminderTime <= System.currentTimeMillis()) return

        val intent = Intent().apply {
            component = ComponentName(context, "com.suvojeet.notenext.util.ReminderBroadcastReceiver")
            putExtra("NOTE_ID", note.id)
            putExtra("NOTE_TITLE", note.title)
            putExtra("NOTE_CONTENT", note.content)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Always use exact alarm for precision
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            } else {
                // Fallback or just try anyway (will crash if permission revoked, but we handle permission in UI now)
                try {
                     alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        } else {
             alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }

    override fun cancel(note: Note) {
        val intent = Intent().apply {
            component = ComponentName(context, "com.suvojeet.notenext.util.ReminderBroadcastReceiver")
            putExtra("NOTE_ID", note.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
