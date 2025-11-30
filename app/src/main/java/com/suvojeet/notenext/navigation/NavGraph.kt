package com.suvojeet.notenext.navigation



import androidx.compose.animation.core.tween
import com.suvojeet.notenext.ui.project.toNotesUiEvent

import androidx.compose.animation.fadeIn

import androidx.compose.animation.fadeOut

import androidx.compose.animation.slideInHorizontally

import androidx.compose.animation.slideOutHorizontally

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Archive

import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CreateNewFolder

import androidx.compose.material.icons.automirrored.filled.Label

import androidx.compose.material.icons.automirrored.outlined.Label

import androidx.compose.material3.DrawerValue

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.ModalDrawerSheet

import androidx.compose.material3.ModalNavigationDrawer

import androidx.compose.material3.NavigationDrawerItem

import androidx.compose.material3.NavigationDrawerItemDefaults

import androidx.compose.material3.Text

import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet

import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.foundation.background

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.compose.NavHost

import androidx.navigation.compose.composable

import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.windowsizeclass.WindowSizeClass

import com.suvojeet.notenext.dependency_injection.ViewModelFactory

import com.suvojeet.notenext.ui.archive.ArchiveScreen

import com.suvojeet.notenext.ui.bin.BinScreen

import com.suvojeet.notenext.ui.bin.BinViewModel

import com.suvojeet.notenext.ui.labels.EditLabelsScreen

import com.suvojeet.notenext.ui.notes.NotesEvent

import com.suvojeet.notenext.ui.notes.NotesScreen

import com.suvojeet.notenext.ui.notes.NotesViewModel

import com.suvojeet.notenext.ui.lock.PinSetupScreen
import com.suvojeet.notenext.ui.settings.SettingsScreen
import com.suvojeet.notenext.ui.reminder.ReminderScreen
import com.suvojeet.notenext.ui.reminder.AddEditReminderScreen
import com.suvojeet.notenext.ui.settings.AboutScreen
import com.suvojeet.notenext.ui.project.ProjectScreen
import com.suvojeet.notenext.ui.project.ProjectViewModel
import com.suvojeet.notenext.ui.project.ProjectNotesScreen
import com.suvojeet.notenext.ui.project.ProjectNotesViewModel
import com.suvojeet.notenext.ui.project.toNotesState
import com.suvojeet.notenext.ui.project.toProjectNotesEvent
import com.suvojeet.notenext.ui.project.toNotesUiEvent
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen

import com.suvojeet.notenext.ui.settings.ThemeMode

import com.suvojeet.notenext.data.LinkPreviewRepository

import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.ui.settings.BackupScreen
import com.suvojeet.notenext.ui.settings.RestoreScreen

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import com.suvojeet.notenext.R
import kotlinx.coroutines.flow.SharingStarted



@Composable

