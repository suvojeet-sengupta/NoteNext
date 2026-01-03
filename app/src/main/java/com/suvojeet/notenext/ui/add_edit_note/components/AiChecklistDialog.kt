package com.suvojeet.notenext.ui.add_edit_note.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val PREFS_NAME = "ai_checklist_prefs"
private const val KEY_PROMPT_HISTORY = "prompt_history"
private const val MAX_HISTORY_SIZE = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChecklistSheet(
    isVisible: Boolean,
    isGenerating: Boolean,
    generatedItems: List<String>,
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit,
    onInsert: (List<String>) -> Unit,
    onRegenerate: (String) -> Unit
) {
    val context = LocalContext.current
    var topic by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetValue ->
            // Only allow dismissing from expanded state, prevents accidental swipe
            sheetValue != SheetValue.Hidden
        }
    )
    
    // Local state for editable items
    var editableItems by remember { mutableStateOf(listOf<String>()) }
    
    // Prompt history
    var promptHistory by remember { mutableStateOf(loadPromptHistory(context)) }
    
    // Sync editableItems with generatedItems when new items arrive
    LaunchedEffect(generatedItems) {
        if (generatedItems.isNotEmpty()) {
            editableItems = generatedItems.toList()
        }
    }
    
    // Check network connectivity
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    // Save prompt to history when generating
    fun saveAndGenerate(prompt: String) {
        if (prompt.isNotBlank()) {
            if (!isNetworkAvailable()) {
                Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
                return
            }
            promptHistory = savePromptToHistory(context, prompt, promptHistory)
            onGenerate(prompt)
        }
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Help me create a list",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = "Describe the list that you want to create. Try a packing list, to-do list or something else.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Prompt History Chips (only show if no items generated yet)
                if (promptHistory.isNotEmpty() && editableItems.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            promptHistory.forEach { prompt ->
                                SuggestionChip(
                                    onClick = { 
                                        topic = prompt
                                        saveAndGenerate(prompt)
                                    },
                                    label = { 
                                        Text(
                                            text = prompt,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        ) 
                                    },
                                    modifier = Modifier.widthIn(max = 150.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Input field with optional regenerate button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            placeholder = { Text("Back-to-school shopping list for a 10-year-old") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                        
                        if (editableItems.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    editableItems = emptyList()
                                    saveAndGenerate(topic) 
                                },
                                enabled = topic.isNotBlank() && !isGenerating
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Show preview if items are generated
                AnimatedContent(
                    targetState = editableItems.isNotEmpty(),
                    label = "preview"
                ) { hasItems ->
                    if (hasItems) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Generated list",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap to edit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .heightIn(max = 250.dp)
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    itemsIndexed(editableItems, key = { index, _ -> index }) { index, item ->
                                        // Staggered animation for each item
                                        var isItemVisible by remember { mutableStateOf(false) }
                                        LaunchedEffect(Unit) {
                                            kotlinx.coroutines.delay(index * 50L) // Stagger delay
                                            isItemVisible = true
                                        }
                                        
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isItemVisible,
                                            enter = androidx.compose.animation.slideInHorizontally(
                                                initialOffsetX = { 100 },
                                                animationSpec = androidx.compose.animation.core.spring(
                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                                )
                                            ) + androidx.compose.animation.fadeIn()
                                        ) {
                                            EditableItemRow(
                                                text = item,
                                                onTextChange = { newText ->
                                                    editableItems = editableItems.toMutableList().apply {
                                                        this[index] = newText
                                                    }
                                                },
                                                onDelete = {
                                                    editableItems = editableItems.toMutableList().apply {
                                                        removeAt(index)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NoteNext may display inaccurate information, so double-check its responses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (editableItems.isEmpty()) {
                        // Create button
                        Button(
                            onClick = { saveAndGenerate(topic) },
                            enabled = topic.isNotBlank() && !isGenerating,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating...")
                            } else {
                                Text("Create")
                            }
                        }
                    } else {
                        // Insert button
                        Button(
                            onClick = {
                                onInsert(editableItems.filter { it.isNotBlank() })
                                onDismiss()
                            },
                            enabled = editableItems.any { it.isNotBlank() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("Insert ${editableItems.count { it.isNotBlank() }} items")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableItemRow(
    text: String,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = false,
            maxLines = 3  // Prevent overflow for long items
        )
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove item",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper functions for SharedPreferences
private fun loadPromptHistory(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val historyString = prefs.getString(KEY_PROMPT_HISTORY, "") ?: ""
    return if (historyString.isBlank()) emptyList() else historyString.split("|||")
}

private fun savePromptToHistory(context: Context, prompt: String, currentHistory: List<String>): List<String> {
    // Remove duplicate if exists, add to front, limit to MAX_HISTORY_SIZE
    val newHistory = (listOf(prompt) + currentHistory.filter { it != prompt }).take(MAX_HISTORY_SIZE)
    
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_PROMPT_HISTORY, newHistory.joinToString("|||")).apply()
    
    return newHistory
}
