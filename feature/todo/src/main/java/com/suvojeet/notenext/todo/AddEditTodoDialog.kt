@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.core.R
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.TodoSubtask
import com.suvojeet.notenext.ui.components.springPress
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddEditTodoDialog(
    editingTodo: TodoItem?,
    initialSubtasks: List<TodoSubtask>,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?, Long?, Int?, List<TodoSubtask>) -> Unit,
    onDraftChange: (String) -> Unit = {}
) {
    var title by remember { mutableStateOf(editingTodo?.title ?: "") }
    var description by remember { mutableStateOf(editingTodo?.description ?: "") }
    var priority by remember { mutableIntStateOf(editingTodo?.priority ?: 0) }
    var dueDate by remember { mutableStateOf(editingTodo?.dueDate) }
    var reminderTime by remember { mutableStateOf(editingTodo?.reminderTime) }
    var selectedProjectId by remember { mutableStateOf(editingTodo?.projectId) }
    
    var localSubtasks by remember { mutableStateOf(initialSubtasks) }
    var newSubtaskText by remember { mutableStateOf("") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showProjectPicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(title, description) {
        if (editingTodo == null) {
            onDraftChange(if (title.isBlank()) description else "$title\n$description")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = if (editingTodo == null) stringResource(id = R.string.add_todo) else stringResource(id = R.string.todo_edit_task),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.todo_description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2
                )
                
                Column {
                    Text(
                        text = stringResource(id = R.string.priority),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PriorityChip(
                            label = stringResource(id = R.string.priority_low),
                            selected = priority == 0,
                            onClick = { priority = 0 },
                            color = TodoPriorityColors.Low,
                            modifier = Modifier.weight(1f)
                        )
                        PriorityChip(
                            label = stringResource(id = R.string.todo_priority_med_short),
                            selected = priority == 1,
                            onClick = { priority = 1 },
                            color = TodoPriorityColors.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        PriorityChip(
                            label = stringResource(id = R.string.priority_high),
                            selected = priority == 2,
                            onClick = { priority = 2 },
                            color = TodoPriorityColors.High,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Subtasks Section
                Column {
                    Text(
                        text = stringResource(id = R.string.todo_subtasks),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    localSubtasks.forEachIndexed { index, subtask ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = subtask.isChecked,
                                onCheckedChange = { checked ->
                                    localSubtasks = localSubtasks.mapIndexed { i, s ->
                                        if (i == index) s.copy(isChecked = checked) else s
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subtask.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    localSubtasks = localSubtasks.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text(stringResource(id = R.string.todo_add_subtask_placeholder), fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        scope.launch {
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                },
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true,
                            trailingIcon = {
                                if (newSubtaskText.isNotBlank()) {
                                    IconButton(onClick = {
                                        localSubtasks = localSubtasks + TodoSubtask(
                                            text = newSubtaskText,
                                            todoId = editingTodo?.id ?: 0,
                                            position = localSubtasks.size
                                        )
                                        newSubtaskText = ""
                                    }) {
                                        Icon(Icons.Default.Add, null)
                                    }
                                }
                            }
                        )
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f).springPress(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (dueDate != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dueDate?.let {
                                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
                                } ?: stringResource(id = R.string.due_date),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f).springPress(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (reminderTime != null) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f) else Color.Transparent
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (reminderTime != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reminderTime?.let {
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
                                } ?: stringResource(id = R.string.todo_reminder),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (reminderTime != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedCard(
                        onClick = { showProjectPicker = true },
                        modifier = Modifier.weight(1f).springPress(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (selectedProjectId != null) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f) else Color.Transparent
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (selectedProjectId != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val projectFallback = stringResource(id = R.string.todo_project)
                            Text(
                                text = selectedProjectId?.let { id ->
                                    projects.find { it.id == id }?.name ?: projectFallback
                                } ?: projectFallback,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                color = if (selectedProjectId != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        onSave(title, description, priority, dueDate, reminderTime, selectedProjectId, localSubtasks) 
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.springPress(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(id = R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = MaterialTheme.shapes.extraLarge,
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) { Text(stringResource(id = R.string.ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) { Text(stringResource(id = R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            reminderTime?.let { timeInMillis = it }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selCal = Calendar.getInstance()
                        val now = Calendar.getInstance()
                        selCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 
                                   timePickerState.hour, timePickerState.minute, 0)
                        selCal.set(Calendar.MILLISECOND, 0)
                        
                        if (selCal.timeInMillis <= System.currentTimeMillis()) {
                            selCal.add(Calendar.DAY_OF_MONTH, 1)
                        }
                        
                        reminderTime = selCal.timeInMillis
                        showTimePicker = false
                    }
                ) { Text(stringResource(id = R.string.ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reminderTime = null
                        showTimePicker = false
                    }
                ) { Text(stringResource(id = R.string.todo_clear)) }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    if (showProjectPicker) {
        AlertDialog(
            onDismissRequest = { showProjectPicker = false },
            title = { Text(stringResource(id = R.string.todo_select_project)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ListItem(
                        headlineContent = { Text(stringResource(id = R.string.todo_no_project)) },
                        modifier = Modifier.clickable { 
                            selectedProjectId = null
                            showProjectPicker = false
                        },
                        trailingContent = { if (selectedProjectId == null) Icon(Icons.Default.Check, null) }
                    )
                    projects.forEach { project ->
                        ListItem(
                            headlineContent = { Text(project.name) },
                            modifier = Modifier.clickable { 
                                selectedProjectId = project.id
                                showProjectPicker = false
                            },
                            trailingContent = { if (selectedProjectId == project.id) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectPicker = false }) { Text(stringResource(id = R.string.todo_close)) }
            }
        )
    }
}

@Composable
private fun PriorityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = color,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.springPress()
    )
}
