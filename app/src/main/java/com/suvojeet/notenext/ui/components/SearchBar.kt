package com.suvojeet.notenext.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    modifier: Modifier = Modifier
) {
    var isSearchFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSearchFocused) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                )
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val scale by animateFloatAsState(
                targetValue = if (isSearchFocused) 1.1f else 1f,
                animationSpec = spring(dampingRatio = 0.5f),
                label = "searchIconScale"
            )

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .scale(scale)
            )

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search notes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )

            AnimatedVisibility(
                visible = searchQuery.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            CompactViewModeToggle(
                currentMode = layoutType,
                onModeChange = onLayoutToggleClick
            )

            Box {
                CompactSortButton(onClick = onSortClick)
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = onSortMenuDismissRequest
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort by date created") },
                        onClick = {
                            onSortOptionClick(SortType.DATE_CREATED)
                            onSortMenuDismissRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by date modified") },
                        onClick = {
                            onSortOptionClick(SortType.DATE_MODIFIED)
                            onSortMenuDismissRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by title") },
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

@Composable
fun CompactViewModeToggle(
    currentMode: LayoutType,
    onModeChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
        animationSpec = tween(300),
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

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun CompactSortButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "sortButtonScale"
    )

    FilledTonalButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.Sort,
            contentDescription = "Sort",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Sort",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
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
    var searchQuery by remember { mutableStateOf("") }
    var layoutType by remember { mutableStateOf(LayoutType.GRID) }
    var showSortMenu by remember { mutableStateOf(false) }

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
        onSortOptionClick = {}
    )
}