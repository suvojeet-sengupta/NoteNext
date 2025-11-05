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

import androidx.compose.material.icons.filled.CreateNewFolder

import com.suvojeet.notenext.ui.settings.ThemeMode

@Composable
fun MultiActionFab(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNoteClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onProjectClick: () -> Unit,
    showProjectButton: Boolean = true,
    themeMode: ThemeMode
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300), label = ""
    )

    var showProject by remember { mutableStateOf(false) }
    var showChecklist by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            showNote = true
            kotlinx.coroutines.delay(50)
            showChecklist = true
            kotlinx.coroutines.delay(50)
            showProject = true
        } else {
            showProject = false
            kotlinx.coroutines.delay(50)
            showChecklist = false
            kotlinx.coroutines.delay(50)
            showNote = false
        }
    }

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = showProject && showProjectButton,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            FabItem(
                icon = Icons.Default.CreateNewFolder,
                label = "Project",
                onClick = {
                    onProjectClick()
                    onExpandedChange(false)
                },
                themeMode = themeMode
            )
        }

        AnimatedVisibility(
            visible = showChecklist,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            FabItem(
                icon = Icons.Default.CheckBox,
                label = "Checklist",
                onClick = {
                    onChecklistClick()
                    onExpandedChange(false)
                },
                themeMode = themeMode
            )
        }

        AnimatedVisibility(
            visible = showNote,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            FabItem(
                icon = Icons.Default.Note,
                label = "Note",
                onClick = {
                    onNoteClick()
                    onExpandedChange(false)
                },
                themeMode = themeMode
            )
        }

        FloatingActionButton(
            onClick = {
                pressed = true
                onExpandedChange(!isExpanded)
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.scale(pressScale)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.rotate(rotation)
            )
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

@Composable
private fun FabItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    themeMode: ThemeMode
) {
    val cardColor = if (themeMode == ThemeMode.AMOLED) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
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