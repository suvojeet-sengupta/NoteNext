package com.suvojeet.notenext.ui.bin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.ui.notes.HtmlConverter
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinnedNoteScreen(
    state: BinState,
    onDismiss: () -> Unit
) {
    val noteWithAttachments = state.notes.find { it.note.id == state.expandedNoteId }

    BackHandler { onDismiss() }

    if (noteWithAttachments != null) {
        val note = noteWithAttachments.note
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(note.color)
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(note.color))
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (note.noteType == "CHECKLIST") {
                             noteWithAttachments.checklistItems.forEach { item ->
                                androidx.compose.foundation.layout.Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = HtmlConverter.htmlToAnnotatedString(note.content),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (noteWithAttachments.attachments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Simple attachment list for now, ideally reused attachment component
                            noteWithAttachments.attachments.forEach { attachment ->
                                if (attachment.type == "IMAGE") {
                                    coil.compose.AsyncImage(
                                        model = attachment.uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}