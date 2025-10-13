
package com.example.notesapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notesapp.di.ViewModelFactory
import com.example.notesapp.ui.add_edit_note.AddEditNoteScreen
import com.example.notesapp.ui.notes.NotesScreen

import com.example.notesapp.ui.sample.SampleScreen

@Composable
fun NavGraph(factory: ViewModelFactory) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "notes") {
        composable("notes") {
            NotesScreen(
                factory = factory,
                onNoteClick = { navController.navigate("add_edit_note?noteId=$it") },
                onAddNoteClick = { navController.navigate("add_edit_note") },
                onSampleScreenClick = { navController.navigate("sample_screen") }
            )
        }
        composable(
            route = "add_edit_note?noteId={noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.IntType
                defaultValue = -1
            })
        ) {
            AddEditNoteScreen(
                factory = factory,
                onNoteSaved = { navController.popBackStack() }
            )
        }
        composable("sample_screen") {
            SampleScreen()
        }
    }
}
