package com.suvojeet.notenext.navigation



import androidx.compose.animation.core.tween

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

import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.compose.NavHost

import androidx.navigation.compose.composable

import androidx.navigation.compose.rememberNavController

import com.suvojeet.notenext.dependency_injection.ViewModelFactory

import com.suvojeet.notenext.ui.archive.ArchiveScreen

import com.suvojeet.notenext.ui.bin.BinScreen

import com.suvojeet.notenext.ui.bin.BinViewModel

import com.suvojeet.notenext.ui.labels.EditLabelsScreen

import com.suvojeet.notenext.ui.notes.NotesEvent

import com.suvojeet.notenext.ui.notes.NotesScreen

import com.suvojeet.notenext.ui.notes.NotesViewModel

import com.suvojeet.notenext.ui.settings.SettingsScreen

import com.suvojeet.notenext.ui.settings.ThemeMode

import com.suvojeet.notenext.data.LinkPreviewRepository

import com.suvojeet.notenext.ui.settings.SettingsRepository

import kotlinx.coroutines.launch



@Composable

fun NavGraph(factory: ViewModelFactory, themeMode: ThemeMode) {

    val navController = rememberNavController()

    val context = LocalContext.current

    val settingsRepository = remember { SettingsRepository(context) }

    val linkPreviewRepository = remember { LinkPreviewRepository() }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val scope = rememberCoroutineScope()

    val notesViewModel: NotesViewModel = viewModel(factory = ViewModelFactory(factory.noteDao, factory.labelDao, linkPreviewRepository))

    val notesState by notesViewModel.state.collectAsState()



    ModalNavigationDrawer(

        drawerState = drawerState,

        drawerContent = {

            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {

                Text(

                    text = "NoteNext",

                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),

                    modifier = Modifier.padding(16.dp)

                )

                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(

                    icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Notes") },

                    label = { Text("Notes") },

                    selected = notesState.filteredLabel == null,

                    onClick = {

                        scope.launch { drawerState.close() }

                        notesViewModel.onEvent(NotesEvent.FilterByLabel(null))

                        navController.navigate("notes") {

                            popUpTo(navController.graph.startDestinationId)

                            launchSingleTop = true

                        }

                    },

                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                )

                NavigationDrawerItem(

                    icon = { Icon(Icons.Default.Archive, contentDescription = "Archive") },

                    label = { Text("Archive") },

                    selected = false,

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

                    icon = { Icon(Icons.Default.Delete, contentDescription = "Bin") },

                    label = { Text("Bin") },

                    selected = false,

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

                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },

                    label = { Text("Settings") },

                    selected = false,

                    onClick = {

                        scope.launch { drawerState.close() }

                        navController.navigate("settings")

                    },

                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                )



                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))



                if (notesState.labels.isEmpty()) {

                    NavigationDrawerItem(

                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Create new label") },

                        label = { Text("Create new label") },

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

                            text = "LABELS",

                            style = MaterialTheme.typography.labelSmall,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                        IconButton(onClick = {

                            scope.launch { drawerState.close() }

                            navController.navigate("edit_labels")

                        }) {

                            Icon(

                                imageVector = Icons.Default.Edit,

                                contentDescription = "Edit Labels",

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

                                navController.navigate("notes") {

                                    popUpTo(navController.graph.startDestinationId)

                                    launchSingleTop = true

                                }

                            },

                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

                        )

                    }

                }

            }

        }

    ) {

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

                    settingsRepository = settingsRepository,

                    onMenuClick = { scope.launch { drawerState.open() } }

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

        }

    }

}
