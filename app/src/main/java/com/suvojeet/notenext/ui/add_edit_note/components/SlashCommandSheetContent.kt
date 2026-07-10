@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class SlashCommand(
    val title: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@Composable
fun SlashCommandSheetContent(
    onDismissRequest: () -> Unit,
    onCommandSelected: (SlashCommand) -> Unit
) {
    val commands = listOf(
        SlashCommand("Heading 1", Icons.Default.FormatSize) { /* Handle in parent */ },
        SlashCommand("Checklist", Icons.Default.CheckBox) { /* Handle in parent */ },
        SlashCommand("Bulleted List", Icons.Default.List) { /* Handle in parent */ },
        SlashCommand("Image", Icons.Default.Image) { /* Handle in parent */ }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Insert",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
        )

        LazyColumn {
            items(
                items = commands,
                key = { it.title }
            ) { command ->
                ListItem(
                    headlineContent = { Text(command.title) },
                    leadingContent = { 
                        Icon(
                            imageVector = command.icon, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onCommandSelected(command)
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
