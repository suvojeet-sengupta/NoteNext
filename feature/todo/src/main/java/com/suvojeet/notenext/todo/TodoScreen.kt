@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.todo

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.suvojeet.notenext.core.R
import com.suvojeet.notenext.ui.components.ExpressiveLoading
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun TodoScreen(
    onBackClick: () -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagedTodos = viewModel.pagedTodos.collectAsLazyPagingItems()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val shareChooserTitle = stringResource(R.string.todo_share_chooser_title)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TodoUiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onAction?.invoke()
                    }
                }
                is TodoUiEvent.ShareTodo -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, event.title)
                        putExtra(android.content.Intent.EXTRA_TEXT, event.content)
                    }
                    val chooser = android.content.Intent.createChooser(intent, shareChooserTitle)
                    context.startActivity(chooser)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.todo_screen_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.todo_back_content_description)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(TodoEvent.ShowAiTodoDialog) }, modifier = Modifier.springPress()) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = stringResource(R.string.todo_ai_action_content_description)
                        )
                    }
                    if (state.filter == TodoFilter.Completed) {
                        IconButton(onClick = { viewModel.onEvent(TodoEvent.DeleteAllCompleted) }, modifier = Modifier.springPress()) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.todo_clear_completed_content_description)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onEvent(TodoEvent.ShowAddDialog) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.springPress(),
                icon = {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.todo_add_fab_content_description)
                    )
                },
                text = { Text(stringResource(R.string.add_todo)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterSegmentedRow(
                selectedFilter = state.filter,
                onFilterSelected = { viewModel.onEvent(TodoEvent.SetFilter(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (!state.isLoading && state.filter is TodoFilter.All) {
                ProductivityDashboard(
                    activeCount = state.activeCount,
                    completedTodayCount = state.completedTodayCount,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                )
            }

            if (state.isLoading) {
                ExpressiveLoading()
            } else if (pagedTodos.itemCount == 0) {
                EmptyState(
                    icon = Icons.Default.CheckCircle,
                    message = when (state.filter) {
                        is TodoFilter.All -> stringResource(R.string.todo_empty_all)
                        is TodoFilter.Active -> stringResource(R.string.todo_empty_active)
                        is TodoFilter.Completed -> stringResource(R.string.todo_empty_completed)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        count = pagedTodos.itemCount,
                        key = pagedTodos.itemKey { it.todo.id },
                        contentType = pagedTodos.itemContentType { "todo" }
                    ) { index ->
                        pagedTodos[index]?.let { todoWithSubtasks ->
                            TodoItemCard(
                                todoWithSubtasks = todoWithSubtasks,
                                onToggleComplete = { viewModel.onEvent(TodoEvent.ToggleComplete(todoWithSubtasks.todo)) },
                                onClick = { viewModel.onEvent(TodoEvent.ShowEditDialog(todoWithSubtasks.todo)) },
                                onDelete = { viewModel.onEvent(TodoEvent.DeleteTodo(todoWithSubtasks.todo)) },
                                onConvertToNote = { viewModel.onEvent(TodoEvent.ConvertToNote(todoWithSubtasks)) },
                                onShare = { viewModel.onEvent(TodoEvent.ShareTodo(todoWithSubtasks)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showAddEditDialog) {
        val projects by viewModel.projects.collectAsStateWithLifecycle(initialValue = emptyList())
        AddEditTodoDialog(
            editingTodo = state.editingTodo,
            initialSubtasks = state.editingSubtasks,
            projects = projects,
            onDismiss = { viewModel.onEvent(TodoEvent.DismissDialog) },
            onSave = { title, description, priority, dueDate, reminderTime, projectId, subtasks ->
                viewModel.onEvent(TodoEvent.SaveTodo(title, description, priority, dueDate, reminderTime, projectId, subtasks))
            },
            onDraftChange = { viewModel.onEvent(TodoEvent.OnDraftChange(it)) }
        )
    }

    if (state.showAiTodoDialog) {
        AiTodoDialog(
            isGenerating = state.isGenerating,
            onDismiss = { viewModel.onEvent(TodoEvent.DismissAiTodoDialog) },
            onGenerate = { viewModel.onEvent(TodoEvent.GenerateAiTodos(it)) }
        )
    }
}

@Composable
fun ProductivityDashboard(
    activeCount: Int,
    completedTodayCount: Int,
    modifier: Modifier = Modifier
) {
    val total = activeCount + completedTodayCount
    val progress = if (total > 0) completedTodayCount.toFloat() / total else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.todo_daily_progress),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (completedTodayCount > 0)
                            stringResource(R.string.todo_crushed_tasks_today, completedTodayCount)
                        else
                            stringResource(R.string.todo_time_to_get_started),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    label = stringResource(R.string.todo_remaining),
                    value = activeCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = stringResource(R.string.todo_done_today),
                    value = completedTodayCount.toString(),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun FilterSegmentedRow(
    selectedFilter: TodoFilter,
    onFilterSelected: (TodoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        TodoFilter.All to stringResource(R.string.todo_filter_all),
        TodoFilter.Active to stringResource(R.string.todo_filter_active),
        TodoFilter.Completed to stringResource(R.string.todo_filter_done),
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        filters.forEachIndexed { index, (filter, label) ->
            SegmentedButton(
                selected = selectedFilter::class == filter::class,
                onClick = { onFilterSelected(filter) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size),
                modifier = Modifier.springPress()
            ) {
                Text(label)
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun AiTodoDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_todo_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.ai_todo_dialog_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.ai_todo_dialog_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(input) },
                enabled = input.isNotBlank() && !isGenerating
            ) {
                if (isGenerating) {
                    LoadingIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.ai_todo_dialog_thinking),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(stringResource(R.string.ai_todo_dialog_generate))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