fun NavGraph(factory: ViewModelFactory, themeMode: ThemeMode, windowSizeClass: WindowSizeClass, startNoteId: Int = -1, sharedText: String? = null) {

    val navController = rememberNavController()

    val context = LocalContext.current

    val settingsRepository = remember { SettingsRepository(context) }

    val linkPreviewRepository = remember { LinkPreviewRepository() }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val scope = rememberCoroutineScope()

    val notesViewModel: NotesViewModel = viewModel(factory = factory)

    val notesState by notesViewModel.state.collectAsState()

    LaunchedEffect(startNoteId) {
        if (startNoteId != -1) {
            notesViewModel.onEvent(NotesEvent.ExpandNote(startNoteId))
        }
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            notesViewModel.onEvent(NotesEvent.CreateNoteFromSharedText(sharedText))
        }
    }

    val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    

        if (isExpandedScreen) {

                        PermanentNavigationDrawer(

                            drawerContent = {

                                PermanentDrawerSheet(modifier = Modifier.fillMaxWidth(0.15f)) {

                                    Text(
                                        text = stringResource(id = R.string.app_name),

                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),

                                        modifier = Modifier.padding(16.dp)

                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                                    val currentRoute = navBackStackEntry?.destination?.route

            

                                    NavigationDrawerItem(

                                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.notes)) },

                                        label = { Text(stringResource(id = R.string.notes)) },

                                        selected = currentRoute == "notes" && notesState.filteredLabel == null,

                                        onClick = {
                                            if (currentRoute != "notes" || notesState.filteredLabel != null) {
                                                notesViewModel.onEvent(NotesEvent.FilterByLabel(null))
                                                navController.navigate("notes") {
                                                    popUpTo("notes") { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        },

                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                                    )

                                    NavigationDrawerItem(
                                        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(id = R.string.projects)) },
                                        label = { Text(stringResource(id = R.string.projects)) },
                                        selected = currentRoute == "projects",
                                        onClick = {
                                            navController.navigate("projects")
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )

                                    NavigationDrawerItem(

                                        icon = { Icon(Icons.Default.Archive, contentDescription = stringResource(id = R.string.archive)) },

                                        label = { Text(stringResource(id = R.string.archive)) },

                                        selected = currentRoute == "archive",

                                        onClick = {

                                            navController.navigate("archive") {

                                                popUpTo(navController.graph.startDestinationId)

                                                launchSingleTop = true

                                            }

                                        },

                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                                    )

                                    NavigationDrawerItem(

                                        icon = { Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.bin)) },

                                        label = { Text(stringResource(id = R.string.bin)) },

                                        selected = currentRoute == "bin",

                                        onClick = {

                                            navController.navigate("bin") {

                                                popUpTo(navController.graph.startDestinationId)

                                                launchSingleTop = true

                                            }

                                        },

                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                                    )

                                    NavigationDrawerItem(

                                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings)) },

                                        label = { Text(stringResource(id = R.string.settings)) },

                                        selected = currentRoute == "settings",

                                        onClick = {

                                            navController.navigate("settings")

                                        },

                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                                    )

                                    NavigationDrawerItem(

                                        icon = { Icon(Icons.Default.Notifications, contentDescription = stringResource(id = R.string.reminders)) },

                                        label = { Text(stringResource(id = R.string.reminders)) },

                                        selected = currentRoute == "reminder",

                                        onClick = {

                                            navController.navigate("reminder")

                                        },

                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                                    )

            

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            

                                    if (notesState.labels.isEmpty()) {

                                        NavigationDrawerItem(

                                            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.create_new_label)) },

                                            label = { Text(stringResource(id = R.string.create_new_label)) },

                                            selected = false,

                                            onClick = {

                                                navController.navigate("edit_labels")

                                            },

                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                                        )

                                    } else {

                                        Row(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .padding(horizontal = 16.dp),

                                            verticalAlignment = Alignment.CenterVertically,

                                            horizontalArrangement = Arrangement.SpaceBetween

                                        ) {

                                            Text(

                                                text = stringResource(id = R.string.labels_title),

                                                style = MaterialTheme.typography.labelSmall,

                                                color = MaterialTheme.colorScheme.onSurfaceVariant

                                            )

                                            IconButton(onClick = {

                                                navController.navigate("edit_labels")

                                            }) {

                                                Icon(

                                                    imageVector = Icons.Default.Edit,

                                                    contentDescription = stringResource(id = R.string.edit_labels),

                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant

                                                )

                                            }

                                        }

            

                                        notesState.labels.forEach { label ->

                                            NavigationDrawerItem(

                                                icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = label) },

                                                label = { Text(label) },

                                                selected = notesState.filteredLabel == label,

                                                onClick = {

                                                    notesViewModel.onEvent(NotesEvent.FilterByLabel(label))
                                                    if (currentRoute != "notes" || notesState.filteredLabel != label) {
                                                        navController.navigate("notes") {
                                                            popUpTo("notes") { inclusive = true }
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                },

                                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                                            )

                                        }

                                    }

                                }

                            }

                        ) {

                            NavHost(navController = navController, startDestination = "notes", modifier = Modifier.background(MaterialTheme.colorScheme.background)) {

            

                                composable(

            

                                    route = "notes",

            

                                    enterTransition = { fadeIn(animationSpec = tween(300)) },

            

                                    exitTransition = { fadeOut(animationSpec = tween(300)) }

            

                                ) {

            

                                    NotesScreen(

                                        viewModel = notesViewModel,

                                        onSettingsClick = { navController.navigate("settings") },

                                        onArchiveClick = { navController.navigate("archive") },

                                        onEditLabelsClick = { navController.navigate("edit_labels") },

                                        onBinClick = { navController.navigate("bin") },

                                        themeMode = themeMode,

                                        settingsRepository = settingsRepository,

                                        onMenuClick = { scope.launch { drawerState.open() } },

                                        events = notesViewModel.events

                                    )

            

                                }

    

                    composable(

    

                        route = "settings",

    

                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },

    

                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }

    

                    ) {

    

                        SettingsScreen(

    

                            onBackClick = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) }

    

                        )

    

                    }

                    composable(
                        route = "backup",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                        BackupScreen(
                            factory = factory,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "restore",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                        RestoreScreen(
                            factory = factory,
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

    

                            onMenuClick = { scope.launch { drawerState.open() } }

    

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

    

                        val binViewModel: BinViewModel = viewModel(factory = factory)

    

                        BinScreen(

    

                            viewModel = binViewModel,

    

                            onMenuClick = { scope.launch { drawerState.open() } }

    

                        )

    

                    }

                    composable(
                        route = "pin_setup",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                        PinSetupScreen(
                            onPinSet = { navController.popBackStack() }
                        )
                    }

    

                }

            }

        } else {

            ModalNavigationDrawer(

    

    

    

                drawerState = drawerState,

    

    

    

                gesturesEnabled = notesState.expandedNoteId == null,

    

    

    

                drawerContent = {

    

                    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.7f)) {

    

                        Text(

    

                            text = stringResource(id = R.string.app_name),

    

                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),

    

                            modifier = Modifier.padding(16.dp)

    

                        )

    

                        Spacer(modifier = Modifier.height(16.dp))

                        val navBackStackEntry by navController.currentBackStackEntryAsState()

                        val currentRoute = navBackStackEntry?.destination?.route

    

                        NavigationDrawerItem(

                            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.notes)) },

                            label = { Text(stringResource(id = R.string.notes)) },

                            selected = currentRoute == "notes" && notesState.filteredLabel == null,

                            onClick = {
                                scope.launch { drawerState.close() }
                                if (currentRoute != "notes" || notesState.filteredLabel != null) {
                                    notesViewModel.onEvent(NotesEvent.FilterByLabel(null))
                                    navController.navigate("notes") {
                                        popUpTo("notes") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            },

                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(id = R.string.projects)) },
                            label = { Text(stringResource(id = R.string.projects)) },
                            selected = currentRoute == "projects",
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("projects")
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(

                            icon = { Icon(Icons.Default.Archive, contentDescription = stringResource(id = R.string.archive)) },

                            label = { Text(stringResource(id = R.string.archive)) },

                            selected = currentRoute == "archive",

                            onClick = {

                                scope.launch { drawerState.close() }

                                navController.navigate("archive") {

                                    popUpTo(navController.graph.startDestinationId)

                                    launchSingleTop = true

                                }

                            },

                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                        )

                        NavigationDrawerItem(

                            icon = { Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.bin)) },

                            label = { Text(stringResource(id = R.string.bin)) },

                            selected = currentRoute == "bin",

                            onClick = {

                                scope.launch { drawerState.close() }

                                navController.navigate("bin") {

                                    popUpTo(navController.graph.startDestinationId)

                                    launchSingleTop = true

                                }

                            },

                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                        )

                        NavigationDrawerItem(

                            icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings)) },

                            label = { Text(stringResource(id = R.string.settings)) },

                            selected = currentRoute == "settings",

                            onClick = {

                                scope.launch { drawerState.close() }

                                navController.navigate("settings")

                            },

                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                        )

                        NavigationDrawerItem(

                            icon = { Icon(Icons.Default.Notifications, contentDescription = stringResource(id = R.string.reminders)) },

                            label = { Text(stringResource(id = R.string.reminders)) },

                            selected = currentRoute == "reminder",

                            onClick = {

                                scope.launch { drawerState.close() }

                                navController.navigate("reminder")

                            },

                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                        )

    

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    

                        if (notesState.labels.isEmpty()) {

                            NavigationDrawerItem(

                                icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.create_new_label)) },

                                label = { Text(stringResource(id = R.string.create_new_label)) },

                                selected = false,

                                onClick = {

                                    scope.launch { drawerState.close() }

                                    navController.navigate("edit_labels")
                                },

                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                            )

                        } else {

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(horizontal = 16.dp),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.SpaceBetween

                            ) {

                                Text(

                                    text = stringResource(id = R.string.labels_title),

                                    style = MaterialTheme.typography.labelSmall,

                                    color = MaterialTheme.colorScheme.onSurfaceVariant

                                )

                                IconButton(onClick = {

                                    scope.launch { drawerState.close() }

                                    navController.navigate("edit_labels")

                                }) {

                                    Icon(

                                        imageVector = Icons.Default.Edit,

                                        contentDescription = stringResource(id = R.string.edit_labels),

                                        tint = MaterialTheme.colorScheme.onSurfaceVariant

                                    )

                                }

                            }

    

                            notesState.labels.forEach { label ->

                                NavigationDrawerItem(

                                    icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = label) },

                                    label = { Text(label) },

                                    selected = notesState.filteredLabel == label,

                                                                            onClick = {
                                    
                                                                                scope.launch { drawerState.close() }
                                    
                                                                                notesViewModel.onEvent(NotesEvent.FilterByLabel(label))
                                                                                if (currentRoute != "notes" || notesState.filteredLabel != label) {
                                                                                    navController.navigate("notes") {
                                                                                        popUpTo("notes") { inclusive = true }
                                                                                        launchSingleTop = true
                                                                                    }
                                                                                }
                                                                            },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                                )

                            }

                        }

                    }

                }

            ) {

                NavHost(navController = navController, startDestination = "notes", modifier = Modifier.background(MaterialTheme.colorScheme.background)) {

    

                    composable(

    

                        route = "notes",

    

                        enterTransition = { fadeIn(animationSpec = tween(300)) },

    

                        exitTransition = { fadeOut(animationSpec = tween(300)) }

    

                    ) {

    

                        NotesScreen(

                            viewModel = notesViewModel,

                            onSettingsClick = { navController.navigate("settings") },

                            onArchiveClick = { navController.navigate("archive") },

                            onEditLabelsClick = { navController.navigate("edit_labels") },

                            onBinClick = { navController.navigate("bin") },

                            themeMode = themeMode,

                            settingsRepository = settingsRepository,

                            onMenuClick = { scope.launch { drawerState.open() } },

                            events = notesViewModel.events

                        )

    

                    }

    

                    composable(

    

                        route = "settings",

    

                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },

    

                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }

    

                    ) {

    

                        SettingsScreen(

    

                            onBackClick = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) }

    

                        )

    

                    }

                    composable(
                        route = "backup",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                        BackupScreen(
                            factory = factory,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "restore",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                        RestoreScreen(
                            factory = factory,
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

    

                            onMenuClick = { scope.launch { drawerState.open() } }

    

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

    

                        val binViewModel: BinViewModel = viewModel(factory = factory)

    

                        BinScreen(

    

                            viewModel = binViewModel,

    

                            onMenuClick = { scope.launch { drawerState.open() } }

    

                        )

    

                    }

                    composable(
                        route = "pin_setup",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                                                PinSetupScreen(
                                                    onPinSet = { navController.popBackStack() }
                                                )
                                            }
                                            composable(
                                                route = "reminder",
                                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                                                exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                                            ) {
                                                                        ReminderScreen(
                                                                            factory = factory,
                                                                            onBackClick = { navController.popBackStack() },
                                                                            onAddReminderClick = { navController.navigate("add_edit_reminder") }
                                                                        )
                                                                    }
                                                                    composable(
                                                                        route = "add_edit_reminder",
                                                                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                                                                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                                                                    ) {
                                                                        AddEditReminderScreen(
                                                                            onBackClick = { navController.popBackStack() }
                                                                        )
                                                                    }
                    composable(
                        route = "projects",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                        ProjectScreen(
                            factory = factory,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onProjectClick = { projectId ->
                                navController.navigate("project_notes/$projectId")
                            },
                            navController = navController,
                            settingsRepository = settingsRepository
                        )
                    }
                    composable(
                        route = "about",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                        AboutScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "project_notes/{projectId}",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                    ) {
                        ProjectNotesScreen(
                            factory = factory,
                            onBackClick = { navController.popBackStack() },
                            themeMode = themeMode,
                            settingsRepository = settingsRepository
                        )
                    }
                    composable(
                        route = "add_edit_note?projectId={projectId}&noteType={noteType}",
                        arguments = listOf(
                            androidx.navigation.navArgument("projectId") { type = androidx.navigation.NavType.IntType; defaultValue = -1 },
                            androidx.navigation.navArgument("noteType") { type = androidx.navigation.NavType.StringType; defaultValue = "TEXT" }
                        )
                    ) {
                        val viewModel: ProjectNotesViewModel = viewModel(factory = factory)
                        AddEditNoteScreen(
                            state = viewModel.state.collectAsState().value.toNotesState(),
                            onEvent = { viewModel.onEvent(it.toProjectNotesEvent()) },
                            onDismiss = { navController.popBackStack() },
                            themeMode = themeMode,
                            settingsRepository = settingsRepository,
                            events = viewModel.events.map { it.toNotesUiEvent() }.shareIn(rememberCoroutineScope(), SharingStarted.WhileSubscribed())
                        )
                    }                        
                            
                        
                                        }
                                    }
                                }
                        }