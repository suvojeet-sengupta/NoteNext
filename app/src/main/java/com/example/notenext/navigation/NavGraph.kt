
package com.example.notenext.navigation

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
import com.example.notenext.dependency_injection.ViewModelFactory
import com.example.notenext.ui.add_edit_note.AddEditNoteScreen
import com.example.notenext.ui.notes.NotesScreen
import com.example.notenext.ui.settings.SettingsScreen
import com.example.notenext.ui.settings.ThemeMode

@Composable
fun NavGraph(factory: ViewModelFactory, themeMode: ThemeMode) {
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
                onAddNoteClick = { navController.navigate("add_edit_note") },
                onSettingsClick = { navController.navigate("settings") }
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
                onNoteSaved = { navController.popBackStack() },
                themeMode = themeMode
            )
        }
        composable(
            route = "settings",
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
