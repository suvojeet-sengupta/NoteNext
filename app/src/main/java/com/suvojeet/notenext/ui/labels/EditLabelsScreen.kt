@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.labels

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.components.SearchTopAppBar

@Composable
fun EditLabelsScreen(
    onBackPressed: () -> Unit
) {
    val viewModel: EditLabelsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    
    // Determine if FAB should be extended based on scroll
    val isExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10 }
    }

    val filteredLabels = remember(state.labels, state.searchQuery) {
        if (state.searchQuery.isEmpty()) {
            state.labels
        } else {
            state.labels.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (state.isSearchVisible) {
                SearchTopAppBar(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { viewModel.onEvent(EditLabelsEvent.OnSearchQueryChange(it)) },
                    onBackClick = { viewModel.onEvent(EditLabelsEvent.OnSearchVisibilityChange(false)) }
                )
            } else {
                MediumTopAppBar(
                    title = {
                        Text(
                            stringResource(id = R.string.edit_labels),
                            fontWeight = FontWeight.Normal
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed, modifier = Modifier.springPress()) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.onEvent(EditLabelsEvent.OnSearchVisibilityChange(true)) },
                            modifier = Modifier.springPress()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.edit_labels_search_cd))
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onEvent(EditLabelsEvent.ShowAddLabelDialog) },
                expanded = isExpanded,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(id = R.string.add_label), fontWeight = FontWeight.Bold) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.springPress(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ExpressiveSection(
                    title = stringResource(id = R.string.edit_labels_section_title),
                    description = stringResource(id = R.string.edit_labels_section_desc)
                ) {
                    // This section can hold a summary or quick actions if needed
                }
            }

            if (filteredLabels.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.Label,
                        message = if (state.searchQuery.isEmpty())
                            stringResource(id = R.string.edit_labels_empty_default)
                        else
                            stringResource(id = R.string.edit_labels_empty_search, state.searchQuery)
                    )
                }
            } else {
                item {
                    SettingsGroupCard {
                        filteredLabels.forEachIndexed { index, label ->
                            LabelItem(
                                label = label,
                                onEditClick = { viewModel.onEvent(EditLabelsEvent.ShowEditLabelDialog(label)) }
                            )
                            if (index < filteredLabels.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Extra space for FAB
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        if (state.showAddLabelDialog) {
            LabelActionSheet(
                title = stringResource(id = R.string.add_label),
                initialName = "",
                onDismiss = { viewModel.onEvent(EditLabelsEvent.HideDialog) },
                onConfirm = { name ->
                    viewModel.onEvent(EditLabelsEvent.AddLabel(name))
                }
            )
        }

        if (state.showEditLabelDialog) {
            state.selectedLabel?.let { label ->
                LabelActionSheet(
                    title = stringResource(id = R.string.edit_labels),
                    initialName = label.name,
                    isEdit = true,
                    onDismiss = { viewModel.onEvent(EditLabelsEvent.HideDialog) },
                    onConfirm = { newName ->
                        viewModel.onEvent(EditLabelsEvent.UpdateLabel(label, newName))
                    },
                    onDelete = {
                        viewModel.onEvent(EditLabelsEvent.DeleteLabel(label))
                    }
                )
            }
        }
    }
}

@Composable
fun LabelItem(
    label: Label,
    onEditClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
        headlineContent = { 
            Text(
                text = label.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onEditClick, modifier = Modifier.springPress()) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit_labels),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun LabelActionSheet(
    title: String,
    initialName: String,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp)
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (isEdit && onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.edit_labels_delete_cd))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(id = R.string.edit_labels_name_field)) },
                placeholder = { Text(stringResource(id = R.string.label_dialog_input_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                },
                trailingIcon = {
                    if (name.isNotEmpty()) {
                        IconButton(onClick = { name = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.edit_labels_clear_cd))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .springPress(),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = name.isNotBlank()
            ) {
                Text(
                    if (isEdit) stringResource(id = R.string.edit_labels_save_changes) else stringResource(id = R.string.edit_labels_create_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .springPress(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(stringResource(id = R.string.cancel), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
