package com.suvojeet.notenext.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.ui.reminder.RepeatOption
import java.util.Calendar

interface AlarmScheduler {
    fun schedule(note: Note)
    fun cancel(note: Note)
}

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(note: Note) {
        note.reminderTime?.let { reminderTimeMillis ->
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
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

            val calendar = Calendar.getInstance().apply {
                timeInMillis = reminderTimeMillis
            }

            when (RepeatOption.valueOf(note.repeatOption ?: RepeatOption.NEVER.name)) {
                RepeatOption.NEVER -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
                RepeatOption.DAILY -> {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
                }
                RepeatOption.WEEKLY -> {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY * 7, pendingIntent)
                }
                RepeatOption.MONTHLY -> {
                    // For monthly, we need to calculate the next month's date manually
                    // This is a simplified approach, a more robust solution would involve a WorkManager or more complex AlarmManager logic
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY * 30, pendingIntent)
                }
                RepeatOption.YEARLY -> {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY * 365, pendingIntent)
                }
            }
        }
    }

    override fun cancel(note: Note) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
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
