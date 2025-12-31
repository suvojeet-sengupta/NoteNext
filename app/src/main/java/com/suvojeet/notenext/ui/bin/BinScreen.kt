package com.suvojeet.notenext.ui.bin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.NoteItem
import com.suvojeet.notenext.data.NoteWithAttachments

import androidx.compose.material.icons.filled.Menu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BinScreen(
    viewModel: BinViewModel,
    onMenuClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()
    var showEmptyBinDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSelectionModeActive) {
                    BinContextualTopAppBar(
                        selectedItemCount = state.selectedNoteIds.size,
                        onClearSelection = { viewModel.onEvent(BinEvent.ClearSelection) },
                        onRestoreClick = { viewModel.onEvent(BinEvent.RestoreSelectedNotes) },
                        onDeletePermanentlyClick = { viewModel.onEvent(BinEvent.DeleteSelectedNotesPermanently) }
                    )
                } else {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.bin_title)) },
                        navigationIcon = {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                            }
                        },
                        actions = {
                            if (state.notes.isNotEmpty()) {
                                IconButton(onClick = { showEmptyBinDialog = true }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Empty Bin")
                                }
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (state.notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.bin_empty_message),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        items(
                            items = state.notes,
                            key = { it.note.id }
                        ) { noteWithAttachments ->
                            NoteItem(
                                note = noteWithAttachments,
                                onNoteClick = {
                                    if (isSelectionModeActive) {
                                        viewModel.onEvent(BinEvent.ToggleNoteSelection(noteWithAttachments.note.id))
                                    } else {
                                        viewModel.onEvent(BinEvent.ExpandNote(noteWithAttachments.note.id))
                                    }
                                },
                                onNoteLongClick = { viewModel.onEvent(BinEvent.ToggleNoteSelection(noteWithAttachments.note.id)) },
                                isSelected = state.selectedNoteIds.contains(noteWithAttachments.note.id),
                                binnedDaysRemaining = if (noteWithAttachments.note.isBinned) {
                                    val binnedOn = noteWithAttachments.note.binnedOn
                                    if (binnedOn != null) {
                                        val daysSinceBinned = (System.currentTimeMillis() - binnedOn) / (1000 * 60 * 60 * 24)
                                        (state.autoDeleteDays - daysSinceBinned).toInt().coerceAtLeast(0)
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = state.expandedNoteId != null,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            BinnedNoteScreen(
                state = state,
                onDismiss = { viewModel.onEvent(BinEvent.CollapseNote) }
            )
        }
    }

    if (showEmptyBinDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEmptyBinDialog = false },
            title = { Text("Empty Bin") },
            text = { Text("Are you sure you want to permanently delete all notes in the bin? This action cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.onEvent(BinEvent.EmptyBin)
                        showEmptyBinDialog = false
                    }
                ) {
                    Text("Delete All", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEmptyBinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BinContextualTopAppBar(
    selectedItemCount: Int,
    onClearSelection: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeletePermanentlyClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = stringResource(id = R.string.x_selected, selectedItemCount)) },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.clear_selection))
            }
        },
        actions = {
            IconButton(onClick = onRestoreClick) {
                Icon(Icons.Default.Restore, contentDescription = stringResource(id = R.string.restore))
            }
            Box {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.delete_permanently)) },
                        onClick = {
                            onDeletePermanentlyClick()
                            showMenu = false
                        }
                    )
                }
            }
        }
    )
}
