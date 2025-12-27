```kotlin
package com.suvojeet.notenext.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    onMenuClick: () -> Unit,
    onProjectClick: (Int) -> Unit,
    navController: androidx.navigation.NavController,
    settingsRepository: SettingsRepository
) {
    val viewModel: ProjectViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    var showCreateProjectDialog by remember { mutableStateOf(false) }

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

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onConfirm = { projectName ->
                viewModel.onEvent(ProjectScreenEvent.CreateProject(projectName))
                showCreateProjectDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.projects)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateProjectDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.create_new_project))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.projects.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Create,
                    message = stringResource(id = R.string.no_projects_yet),
                    description = stringResource(id = R.string.create_new_project)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.projects) { project ->
                        ProjectItem(
                            project = project,
                            onClick = { onProjectClick(project.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.create_new_project)) },
        text = {
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text(stringResource(id = R.string.project_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(projectName) },
                enabled = projectName.isNotBlank()
            ) {
                Text(stringResource(id = R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
```
