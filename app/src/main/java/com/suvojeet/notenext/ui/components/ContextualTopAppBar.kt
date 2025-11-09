package com.suvojeet.notenext.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualTopAppBar(
    selectedItemCount: Int,
    onClearSelection: () -> Unit,
    onTogglePinClick: () -> Unit,
    onReminderClick: () -> Unit,
    onColorClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onSendClick: () -> Unit,
    onLabelClick: () -> Unit,
    onMoveToProjectClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Animation for entrance
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        TopAppBar(
            title = {
                AnimatedContent(
                    targetState = selectedItemCount,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()).togetherWith(
                            slideOutVertically { it } + fadeOut()
                        )
                    },
                    label = "count"
                ) { count ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        // Animated counter badge
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                    }
                }
            },
            navigationIcon = {
                AnimatedIconButton(
                    onClick = onClearSelection,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Clear selection"
                )
            },
            actions = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    AnimatedIconButton(
                        onClick = onTogglePinClick,
                        icon = Icons.Outlined.PushPin,
                        contentDescription = "Pin note",
                        delay = 50
                    )
                    AnimatedIconButton(
                        onClick = onReminderClick,
                        icon = Icons.Default.Notifications,
                        contentDescription = "Set reminder",
                        delay = 100
                    )
                    AnimatedIconButton(
                        onClick = onColorClick,
                        icon = Icons.Default.Palette,
                        contentDescription = "Change color",
                        delay = 150
                    )
                    AnimatedIconButton(
                        onClick = onLabelClick,
                        icon = Icons.AutoMirrored.Outlined.Label,
                        contentDescription = "Add label",
                        delay = 200
                    )
                    Box {
                        AnimatedIconButton(
                            onClick = { showMenu = !showMenu },
                            icon = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            delay = 250
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .widthIn(min = 180.dp)
                        ) {
                            AnimatedDropdownItem(
                                text = "Archive",
                                onClick = {
                                    onArchiveClick()
                                    showMenu = false
                                }
                            )
                            AnimatedDropdownItem(
                                text = "Delete",
                                onClick = {
                                    onDeleteClick()
                                    showMenu = false
                                },
                                textColor = MaterialTheme.colorScheme.error
                            )
                            AnimatedDropdownItem(
                                text = "Make a copy",
                                onClick = {
                                    onCopyClick()
                                    showMenu = false
                                }
                            )
                            if (selectedItemCount == 1) {
                                AnimatedDropdownItem(
                                    text = "Share",
                                    onClick = {
                                        onSendClick()
                                        showMenu = false
                                    }
                                )
                            }
                            AnimatedDropdownItem(
                                text = "Move to project",
                                onClick = {
                                    onMoveToProjectClick()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    delay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )

    IconButton(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = Modifier.scale(scale * pressScale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

@Composable
private fun AnimatedDropdownItem(
    text: String,
    onClick: () -> Unit,
    textColor: Color = Color.Unspecified
) {
    var hovered by remember { mutableStateOf(false) }

    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        },
        onClick = onClick,
        modifier = Modifier.animateContentSize()
    )
}
