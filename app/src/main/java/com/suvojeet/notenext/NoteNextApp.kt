package com.suvojeet.notenext

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.suvojeet.notenext.worker.AutoDeleteWorker
import dagger.hilt.android.HiltAndroidApp
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.ToastConfigurationBuilder
import org.acra.config.NotificationConfigurationBuilder
import org.acra.data.StringFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class NoteNextApp : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        
        val builder = CoreConfigurationBuilder()
            .withBuildConfigClass(BuildConfig::class.java)
            .withReportFormat(StringFormat.JSON)
            .withPluginConfigurations(
                ToastConfigurationBuilder()
                    .withText(getString(R.string.crash_toast_text))
                    .withEnabled(true)
                    .build(),
                NotificationConfigurationBuilder()
                    .withTitle(getString(R.string.crash_report_title))
                    .withText(getString(R.string.crash_report_text))
                    .withChannelName(getString(R.string.crash_report_channel))
                    .withEnabled(true)
                    .build()
            )

        ACRA.init(this, builder)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            createNotificationChannels()
            setupAutoDeleteWorker()
        }
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "logging_service_channel",
                "Logging Service Channel",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for continuous log collection for bug reproduction"
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupAutoDeleteWorker() {
        // No constraints — self-destruct must happen even on low battery. The DB write
        // is cheap; holding it back to spare 0.0001% of the battery is the wrong trade.
        // Per-note exact alarms (AlarmScheduler.scheduleExpiry) handle the precise case;
        // this worker is the safety-net sweeper for missed alarms (e.g. force-stop,
        // permission revoked, alarm dropped under Doze).
        val oneTimeRequest = OneTimeWorkRequestBuilder<AutoDeleteWorker>().build()
        WorkManager.getInstance(this).enqueue(oneTimeRequest)

        // Periodic every 15 minutes (the WorkManager minimum). Down from 1 hour: a
        // 5-minute self-destruct timer was previously up to ~1 h late.
        val cleanupRequest = PeriodicWorkRequestBuilder<AutoDeleteWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AutoDeleteBinNotes",
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest
        )
    }
}
