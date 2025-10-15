
package com.example.notesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.notesapp.data.NoteDatabase
import com.example.notesapp.dependency_injection.ViewModelFactory
import com.example.notesapp.navigation.NavGraph
import com.example.notesapp.ui.settings.SettingsRepository
import com.example.notesapp.ui.settings.ThemeMode
import com.example.notesapp.ui.theme.NotesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = NoteDatabase.getDatabase(this)
        val factory = ViewModelFactory(database.noteDao())
        val settingsRepository = SettingsRepository(this)

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            NotesAppTheme(themeMode = themeMode) {
                NavGraph(factory = factory, themeMode = themeMode)
            }
        }
    }
}
