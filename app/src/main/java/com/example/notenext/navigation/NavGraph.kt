package com.example.notenext.navigation

import com.example.notenext.ui.bin.BinScreen
import com.example.notenext.ui.bin.BinViewModel

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
            val binViewModel: BinViewModel = factory.create(BinViewModel::class.java)
            BinScreen(
                viewModel = binViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}