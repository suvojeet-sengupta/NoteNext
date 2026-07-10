@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.core.model.AttachmentType
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.produceState
import com.suvojeet.notenext.ui.theme.NoteGradients
import com.suvojeet.notenext.ui.components.springPress
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun BinnedNoteScreen(
    state: BinState,
    onDismiss: () -> Unit
) {
    val noteWithAttachments = state.notes.find { it.note.id == state.expandedNoteId }
    val isDark = isSystemInDarkTheme()

    BackHandler { onDismiss() }

    if (noteWithAttachments != null) {
        val note = noteWithAttachments.note
        val adaptiveColor = NoteGradients.getAdaptiveColor(note.color, isDark)
        val backgroundColor = if (adaptiveColor != 0) Color(adaptiveColor) else MaterialTheme.colorScheme.surface
        val contentColor = if (adaptiveColor != 0) NoteGradients.getContentColor(adaptiveColor) else MaterialTheme.colorScheme.onSurface

        val annotatedContent = produceState<AnnotatedString>(initialValue = AnnotatedString(""), note.content) {
            value = HtmlConverter.htmlToAnnotatedString(note.content)
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = stringResource(id = R.string.back),
                                tint = contentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor
                    )
                )
            },
            containerColor = backgroundColor
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (note.noteType == NoteType.CHECKLIST) {
                             noteWithAttachments.checklistItems.forEach { item ->
                                androidx.compose.foundation.layout.Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = contentColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = contentColor,
                                        textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = annotatedContent.value,
                                style = MaterialTheme.typography.bodyLarge,
                                color = contentColor
                            )
                        }

                        if (noteWithAttachments.attachments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            noteWithAttachments.attachments.forEach { attachment ->
                                if (attachment.type == AttachmentType.IMAGE) {
                                    androidx.compose.material3.Card(
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        coil3.compose.AsyncImage(
                                            model = attachment.uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}