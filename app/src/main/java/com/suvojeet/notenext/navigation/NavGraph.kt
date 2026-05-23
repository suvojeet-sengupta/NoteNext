@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.navigation

import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label as OutlinedLabel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesViewModel
import com.suvojeet.notenext.ui.MainViewModel
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import com.suvojeet.notenext.R
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import com.suvojeet.notenext.ui.components.springPress
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute

@Composable
fun NavGraph(
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    mainViewModel: MainViewModel,
    startNoteId: Int = -1,
    startProjectId: Int = -1,
    startAddNote: Boolean = false,
    sharedText: String? = null,
    initialTitle: String? = null,
    searchQuery: String? = null,
    externalUri: android.net.Uri? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notesViewModel: NotesViewModel = hiltViewModel()
    val notesState by notesViewModel.listState.collectAsState()
    val editState by notesViewModel.editState.collectAsState()

    val isDecoySession by mainViewModel.isDecoySession.collectAsState()

    LaunchedEffect(isDecoySession) {
        notesViewModel.setDecoyMode(isDecoySession)
    }

    val activity = context.findActivity() as? FragmentActivity
    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            BiometricAuthManager(context, activity)
        }
    } else {
        null
    }

    LaunchedEffect(startNoteId) {
        if (startNoteId != -1) {
            val isLocked = notesViewModel.getNoteLockStatus(startNoteId)
            if (isLocked) {
                biometricAuthManager?.showBiometricPrompt(
                    onAuthSuccess = {
                        notesViewModel.onEvent(NotesEvent.ExpandNote(startNoteId))
                        navController.navigate(Destination.Notes()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onAuthError = {
                        Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                ) ?: Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
            } else {
                notesViewModel.onEvent(NotesEvent.ExpandNote(startNoteId))
                navController.navigate(Destination.Notes()) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(startProjectId) {
        if (startProjectId != -1) {
            navController.navigate(Destination.ProjectNotes(startProjectId)) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(startAddNote) {
        if (startAddNote) {
            notesViewModel.onEvent(NotesEvent.ExpandNote(-1))
        }
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            notesViewModel.onEvent(NotesEvent.CreateNoteFromSharedText(sharedText))
        }
    }

    LaunchedEffect(initialTitle) {
        if (initialTitle != null) {
            notesViewModel.onEvent(NotesEvent.SetInitialTitle(initialTitle))
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery != null) {
            notesViewModel.onEvent(NotesEvent.OnSearchQueryChange(searchQuery))
        }
    }

    LaunchedEffect(externalUri) {
        if (externalUri != null) {
            notesViewModel.onEvent(NotesEvent.LoadExternalFile(externalUri))
        }
    }

    var showMoreSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val onNotes = currentDestination?.hasRoute<Destination.Notes>() == true
    val onTags = currentDestination?.hasRoute<Destination.EditLabels>() == true
    val onSettings = currentDestination?.hasRoute<Destination.Settings>() == true

    // Bottom bar lives on the three top-level tab roots, and never while a
    // note is open in the editor.
    val showBottomBar = (onNotes && editState.expandedNoteId == null) || onTags || onSettings

    fun navigateTab(destination: Destination) {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                EditorialBottomBar(
                    onNotesSelected = onNotes,
                    onTagsSelected = onTags,
                    onSettingsSelected = onSettings,
                    onNotesClick = {
                        notesViewModel.onEvent(NotesEvent.FilterByLabel(null))
                        navigateTab(Destination.Notes())
                    },
                    onTagsClick = { navigateTab(Destination.EditLabels) },
                    onSearchClick = {
                        if (!onNotes) navigateTab(Destination.Notes())
                        notesViewModel.activateSearch()
                    },
                    onSettingsClick = { navigateTab(Destination.Settings) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(
                navController = navController,
                notesViewModel = notesViewModel,
                themeMode = themeMode,
                windowSizeClass = windowSizeClass,
                settingsRepository = settingsRepository,
                onMenuClick = { showMoreSheet = true },
                isCompact = windowSizeClass.widthSizeClass != androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded
            )
        }
    }

    if (showMoreSheet) {
        MoreSheet(
            labels = notesState.labels,
            activeLabel = notesState.filteredLabel,
            onDismiss = { showMoreSheet = false },
            onNavigate = { destination ->
                showMoreSheet = false
                navController.navigate(destination) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onLabelSelected = { label ->
                showMoreSheet = false
                notesViewModel.onEvent(NotesEvent.FilterByLabel(label))
                if (!onNotes) navigateTab(Destination.Notes())
            }
        )
    }
}

// ─── Editorial bottom navigation bar ─────────────────────────────────

@Composable
private fun EditorialBottomBar(
    onNotesSelected: Boolean,
    onTagsSelected: Boolean,
    onSettingsSelected: Boolean,
    onNotesClick: () -> Unit,
    onTagsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = onNotesSelected,
            onClick = onNotesClick,
            icon = { Icon(Icons.Filled.Note, contentDescription = stringResource(id = R.string.notes)) },
            label = { Text(stringResource(id = R.string.notes), style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = onTagsSelected,
            onClick = onTagsClick,
            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.labels_title)) },
            label = { Text(stringResource(id = R.string.labels_title), style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onSearchClick,
            icon = { Icon(Icons.Filled.Search, contentDescription = stringResource(id = R.string.search)) },
            label = { Text(stringResource(id = R.string.search), style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = onSettingsSelected,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(id = R.string.settings)) },
            label = { Text(stringResource(id = R.string.settings), style = MaterialTheme.typography.labelSmall) }
        )
    }
}

// ─── "More" sheet: every destination the drawer used to reach ────────

@Composable
private fun MoreSheet(
    labels: List<String>,
    activeLabel: String?,
    onDismiss: () -> Unit,
    onNavigate: (Destination) -> Unit,
    onLabelSelected: (String?) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
            )

            data class MoreItem(val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val destination: Destination)
            val items = listOf(
                MoreItem(R.string.projects, Icons.Filled.CreateNewFolder, Destination.Projects),
                MoreItem(R.string.archive, Icons.Filled.Archive, Destination.Archive),
                MoreItem(R.string.reminders, Icons.Filled.Notifications, Destination.Reminder),
                MoreItem(R.string.todos, Icons.Filled.PlaylistAddCheck, Destination.Todo),
                MoreItem(R.string.bin, Icons.Filled.Delete, Destination.Bin),
                MoreItem(R.string.create_new_label, Icons.AutoMirrored.Filled.Label, Destination.EditLabels)
            )
            items.forEach { item ->
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = stringResource(id = item.label)) },
                    label = { Text(stringResource(id = item.label)) },
                    selected = false,
                    onClick = { onNavigate(item.destination) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).springPress()
                )
            }

            if (labels.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))
                Text(
                    text = stringResource(id = R.string.labels_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )
                labels.forEach { label ->
                    NavigationDrawerItem(
                        icon = { Icon(OutlinedLabel, contentDescription = label) },
                        label = { Text(label) },
                        selected = activeLabel == label,
                        onClick = { onLabelSelected(label) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).springPress()
                    )
                }
            }
        }
    }
}

// ─── Shared NavHost ──────────────────────────────────────────────────

@Composable
private fun AppNavHost(
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    isCompact: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Notes()
    ) {
        notesGraph(navController, notesViewModel, themeMode, settingsRepository, onMenuClick, isCompact, windowSizeClass)
        projectGraph(navController, themeMode, settingsRepository, onMenuClick)
        settingsGraph(navController)
    }
}
