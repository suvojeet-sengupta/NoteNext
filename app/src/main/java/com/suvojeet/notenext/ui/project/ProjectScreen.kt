@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.project

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.theme.ThemeMode

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.platform.LocalContext
import com.suvojeet.notenext.util.ShortcutUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectScreen(
    onMenuClick: () -> Unit,
    onProjectClick: (Int) -> Unit,
    navController: androidx.navigation.NavController,
    settingsRepository: SettingsRepository
) {
    val viewModel: ProjectViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showCreateSubProjectDialog by remember { mutableStateOf<Int?>(null) }
    var projectToDelete by remember { mutableStateOf<Int?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }
    val expandedProjects = remember { mutableStateMapOf<Int, Boolean>() }
    val context = LocalContext.current

    // Build hierarchical project list
    val hierarchicalProjects = remember(state.projects) {
        val projectMap = state.projects.associateBy { it.id }
        val rootProjects = state.projects.filter { it.parentId == null }

        fun buildHierarchy(project: com.suvojeet.notenext.data.Project, depth: Int): List<HierarchicalProjectEntry> {
            val entry = HierarchicalProjectEntry(project, depth)
            val isExpanded = expandedProjects[project.id] ?: false
            val children = state.projects.filter { it.parentId == project.id }

            return listOf(entry) + if (isExpanded) {
                children.flatMap { child -> buildHierarchy(child, depth + 1) }
            } else {
                emptyList()
            }
        }

        rootProjects.flatMap { buildHierarchy(it, 0) }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectScreenEvent.CreateNewNote -> {
                    navController.navigate("add_edit_note?projectId=${event.projectId}&noteType=TEXT")
                }
                is ProjectScreenEvent.CreateNewChecklist -> {
                    navController.navigate("add_edit_note?projectId=${event.projectId}&noteType=CHECKLIST")
                }
                else -> { }
            }
        }
    }

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onConfirm = { projectName, projectDescription ->
                viewModel.onEvent(ProjectScreenEvent.CreateProject(projectName, projectDescription))
                showCreateProjectDialog = false
            }
        )
    }

    if (showCreateSubProjectDialog != null) {
        CreateProjectDialog(
            onDismiss = { showCreateSubProjectDialog = null },
            onConfirm = { projectName, projectDescription ->
                viewModel.onEvent(ProjectScreenEvent.CreateProject(projectName, projectDescription, showCreateSubProjectDialog))
                showCreateSubProjectDialog = null
            }
        )
    }

    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(id = R.string.projects_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.projects_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.let { viewModel.onEvent(ProjectScreenEvent.DeleteProject(it)) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.projects),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.springPress()) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTopMenu = true }, modifier = Modifier.springPress()) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.projects_more_cd))
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.projects_add_menu)) },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showCreateProjectDialog = true
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.projects.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Folder,
                    message = stringResource(id = R.string.no_projects_yet),
                    description = stringResource(id = R.string.create_first_project)
                )
            } else {
                ExpressiveSection(
                    title = stringResource(id = R.string.projects_section_title),
                    description = stringResource(id = R.string.projects_section_desc)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(hierarchicalProjects, key = { it.project.id }) { entry ->
                            HierarchicalProjectItem(
                                project = entry.project,
                                isExpanded = expandedProjects[entry.project.id] ?: false,
                                onToggleExpand = {
                                    expandedProjects[entry.project.id] = expandedProjects[entry.project.id] != true
                                },
                                onClick = { onProjectClick(entry.project.id) },
                                onLongClick = {
                                    projectToDelete = entry.project.id
                                },
                                onCreateSubProject = {
                                    showCreateSubProjectDialog = entry.project.id
                                },
                                onAddToHome = {
                                    ShortcutUtils.pinProjectToHome(context, entry.project.id, entry.project.name)
                                },
                                depth = entry.depth
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(id = R.string.create_new_project), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(id = R.string.project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text(stringResource(id = R.string.project_description)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(projectName, projectDescription.ifBlank { null }) },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.springPress(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(id = R.string.create), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

data class HierarchicalProjectEntry(
    val project: com.suvojeet.notenext.data.Project,
    val depth: Int
)
