package com.suvojeet.notenext.ui.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.notenext.dependency_injection.ViewModelFactory

import com.suvojeet.notenext.ui.components.MultiActionFab
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.ui.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    factory: ViewModelFactory,
    onMenuClick: () -> Unit,
    onProjectClick: (Int) -> Unit,
    navController: androidx.navigation.NavController,
    settingsRepository: SettingsRepository
) {
    val viewModel: ProjectViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    var isFabExpanded by remember { mutableStateOf(false) }
    val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectScreenEvent.CreateNewNote -> {
                    navController.navigate("add_edit_note?projectId=${event.projectId}&noteType=TEXT")
                }
                is ProjectScreenEvent.CreateNewChecklist -> {
                    navController.navigate("add_edit_note?projectId=${event.projectId}&noteType=CHECKLIST")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            MultiActionFab(
                isExpanded = isFabExpanded,
                onExpandedChange = { isFabExpanded = it },
                onNoteClick = {
                    viewModel.onEvent(ProjectScreenEvent.CreateNewNote(-1))
                    isFabExpanded = false
                },
                onChecklistClick = {
                    viewModel.onEvent(ProjectScreenEvent.CreateNewChecklist(-1))
                    isFabExpanded = false
                },
                onProjectClick = {
                    // Do nothing
                },
                themeMode = themeMode
            )
        }
    ) {
        padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "No projects yet. Create one from the main screen FAB.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        overflow = TextOverflow.Visible
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.projects) { project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { onProjectClick(project.id) },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
