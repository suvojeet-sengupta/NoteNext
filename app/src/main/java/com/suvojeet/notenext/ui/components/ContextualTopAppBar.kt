package com.suvojeet.notenext.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R

/**
 * A contextual top app bar that appears when one or more items are selected.
 * It provides actions relevant to the selected items, such as pinning, deleting, etc.
 * The bar and its icons animate in and out with a playful spring animation.
 *
 * @param selectedItemCount The number of items currently selected.
 * @param onClearSelection Lambda to be invoked when the selection is cleared.
 * @param onTogglePinClick Lambda for the pin/unpin action.
 * @param onReminderClick Lambda for the reminder action.
 * @param onColorClick Lambda for the color change action.
 * @param onArchiveClick Lambda for the archive action.
 * @param onDeleteClick Lambda for the delete action.
 * @param onCopyClick Lambda for the copy action.
 * @param onSendClick Lambda for the send/share action.
 * @param onLabelClick Lambda for the label action.
 * @param onMoveToProjectClick Lambda for moving items to a project.
 */
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
    onMoveToProjectClick: () -> Unit,
    onLockClick: () -> Unit,
    onSelectAllClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Animate the entire app bar's appearance with a spring effect.
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "AppBarScale"
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
                // Animate the counter change.
                AnimatedContent(
                    targetState = selectedItemCount,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()).togetherWith(
                            slideOutVertically { it } + fadeOut()
                        )
                    },
                    label = "SelectedItemCount"
                ) { count ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        // A circular badge to display the number of selected items.
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
                    contentDescription = stringResource(id = R.string.clear_selection)
                )
            },
            actions = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    // Action icons appear with a slight stagger.
                    AnimatedIconButton(
                        onClick = onTogglePinClick,
                        icon = Icons.Outlined.PushPin,
                        contentDescription = stringResource(id = R.string.pin_note),
                        delay = 50
                    )
                    AnimatedIconButton(
                        onClick = onLockClick,
                        icon = Icons.Default.Lock, // Using filled lock icon
                        contentDescription = "Lock/Unlock",
                        delay = 75
                    )
                    AnimatedIconButton(
                        onClick = onReminderClick,
                        icon = Icons.Default.Notifications,
                        contentDescription = stringResource(id = R.string.reminders),
                        delay = 100
                    )
                    AnimatedIconButton(
                        onClick = onColorClick,
                        icon = Icons.Default.Palette,
                        contentDescription = stringResource(id = R.string.toggle_color_picker),
                        delay = 150
                    )
                    AnimatedIconButton(
                        onClick = onLabelClick,
                        icon = Icons.AutoMirrored.Outlined.Label,
                        contentDescription = stringResource(id = R.string.add_label),
                        delay = 200
                    )
                    Box {
                        AnimatedIconButton(
                            onClick = { showMenu = !showMenu },
                            icon = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.more_options),
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
                                text = "Select All",
                                onClick = {
                                    onSelectAllClick()
                                    showMenu = false
                                }
                            )
                            AnimatedDropdownItem(
                                text = stringResource(id = R.string.archive),
                                onClick = {
                                    onArchiveClick()
                                    showMenu = false
                                }
                            )
                            AnimatedDropdownItem(
                                text = stringResource(id = R.string.delete),
                                onClick = {
                                    onDeleteClick()
                                    showMenu = false
                                },
                                textColor = MaterialTheme.colorScheme.error
                            )
                            AnimatedDropdownItem(
                                text = stringResource(id = R.string.make_a_copy),
                                onClick = {
                                    onCopyClick()
                                    showMenu = false
                                }
                            )
                            // "Share" is only shown for a single selection.
                            if (selectedItemCount == 1) {
                                AnimatedDropdownItem(
                                    text = stringResource(id = R.string.share),
                                    onClick = {
                                        onSendClick()
                                        showMenu = false
                                    }
                                )
                            }
                            AnimatedDropdownItem(
                                text = stringResource(id = R.string.move_to_project),
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

/**
 * An icon button that animates its appearance and provides a press effect.
 *
 * @param onClick The lambda to be invoked on click.
 * @param icon The vector graphic to be displayed.
 * @param contentDescription The accessibility description.
 * @param delay The delay in milliseconds before the button starts its entrance animation.
 */
@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    delay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    // Trigger the entrance animation after the specified delay.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    // Animate the scale for the entrance.
    val entranceScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "IconEntranceScale"
    )

    // Animate the scale for the press effect.
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "IconPressScale"
    )

    IconButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier.scale(entranceScale * pressScale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }

    // Reset the press state after a short delay to create a 'pop' effect.
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * A dropdown menu item with a subtle size animation.
 */
@Composable
private fun AnimatedDropdownItem(
    text: String,
    onClick: () -> Unit,
    textColor: Color = Color.Unspecified
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        },
        onClick = onClick,
        modifier = Modifier.animateContentSize() // Animates size changes.
    )
}
