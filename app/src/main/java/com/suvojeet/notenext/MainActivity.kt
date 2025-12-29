
package com.suvojeet.notenext

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.suvojeet.notenext.navigation.NavGraph
import com.suvojeet.notenext.ui.theme.NoteNextTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.ui.theme.ShapeFamily
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.suvojeet.notenext.ui.lock.LockScreen
import androidx.compose.runtime.LaunchedEffect
import com.suvojeet.notenext.ui.setup.SetupScreen
import java.util.Locale
import android.content.res.Configuration
import android.content.Intent

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(this)

        val startNoteId = intent.getIntExtra("NOTE_ID", -1)
        val startAddNote = intent.getBooleanExtra("START_ADD_NOTE", false)
        val sharedText = when {
            intent.action == Intent.ACTION_SEND && "text/plain" == intent.type -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null

        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.disallowScreenshots.collect { disallow ->
                    if (disallow) {
                        window.setFlags(
                            android.view.WindowManager.LayoutParams.FLAG_SECURE,
                            android.view.WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        setContent {
            val languageCode by settingsRepository.language.collectAsState(initial = "en")
            LaunchedEffect(languageCode) {
                val locale = Locale(languageCode)
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration)
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
            }



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
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (enableAppLockLoaded == null || isSetupCompleteLoaded == null) {
                        // Show a blank screen or splash screen while settings are loading
                        Surface(modifier = Modifier.fillMaxSize()) {}
                    } else if (isSetupCompleteLoaded == false) {
                        SetupScreen { /* Setup is complete, UI will recompose based on isSetupComplete */ }
                    } else if (enableAppLockLoaded!! && !unlocked) {
                        LockScreen(onUnlock = { unlocked = true })
                    } else {
                        NavGraph(themeMode = themeMode, windowSizeClass = windowSizeClass, startNoteId = startNoteId, startAddNote = startAddNote, sharedText = sharedText)
                    }
                }
            }
        }
    }
}
