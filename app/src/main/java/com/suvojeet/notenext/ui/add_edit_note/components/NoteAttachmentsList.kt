@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.core.model.AttachmentType
import com.suvojeet.notenext.ui.add_edit_note.ImageViewerData
import com.suvojeet.notenext.ui.notes.NotesEvent

@Composable
fun NoteAttachmentsList(
    attachments: List<Attachment>,
    onEvent: (NotesEvent) -> Unit,
    onImageClick: (ImageViewerData) -> Unit
) {
    val imageAttachments = attachments.filter { it.type == AttachmentType.IMAGE }
    val audioAttachments = attachments.filter { it.type == AttachmentType.AUDIO }

    if (audioAttachments.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            audioAttachments.forEach { attachment ->
                AudioAttachmentPlayer(
                    uri = attachment.uri,
                    onRemove = { onEvent(NotesEvent.RemoveAttachment(attachment.tempId)) }
                )
            }
        }
    }

    if (imageAttachments.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        val imageCount = imageAttachments.size
        
        if (imageCount == 1) {
            // Single image: full width
            val attachment = imageAttachments.first()
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .clickable {
                            onImageClick(ImageViewerData(uri = Uri.parse(attachment.uri), tempId = attachment.tempId))
                        },
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { onEvent(NotesEvent.RemoveAttachment(attachment.tempId)) },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Remove image", 
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // Multiple images: up to 3 per row in a horizontal scrollable row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(imageAttachments, key = { it.uri }) { attachment ->
                    Box {
                        AsyncImage(
                            model = attachment.uri,
                            contentDescription = null,
                            modifier = Modifier
                                .width(120.dp)
                                .height(120.dp)
                                .aspectRatio(1f)
                                .clickable {
                                    onImageClick(ImageViewerData(uri = Uri.parse(attachment.uri), tempId = attachment.tempId))
                                },
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { onEvent(NotesEvent.RemoveAttachment(attachment.tempId)) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Remove image", 
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
