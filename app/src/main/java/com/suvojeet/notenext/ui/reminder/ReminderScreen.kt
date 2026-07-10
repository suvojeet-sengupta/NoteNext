@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.reminder

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Notifications
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
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.springPress
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReminderScreen(
    onBackClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    reminderViewModel: ReminderViewModel = hiltViewModel()
) {
    val allReminders by reminderViewModel.allReminders.collectAsStateWithLifecycle()
    val upcomingReminders by reminderViewModel.upcomingReminders.collectAsStateWithLifecycle()
    val elapsedReminders by reminderViewModel.elapsedReminders.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Upcoming, 2: Elapsed

    val currentList = remember(selectedTab, allReminders, upcomingReminders, elapsedReminders) {
        when (selectedTab) {
            0 -> allReminders
            1 -> upcomingReminders
            2 -> elapsedReminders
            else -> allReminders
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(id = R.string.reminders_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text(stringResource(id = R.string.rem_tab_all)) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.springPress()
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text(stringResource(id = R.string.rem_tab_upcoming)) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.springPress()
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text(stringResource(id = R.string.rem_tab_elapsed)) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.springPress()
                )
            }

            if (currentList.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Notifications,
                    message = stringResource(id = R.string.rem_empty_message)
                )
            } else {
                ExpressiveSection(
                    title = when(selectedTab) {
                        0 -> stringResource(id = R.string.rem_section_all)
                        1 -> stringResource(id = R.string.rem_tab_upcoming)
                        2 -> stringResource(id = R.string.rem_tab_elapsed)
                        else -> stringResource(id = R.string.rem_section_default)
                    },
                    description = stringResource(id = R.string.rem_section_desc)
                ) {
                    SettingsGroupCard {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = currentList,
                                key = { it.id }
                            ) { note ->
                                ReminderItem(
                                    note = note, 
                                    onClick = { onNoteClick(note) },
                                    onDeleteClick = { reminderViewModel.deleteReminder(note) }
                                )
                                if (currentList.last() != note) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItem(
    note: Note,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .springPress()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifEmpty { stringResource(id = R.string.rem_no_title) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                 maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            note.reminderTime?.let { time ->
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                Text(
                    text = stringResource(id = R.string.rem_next_label, sdf.format(Date(time))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "1",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        IconButton(onClick = onDeleteClick, modifier = Modifier.springPress()) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(id = R.string.rem_delete_cd),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
