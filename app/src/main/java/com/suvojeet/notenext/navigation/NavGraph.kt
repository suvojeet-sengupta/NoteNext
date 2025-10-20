package com.suvojeet.notenext.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.suvojeet.notenext.dependency_injection.ViewModelFactory
import com.suvojeet.notenext.ui.archive.ArchiveScreen
import com.suvojeet.notenext.ui.bin.BinScreen
import com.suvojeet.notenext.ui.bin.BinViewModel
import com.suvojeet.notenext.ui.labels.EditLabelsScreen
import com.suvojeet.notenext.ui.notes.NotesScreen
import com.suvojeet.notenext.ui.settings.SettingsScreen
import com.suvojeet.notenext.ui.settings.ThemeMode
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.ui.settings.SettingsRepository
import androidx.compose.ui.platform.LocalContext

@Composable
fun NavGraph(factory: ViewModelFactory, themeMode: ThemeMode) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val linkPreviewRepository = remember { LinkPreviewRepository() }

    NavHost(navController = navController, startDestination = "notes") {
        composable(
            route = "notes",
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            NotesScreen(
                factory = ViewModelFactory(factory.noteDao, factory.labelDao, linkPreviewRepository),
                onSettingsClick = { navController.navigate("settings") },
                onArchiveClick = { navController.navigate("archive") },
                onEditLabelsClick = { navController.navigate("edit_labels") },
                onBinClick = { navController.navigate("bin") },
                themeMode = themeMode,
                settingsRepository = settingsRepository
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