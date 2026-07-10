package com.suvojeet.notenext.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AutoDeleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val noteDao: NoteDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            
            // Clean up binned notes
            val days = settingsRepository.autoDeleteDays.first()
            val threshold = now - (days * 24 * 60 * 60 * 1000L)
            noteDao.deleteBinnedNotesOlderThan(threshold)
            
            // Clean up expired notes (self-destruct)
            noteDao.deleteExpiredNotes(now)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
