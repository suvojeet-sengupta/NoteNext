
package com.suvojeet.notenext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.suvojeet.notenext.data.NoteDatabase
import com.suvojeet.notenext.dependency_injection.ViewModelFactory
import com.suvojeet.notenext.navigation.NavGraph
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.ui.settings.ThemeMode
import com.suvojeet.notenext.ui.theme.NoteNextTheme
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.ui.theme.ShapeFamily

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = NoteDatabase.getDatabase(this)
        val linkPreviewRepository = LinkPreviewRepository()
        val factory = ViewModelFactory(database.noteDao(), database.labelDao(), linkPreviewRepository)
        val settingsRepository = SettingsRepository(this)

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val shapeFamily by settingsRepository.shapeFamily.collectAsState(initial = ShapeFamily.EXPRESSIVE)
            NoteNextTheme(themeMode = themeMode, shapeFamily = shapeFamily) {
                NavGraph(factory = factory, themeMode = themeMode)
            }
        }
    }
}
