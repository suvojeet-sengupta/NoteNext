package com.suvojeet.notenext.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.suvojeet.notenext.data.ReminderScheduler
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
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val now = System.currentTimeMillis()

                    // Re-arm reminder alarms.
                    val reminderNotes = repository.getAllReminders().first()
                    reminderNotes.forEach { note ->
                        if ((note.reminderTime ?: 0L) > now) {
                            reminderScheduler.scheduleNoteReminder(note)
                        }
                    }

                    // Re-arm self-destruct alarms. Without this, an expiry set before
                    // a reboot would only fire whenever the next periodic
                    // AutoDeleteWorker tick happened (up to ~1h late). With it, even
                    // expiries set "5 minutes from now" survive a reboot precisely.
                    val allNotes = repository.getAllNotes().first()
                    allNotes.forEach { withAttachments ->
                        val note = withAttachments.note
                        if (note.expiryTime != null && !note.isBinned) {
                            reminderScheduler.scheduleNoteExpiry(note)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
