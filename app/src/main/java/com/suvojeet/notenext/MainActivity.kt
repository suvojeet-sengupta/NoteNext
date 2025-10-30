
package com.suvojeet.notenext

import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.app.AlarmManager
import android.os.Build

import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.suvojeet.notenext.data.NoteDatabase
import com.suvojeet.notenext.dependency_injection.ViewModelFactory
import com.suvojeet.notenext.navigation.NavGraph
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.ui.settings.ThemeMode
import com.suvojeet.notenext.ui.theme.NoteNextTheme
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.ui.theme.ShapeFamily
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.suvojeet.notenext.ui.lock.LockScreen
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = NoteDatabase.getDatabase(this)
        val linkPreviewRepository = LinkPreviewRepository()
        val factory = ViewModelFactory(database.noteDao(), database.labelDao(), database.projectDao(), linkPreviewRepository, applicationContext)
        val settingsRepository = SettingsRepository(this)

                requestExactAlarmPermission()

        

                val startNoteId = intent.getIntExtra("NOTE_ID", -1)

        

                setContent {

                    val windowSizeClass = calculateWindowSizeClass(this)

                    val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

                    val shapeFamily by settingsRepository.shapeFamily.collectAsState(initial = ShapeFamily.EXPRESSIVE)

                    val enableAppLock by settingsRepository.enableAppLock.collectAsState(initial = null)

                    var unlocked by remember { mutableStateOf(false) }

        

                    NoteNextTheme(themeMode = themeMode, shapeFamily = shapeFamily) {

                        val appLock = enableAppLock

                        if (appLock == null) {

                            // You can show a loading indicator here

                        } else if (appLock && !unlocked) {

                            LockScreen(onUnlock = { unlocked = true })

                        } else {

                            NavGraph(factory = factory, themeMode = themeMode, windowSizeClass = windowSizeClass, startNoteId = startNoteId)

                        }

                    }

                }

            }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31) and above
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                    startActivity(it)
                }
            }
        }
    }
}
