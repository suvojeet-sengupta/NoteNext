
package com.example.notesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.notesapp.data.NoteDatabase
import com.example.notesapp.di.ViewModelFactory
import com.example.notesapp.navigation.NavGraph
import com.example.notesapp.ui.theme.NotesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = NoteDatabase.getDatabase(this)
        val factory = ViewModelFactory(database.noteDao())
        setContent {
            NotesAppTheme {
                NavGraph(factory = factory)
            }
        }
    }
}
