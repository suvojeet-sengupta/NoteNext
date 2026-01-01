package com.suvojeet.notenext.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.suvojeet.notenext.data.AlarmScheduler
import com.suvojeet.notenext.data.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: NoteRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                val notes = repository.getAllReminders().first()
                val now = System.currentTimeMillis()
                notes.forEach { note ->
                    // Only schedule future reminders or repeating reminders
                    if ((note.reminderTime ?: 0L) > now) {
                        alarmScheduler.schedule(note)
                    }
                }
            }
        }
    }
}
