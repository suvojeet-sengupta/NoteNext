package com.suvojeet.notenext.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.ui.notes.SortType
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.ArrowBack


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    layoutType: LayoutType,
    onLayoutToggleClick: () -> Unit,
    onSortClick: () -> Unit,
    sortMenuExpanded: Boolean,
    onSortMenuDismissRequest: () -> Unit,
    onSortOptionClick: (SortType) -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSearchFocused) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search/Back Icon with smooth animation
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) + 
                    scaleIn(initialScale = 0.8f) togetherWith
                    fadeOut(animationSpec = tween(200)) + 
                    scaleOut(targetScale = 0.8f)
                },
                label = "searchIconTransition"
            ) { active ->
                if (active) {
                    IconButton(
                        onClick = { 
                            onSearchActiveChange(false)
                            onSearchQueryChange("")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSearchFocused) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "searchIconScale"
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(iconScale)
                    )
                }
            }

            // Search TextField
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                        if (focusState.isFocused) onSearchActiveChange(true)
                    },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )

            // Clear button with animation
            AnimatedVisibility(
                visible = isSearchActive && searchQuery.isNotEmpty(),
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Controls with slide animation
            AnimatedVisibility(
                visible = !isSearchActive,
                enter = slideInHorizontally(
                    initialOffsetX = { it / 2 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { it / 2 },
                    animationSpec = tween(200)
                ) + fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Animated divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    CompactViewModeToggle(
                        currentMode = layoutType,
                        onModeChange = onLayoutToggleClick
                    )

                    Box {
                        CompactSortButton(onClick = onSortClick)
                        
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = onSortMenuDismissRequest,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            SortMenuItem(
                                text = "Date Created",
                                icon = Icons.Default.CalendarToday,
                                onClick = {
                                    onSortOptionClick(SortType.DATE_CREATED)
                                    onSortMenuDismissRequest()
                                }
                            )
                            SortMenuItem(
                                text = "Date Modified",
                                icon = Icons.Default.Update,
                                onClick = {
                                    onSortOptionClick(SortType.DATE_MODIFIED)
                                    onSortMenuDismissRequest()
                                }
                            )
                            SortMenuItem(
                                text = "Title",
                                icon = Icons.Default.SortByAlpha,
                                onClick = {
                                    onSortOptionClick(SortType.TITLE)
                                    onSortMenuDismissRequest()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        onClick = onClick
    )
}

@Composable
fun CompactViewModeToggle(
    currentMode: LayoutType,
    onModeChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        CompactViewModeButton(
            icon = Icons.Default.ViewList,
            isSelected = currentMode == LayoutType.LIST,
            onClick = { if (currentMode != LayoutType.LIST) onModeChange() }
        )

        CompactViewModeButton(
            icon = Icons.Default.ViewModule,
            isSelected = currentMode == LayoutType.GRID,
            onClick = { if (currentMode != LayoutType.GRID) onModeChange() }
        )
    }
}

@Composable
fun CompactViewModeButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "buttonContent"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .scale(scale)
            .clip(RoundedCornerShape(11.dp))
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CompactSortButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "sortButtonScale"
    )

    FilledTonalButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .scale(scale)
            .height(34.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.Sort,
            contentDescription = "Sort",
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Sort",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    MaterialTheme {
        var searchQuery by remember { mutableStateOf("") }
        var layoutType by remember { mutableStateOf(LayoutType.GRID) }
        var showSortMenu by remember { mutableStateOf(false) }
        var isSearchActive by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                layoutType = layoutType,
                onLayoutToggleClick = {
                    layoutType = if (layoutType == LayoutType.GRID) LayoutType.LIST else LayoutType.GRID
                },
                onSortClick = { showSortMenu = !showSortMenu },
                sortMenuExpanded = showSortMenu,
                onSortMenuDismissRequest = { showSortMenu = false },
                onSortOptionClick = {},
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it }
            )
            
            // Preview state
            if (isSearchActive) {
                Text(
                    "Search Active: ${searchQuery.ifEmpty { "Empty" }}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
