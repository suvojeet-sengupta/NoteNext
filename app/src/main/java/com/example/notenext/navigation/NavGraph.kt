package com.example.notenext.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.notenext.dependency_injection.ViewModelFactory
import com.example.notenext.ui.archive.ArchiveScreen
import com.example.notenext.ui.bin.BinScreen
import com.example.notenext.ui.bin.BinViewModel
import com.example.notenext.ui.labels.EditLabelsScreen
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
                onSettingsClick = { navController.navigate("settings") },
                onArchiveClick = { navController.navigate("archive") },
                onEditLabelsClick = { navController.navigate("edit_labels") },
                onBinClick = { navController.navigate("bin") },
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
        composable(
            route = "archive",
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            ArchiveScreen(
                factory = factory,
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable(
            route = "edit_labels",
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            EditLabelsScreen(
                factory = factory,
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable(
            route = "bin",
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            val binViewModel: BinViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
            BinScreen(
                viewModel = binViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}