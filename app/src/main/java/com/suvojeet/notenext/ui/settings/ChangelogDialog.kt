package com.suvojeet.notenext.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.suvojeet.notenext.R

@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "What's New",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.2.6",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Changelog List
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Text(
                            text = "January 3, 2026",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(getChangelogItems()) { item ->
                        ChangelogItem(item)
                    }
                }

                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, end = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(onClick = onDismiss) {
                        Text("Awesome!")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogItem(item: ChangelogData) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ChangelogData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

private fun getChangelogItems(): List<ChangelogData> {
    return listOf(
        ChangelogData(
            icon = Icons.Default.Palette,
            title = "Theme-Adaptive Colors",
            description = "Notes now automatically adjust their colors for Light and Dark modes.",
            color = Color(0xFFE91E63) // Pink
        ),
        ChangelogData(
            icon = Icons.Default.Security,
            title = "Enhanced Security",
            description = "Backups now clearly show AES-256 encryption status. Dialogs are redesigned for clarity.",
            color = Color(0xFF4CAF50) // Green
        ),
        ChangelogData(
            icon = Icons.Default.Fullscreen,
            title = "Immersive Editing",
            description = "Note editing is now full-screen with matching status and navigation bars.",
            color = Color(0xFF2196F3) // Blue
        ),
         ChangelogData(
            icon = Icons.Default.Archive,
            title = "Redesigned Archive",
            description = "Archive screen now uses a staggered grid layout with rich link previews.",
            color = Color(0xFFFF9800) // Orange
        ),
        ChangelogData(
            icon = Icons.Default.Update,
            title = "Check for Updates",
            description = "Easily check for new app versions directly from Settings.",
            color = Color(0xFF9C27B0) // Purple
        )
    )
}
