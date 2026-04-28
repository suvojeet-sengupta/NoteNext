@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.navigation

import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
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
import com.suvojeet.notenext.ui.notes.NotesListState

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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
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

    val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    if (isExpandedScreen) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.15f),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    drawerShape = MaterialTheme.shapes.extraLarge
                ) {
                    DrawerContent(
                        navController = navController,
                        notesState = notesState,
                        notesViewModel = notesViewModel,
                        onCloseDrawer = { /* no-op for permanent drawer */ }
                    )
                }
            }
        ) {
            AppNavHost(
                navController = navController,
                notesViewModel = notesViewModel,
                themeMode = themeMode,
                windowSizeClass = windowSizeClass,
                settingsRepository = settingsRepository,
                onMenuClick = { scope.launch { drawerState.open() } },
                isCompact = false
            )
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = editState.expandedNoteId == null,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                ) {
                    DrawerContent(
                        navController = navController,
                        notesState = notesState,
                        notesViewModel = notesViewModel,
                        onCloseDrawer = { action ->
                            scope.launch {
                                drawerState.close()
                                action()
                            }
                        }
                    )                }
            }
        ) {
            AppNavHost(
                navController = navController,
                notesViewModel = notesViewModel,
                themeMode = themeMode,
                windowSizeClass = windowSizeClass,
                settingsRepository = settingsRepository,
                onMenuClick = { scope.launch { drawerState.open() } },
                isCompact = true
            )
        }
    }
}

// ─── Shared drawer content ───────────────────────────────────────────

@Composable
private fun DrawerContent(
    navController: NavHostController,
    notesState: NotesListState,
    notesViewModel: NotesViewModel,
    onCloseDrawer: (onClosed: () -> Unit) -> Unit
) {
    Text(
        text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(24.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    data class DrawerItem(val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val isSelected: Boolean, val onClick: () -> Unit)

    val items = listOf(
        DrawerItem(
            R.string.notes, 
            Icons.AutoMirrored.Filled.Label, 
            currentDestination?.hasRoute<Destination.Notes>() == true && notesState.filteredLabel == null
        ) {
            onCloseDrawer {
                if (currentDestination?.hasRoute<Destination.Notes>() != true || notesState.filteredLabel != null) {
                    notesViewModel.onEvent(NotesEvent.FilterByLabel(null))
                    navController.navigate(Destination.Notes()) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
        DrawerItem(
            R.string.projects, 
            Icons.Default.CreateNewFolder, 
            currentDestination?.hasRoute<Destination.Projects>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Projects) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.archive, 
            Icons.Default.Archive, 
            currentDestination?.hasRoute<Destination.Archive>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Archive) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.reminders, 
            Icons.Default.Notifications, 
            currentDestination?.hasRoute<Destination.Reminder>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Reminder) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.todos, 
            Icons.Default.PlaylistAddCheck, 
            currentDestination?.hasRoute<Destination.Todo>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Todo) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.bin, 
            Icons.Default.Delete, 
            currentDestination?.hasRoute<Destination.Bin>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Bin) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.settings, 
            Icons.Default.Settings, 
            currentDestination?.hasRoute<Destination.Settings>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Settings) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    )

    items.forEach { item ->
        NavigationDrawerItem(
            icon = { Icon(item.icon, contentDescription = stringResource(id = item.label)) },
            label = { Text(stringResource(id = item.label), fontWeight = FontWeight.Bold) },
            selected = item.isSelected,
            onClick = item.onClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).springPress()
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

    if (notesState.labels.isEmpty()) {
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.create_new_label)) },
            label = { Text(stringResource(id = R.string.create_new_label), fontWeight = FontWeight.Bold) },
            selected = false,
            onClick = {
                onCloseDrawer {
                    navController.navigate(Destination.EditLabels) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).springPress()
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.labels_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = {
                onCloseDrawer {
                    navController.navigate(Destination.EditLabels) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }, modifier = Modifier.size(24.dp).springPress()) {
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
                label = { Text(label, fontWeight = FontWeight.Medium) },
                selected = notesState.filteredLabel == label,
                onClick = {
                    onCloseDrawer {
                        notesViewModel.onEvent(NotesEvent.FilterByLabel(label))
                        if (currentDestination?.hasRoute<Destination.Notes>() != true) {
                            navController.navigate(Destination.Notes()) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).springPress()
            )
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
        startDestination = Destination.Notes(),
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        notesGraph(navController, notesViewModel, themeMode, settingsRepository, onMenuClick, isCompact, windowSizeClass)
        projectGraph(navController, themeMode, settingsRepository, onMenuClick)
        settingsGraph(navController)
    }
}
