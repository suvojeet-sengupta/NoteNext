
package com.example.notenext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.notenext.data.NoteDatabase
import com.example.notenext.dependency_injection.ViewModelFactory
import com.example.notenext.navigation.NavGraph
import com.example.notenext.ui.settings.SettingsRepository
import com.example.notenext.ui.settings.ThemeMode
import com.example.notenext.ui.theme.NoteNextTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = NoteDatabase.getDatabase(this)
        val factory = ViewModelFactory(database.noteDao())
        val settingsRepository = SettingsRepository(this)

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            NoteNextTheme(themeMode = themeMode) {
                NavGraph(factory = factory, themeMode = themeMode)
            }
        }
    }
}
