package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.notes.NotesEvent
import kotlinx.coroutines.delay

/**
 * Enhanced color picker with circular reveal animation and scale effects.
 * Colors appear in sequence with a staggered animation for a fluid feel.
 */
@Composable
fun ColorPicker(
    colors: List<Int>,
    editingColor: Int,
    onEvent: (NotesEvent) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // No Color Option with animation
        item {
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isVisible = true }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn()
            ) {
                ColorCircle(
                    color = null,
                    isSelected = editingColor == 0,
                    onClick = { onEvent(NotesEvent.OnColorChange(0)) }
                )
            }
        }

        // Color items with staggered animation
        itemsIndexed(colors) { index, color ->
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay((index + 1) * 30L) // Stagger delay for each color
                isVisible = true
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn()
            ) {
                ColorCircle(
                    color = color,
                    isSelected = editingColor == color,
                    onClick = { onEvent(NotesEvent.OnColorChange(color)) }
                )
            }
        }
    }
}

/**
 * Individual color circle with tap animation.
 */
@Composable
private fun ColorCircle(
    color: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Scale animation on selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (color != null) Color(color) else Color.Transparent,
                CircleShape
            )
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                color = if (isSelected) {
                    if (color != null) contentColorFor(Color(color)) else MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                },
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (color == null) {
            // No color icon
            Icon(
                Icons.Outlined.FormatColorReset,
                contentDescription = "No Color",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        } else if (isSelected) {
            // Checkmark with pop animation
            val checkScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "check_scale"
            )
            
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(id = R.string.selected_color_description),
                tint = contentColorFor(backgroundColor = Color(color)),
                modifier = Modifier
                    .size(20.dp)
                    .scale(checkScale)
            )
        }
    }
}