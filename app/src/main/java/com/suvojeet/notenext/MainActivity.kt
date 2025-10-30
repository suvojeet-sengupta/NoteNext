
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.suvojeet.notenext.ui.setup.SetupScreen

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = NoteDatabase.getDatabase(this)
        val linkPreviewRepository = LinkPreviewRepository()
        val factory = ViewModelFactory(database.noteDao(), database.labelDao(), database.projectDao(), linkPreviewRepository, application)
        val settingsRepository = SettingsRepository(this)

        val startNoteId = intent.getIntExtra("NOTE_ID", -1)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val shapeFamily by settingsRepository.shapeFamily.collectAsState(initial = ShapeFamily.EXPRESSIVE)

            var enableAppLockLoaded by remember { mutableStateOf<Boolean?>(null) }
            var isSetupCompleteLoaded by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                settingsRepository.enableAppLock.collect { enableAppLockLoaded = it }
            }
            LaunchedEffect(Unit) {
                settingsRepository.isSetupComplete.collect { isSetupCompleteLoaded = it }
            }

            var unlocked by remember { mutableStateOf(false) }

            NoteNextTheme(themeMode = themeMode, shapeFamily = shapeFamily) {
                if (enableAppLockLoaded == null || isSetupCompleteLoaded == null) {
                    // Show a blank screen or splash screen while settings are loading
                    Surface(modifier = Modifier.fillMaxSize()) {}
                } else if (!isSetupCompleteLoaded) {
                    SetupScreen(factory = factory) { /* Setup is complete, UI will recompose based on isSetupComplete */ }
                } else if (enableAppLockLoaded!! && !unlocked) {
                    LockScreen(onUnlock = { unlocked = true })
                } else {
                    NavGraph(factory = factory, themeMode = themeMode, windowSizeClass = windowSizeClass, startNoteId = startNoteId)
                }
            }
        }
    }
}
