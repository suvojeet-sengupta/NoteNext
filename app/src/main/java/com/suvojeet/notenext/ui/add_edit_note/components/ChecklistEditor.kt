package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.roundToInt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState

/**
 * A composable for editing a checklist with fluid animations,
 * Swipe-to-Delete, and Collapsible Checked Items section.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.ChecklistEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    isCheckedItemsExpanded: Boolean,
    onToggleCheckedItems: () -> Unit
) {
    val uncheckedItems = state.editingChecklist.filter { !it.isChecked }.sortedBy { it.position }
    val checkedItems = state.editingChecklist.filter { it.isChecked }.sortedBy { it.position }

    // Unchecked Items with staggered animation
    itemsIndexed(uncheckedItems, key = { _, item -> item.id }) { index, item ->
        val dragOffset = remember { mutableStateOf(0f) }
        val isDragging = dragOffset.value != 0f
        
        val currentUncheckedItems by rememberUpdatedState(uncheckedItems)
        val currentIndex by rememberUpdatedState(index)
        
        val dragModifier = Modifier
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset.value += dragAmount.y
                        
                        val threshold = 100f 
                        val items = currentUncheckedItems
                        val i = currentIndex

                        if (dragOffset.value > threshold) {
                            if (i < items.lastIndex) {
                                onEvent(NotesEvent.SwapChecklistItems(item.id, items[i + 1].id))
                                dragOffset.value -= threshold 
                            }
                        } else if (dragOffset.value < -threshold) {
                            if (i > 0) {
                                onEvent(NotesEvent.SwapChecklistItems(item.id, items[i - 1].id))
                                dragOffset.value += threshold
                            }
                        }
                    },
                    onDragEnd = { dragOffset.value = 0f },
                    onDragCancel = { dragOffset.value = 0f }
                )
            }

        // Spring animation entry for each item
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialOffsetY = { -40 }
            ) + fadeIn(animationSpec = tween(300))
        ) {
            ChecklistItemRow(
                item = item,
                inputValue = state.checklistInputValues[item.id],
                onEvent = onEvent,
                isChecked = false,
                isNewlyAdded = state.newlyAddedChecklistItemId == item.id,
                modifier = Modifier
                    .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            scaleX = 1.03f
                            scaleY = 1.03f
                            shadowElevation = 8f
                        }
                    },
                dragModifier = dragModifier
            )
        }
    }

    // Add Item Button with pulse effect
    item {
        var isPulsing by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPulsing) 1.05f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "pulse"
        )
        
        TextButton(
            onClick = { 
                isPulsing = true
                onEvent(NotesEvent.AddChecklistItem) 
            },
            modifier = Modifier
                .padding(vertical = 4.dp)
                .padding(horizontal = 16.dp)
                .scale(scale)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(id = R.string.add_item))
        }
        
        LaunchedEffect(isPulsing) {
            if (isPulsing) {
                kotlinx.coroutines.delay(150)
                isPulsing = false
            }
        }
    }

    // Checked Items Header with animated arrow rotation
    if (checkedItems.isNotEmpty()) {
        item {
            val rotationAngle by animateFloatAsState(
                targetValue = if (isCheckedItemsExpanded) 180f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "arrow_rotation"
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleCheckedItems,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isCheckedItemsExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Checked items (${checkedItems.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                    
                TextButton(onClick = { onEvent(NotesEvent.DeleteAllCheckedItems) }) {
                    Text("Delete all", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Checked Items with collapse animation
        items(checkedItems, key = { it.id }) { item ->
            AnimatedVisibility(
                visible = isCheckedItemsExpanded,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeOut()
            ) {
                ChecklistItemRow(
                    item = item,
                    inputValue = state.checklistInputValues[item.id],
                    onEvent = onEvent,
                    isChecked = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    inputValue: TextFieldValue?,
    onEvent: (NotesEvent) -> Unit,
    isChecked: Boolean,
    isNewlyAdded: Boolean = false,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onEvent(NotesEvent.DeleteChecklistItem(item.id))
                true
            } else {
                false
            }
        }
    )
    
    // Animated checkbox scale
    val checkScale by animateFloatAsState(
        targetValue = if (isChecked) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check_scale"
    )
    
    // Animated text color and strikethrough
    val textColor by animateColorAsState(
        targetValue = if (isChecked) 
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
        else 
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300),
        label = "text_color"
    )
    
    // Auto-focus for newly added items
    LaunchedEffect(isNewlyAdded) {
        if (isNewlyAdded) {
            focusRequester.requestFocus()
            onEvent(NotesEvent.ClearNewlyAddedChecklistItemId)
        }
    }

    SwipeToDismissBox(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        state = dismissState,
        backgroundContent = {
            val color = Color(0xFFFF5252)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 6.dp)
                .padding(start = (item.level * 32).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .size(24.dp)
                    .then(dragModifier),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ) 
            
            // Animated Checkbox
            Box(modifier = Modifier.scale(checkScale)) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { checked ->
                        onEvent(NotesEvent.OnChecklistItemCheckedChange(item.id, checked))
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            // Text field with animated color
            BasicTextField(
                value = inputValue ?: TextFieldValue(item.text),
                onValueChange = { textFieldValue: TextFieldValue ->
                     onEvent(NotesEvent.OnChecklistItemValueChange(item.id, textFieldValue))
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(start = 8.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onEvent(NotesEvent.OnChecklistItemFocus(item.id))
                        }
                    },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = textColor,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = false 
            )
            
            // Delete Button with fade
            IconButton(
                onClick = { onEvent(NotesEvent.DeleteChecklistItem(item.id)) },
                modifier = Modifier.alpha(0.7f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
