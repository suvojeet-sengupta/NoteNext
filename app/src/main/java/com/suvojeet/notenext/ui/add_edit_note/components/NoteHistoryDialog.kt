@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.ui.components.springPress
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoteHistoryDialog(
    versions: List<NoteVersion>,
    isLocked: Boolean,
    onVersionClick: (NoteVersion) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(id = R.string.history)) },
        text = {
            if (versions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No history available")
                }
            } else {
                val sortedVersions = remember(versions) {
                    versions.sortedByDescending { it.timestamp }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = sortedVersions,
                        key = { _, v -> v.timestamp }
                    ) { index, version ->
                        VersionItem(
                            version = version,
                            isLocked = isLocked,
                            isCurrent = index == 0,
                            onClick = { onVersionClick(version) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.ok))
            }
        }
    )
}

@Composable
private fun VersionItem(version: NoteVersion, isLocked: Boolean, isCurrent: Boolean, onClick: () -> Unit) {
    val date = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault()).format(Date(version.timestamp))

    val decryptedVersion = androidx.compose.runtime.remember(version.title, version.content, version.isEncrypted) {
        if (version.isEncrypted) {
            // Note: If this requires biometric auth and the duration has expired, this might fail or show error text.
            // But usually the user just opened the note, so auth duration should be active.
            com.suvojeet.notenext.util.CryptoUtils.decryptNoteVersion(version, isLocked)
        } else {
            version
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timeline node
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 10.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.weight(1f).springPress()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCurrent) "$date · Current" else date,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = decryptedVersion.content.take(60).replace("\n", " ") + if (decryptedVersion.content.length > 60) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Icon(Icons.Rounded.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
