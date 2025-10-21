package com.suvojeet.notenext.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.notenext.R

import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.ui.notes.SortType
import androidx.compose.material.icons.filled.ViewList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    onSearchActiveChange: (Boolean) -> Unit,
    onLayoutToggleClick: () -> Unit,
    onSortClick: () -> Unit,
    layoutType: LayoutType,
    sortMenuExpanded: Boolean,
    onSortMenuDismissRequest: () -> Unit,
    onSortOptionClick: (SortType) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .clickable { onSearchActiveChange(true) }, // Make the whole card clickable
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            Text("Search Notes", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onLayoutToggleClick) {
                    Icon(
                        imageVector = if (layoutType == LayoutType.GRID) Icons.Default.ViewList else Icons.Default.ViewModule,
                        contentDescription = "Layout Toggle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSortClick) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

@Preview
@Composable
fun SearchBarPreview() {
    var showSortMenu by remember { mutableStateOf(false) }
    SearchBar(
        onSearchActiveChange = { /* Do nothing for preview */ },
        onLayoutToggleClick = {},
        onSortClick = { showSortMenu = true },
        layoutType = LayoutType.GRID,
        sortMenuExpanded = showSortMenu,
        onSortMenuDismissRequest = { showSortMenu = false },
        onSortOptionClick = {}
    )
}
