package com.suvojeet.notenext.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import androidx.compose.material.icons.filled.CreateNewFolder

import com.suvojeet.notenext.ui.theme.ThemeMode

/**
 * A Floating Action Button (FAB) that expands to reveal multiple action items (Note, Checklist, Project).
 * Each action item appears with a staggered animation.
 *
 * @param isExpanded Boolean state indicating whether the FAB is expanded.
 * @param onExpandedChange Lambda to be invoked when the expansion state changes.
 * @param onNoteClick Lambda to be invoked when the "Note" action item is clicked.
 * @param onChecklistClick Lambda to be invoked when the "Checklist" action item is clicked.
 * @param onProjectClick Lambda to be invoked when the "Project" action item is clicked.
 * @param showProjectButton Boolean to control the visibility of the "Project" action item.
 * @param themeMode The current theme mode, used for styling the action items.
 */
@Composable
fun MultiActionFab(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNoteClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onProjectClick: () -> Unit,
    showProjectButton: Boolean = true,
    themeMode: ThemeMode,
    isScrollExpanded: Boolean = true // New parameter for scroll awareness
) {
    // Animate the rotation of the main FAB icon (Add/Close).
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300), label = "FabIconRotation"
    )

    // State variables to control the staggered visibility of action items.
    var showProject by remember { mutableStateOf(false) }
    var showChecklist by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }

    // LaunchedEffect to manage the staggered appearance/disappearance of action items.
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            // When expanding, show items with a slight delay between each.
            showNote = true
            kotlinx.coroutines.delay(50)
            showChecklist = true
            kotlinx.coroutines.delay(50)
            showProject = true
        } else {
            // When collapsing, hide items with a slight delay in reverse order.
            showProject = false
            kotlinx.coroutines.delay(50)
            showChecklist = false
            kotlinx.coroutines.delay(50)
            showNote = false
        }
    }

    // State and animation for the main FAB's press effect.
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "MainFabPressScale"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Project action item.
        AnimatedVisibility(
            visible = showProject && showProjectButton,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            FabItem(
                icon = Icons.Default.CreateNewFolder,
                label = stringResource(id = R.string.projects),
                onClick = {
                    onProjectClick()
                    onExpandedChange(false) // Collapse FAB after action.
                },
                themeMode = themeMode
            )
        }

        // Checklist action item.
        AnimatedVisibility(
            visible = showChecklist,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            FabItem(
                icon = Icons.Default.CheckBox,
                label = stringResource(id = R.string.checklist),
                onClick = {
                    onChecklistClick()
                    onExpandedChange(false) // Collapse FAB after action.
                },
                themeMode = themeMode
            )
        }

        // Note action item.
        AnimatedVisibility(
            visible = showNote,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            FabItem(
                icon = Icons.Default.Note,
                label = stringResource(id = R.string.note),
                onClick = {
                    onNoteClick()
                    onExpandedChange(false) // Collapse FAB after action.
                },
                themeMode = themeMode
            )
        }

        // Main Floating Action Button (Extended or Regular)
        androidx.compose.material3.ExtendedFloatingActionButton(
            text = { 
                 AnimatedVisibility(visible = isScrollExpanded && !isExpanded) {
                     Text(text = stringResource(id = R.string.add))
                 }
            },
            icon = {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add),
                    modifier = Modifier.rotate(rotation) // Apply rotation animation.
                )
            },
            onClick = {
                pressed = true
                onExpandedChange(!isExpanded)
            },
            expanded = isScrollExpanded && !isExpanded, // Only expand if scrolled up AND menu is closed
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.scale(pressScale) // Apply press animation.
        )
    }

    // Reset the press state after a short delay to create a 'pop' effect.
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

/**
 * A single item displayed within the [MultiActionFab] when expanded.
 *
 * @param icon The icon to display for the action item.
 * @param label The text label for the action item.
 * @param onClick Lambda to be invoked when the item is clicked.
 * @param themeMode The current theme mode, used to determine the card's background and border.
 */
@Composable
private fun FabItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    themeMode: ThemeMode
) {
    // Determine card color based on theme mode.
    val cardColor = if (themeMode == ThemeMode.AMOLED) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    // Determine border style based on theme mode.
    val border = if (themeMode == ThemeMode.AMOLED) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    } else {
        null
    }

    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}