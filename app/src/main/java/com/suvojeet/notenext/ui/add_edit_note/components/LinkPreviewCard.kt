package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.ui.notes.NotesEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPreviewCard(linkPreview: LinkPreview, onEvent: (NotesEvent) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onEvent(NotesEvent.OnRemoveLinkPreview(linkPreview.url))
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                    SwipeToDismissBoxValue.StartToEnd -> Color.Transparent
                },
                label = "background color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Icon",
                    tint = Color.White
                )
            }
        }) { 
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { linkPreview.url.let { uriHandler.openUri(it) } },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        linkPreview.title?.let { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Remove preview") },
                                    onClick = {
                                        onEvent(NotesEvent.OnRemoveLinkPreview(linkPreview.url))
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Remove preview") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy URL") },
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(linkPreview.url))
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL") }
                                )
                            }
                        }
                    }
                    linkPreview.imageUrl?.let { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Link preview image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    linkPreview.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    ClickableText(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                                append(linkPreview.url)
                            }
                        },
                        onClick = { uriHandler.openUri(linkPreview.url) },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
}