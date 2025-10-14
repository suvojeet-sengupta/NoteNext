
package com.example.notesapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notesapp.di.ViewModelFactory
import com.example.notesapp.ui.add_edit_note.AddEditNoteScreen
import com.example.notesapp.ui.notes.NotesScreen

@Composable
fun NavGraph(factory: ViewModelFactory) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "notes") {
        composable(
            route = "notes",
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            NotesScreen(
                factory = factory,
                onNoteClick = { navController.navigate("add_edit_note?noteId=$it") },
                onAddNoteClick = { navController.navigate("add_edit_note") }
            )
        }
        composable(
            route = "add_edit_note?noteId={noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.IntType
                defaultValue = -1
            }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            AddEditNoteScreen(
                factory = factory,
                onNoteSaved = { navController.popBackStack() }
            )
        }
    }
}
