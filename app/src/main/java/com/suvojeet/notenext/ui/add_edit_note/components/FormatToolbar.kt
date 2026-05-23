@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.theme.ThemeMode
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import androidx.compose.ui.unit.DpOffset

@Composable
fun FormatToolbar(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onInsertLinkClick: () -> Unit,
    onGrammarFixClick: () -> Unit,
    isFixingGrammar: Boolean,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    var showHeadingPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.editingNoteType == NoteType.CHECKLIST) {
                item {
                    FormatToggleButton(
                        onCheckedChange = { state.focusedChecklistItemId?.let { onEvent(NotesEvent.OutdentChecklistItem(it)) } },
                        icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                        description = "Outdent",
                        isActive = false
                    )
                }
                item {
                    FormatToggleButton(
                        onCheckedChange = { state.focusedChecklistItemId?.let { onEvent(NotesEvent.IndentChecklistItem(it)) } },
                        icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                        description = "Indent",
                        isActive = false
                    )
                }
                item {
                     VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
                }
            }

            item {
                FormatToggleButton(
                    onCheckedChange = { _ -> onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontWeight = FontWeight.Bold))) },
                    icon = Icons.Default.FormatBold,
                    description = stringResource(id = R.string.bold_description),
                    isActive = state.isBoldActive
                )
            }
            item {
                FormatToggleButton(
                    onCheckedChange = { _ -> onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontStyle = FontStyle.Italic))) },
                    icon = Icons.Default.FormatItalic,
                    description = stringResource(id = R.string.italic_description),
                    isActive = state.isItalicActive
                )
            }
            item {
                FormatToggleButton(
                    onCheckedChange = { _ -> onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(textDecoration = TextDecoration.Underline))) },
                    icon = Icons.Default.FormatUnderlined,
                    description = stringResource(id = R.string.underline_description),
                    isActive = state.isUnderlineActive
                )
            }

            item {
                 VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
            }

            item {
                Box {
                    FormatToggleButton(
                        onCheckedChange = { showHeadingPicker = true },
                        icon = Icons.Default.FormatSize,
                        description = stringResource(id = R.string.heading_style_description),
                        isActive = state.activeHeadingStyle != 0
                    )
                    DropdownMenu(
                        expanded = showHeadingPicker,
                        onDismissRequest = { showHeadingPicker = false },
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        HeadingStylePickerContent(
                            onDismissRequest = { showHeadingPicker = false },
                            onEvent = onEvent
                        )
                    }
                }
            }

            item {
                FormatToggleButton(
                    onCheckedChange = { _ -> onEvent(NotesEvent.ApplyBulletedList) },
                    icon = Icons.Default.FormatListBulleted,
                    description = "Bulleted List",
                    isActive = false
                )
            }

            item {
                FormatToggleButton(
                    onCheckedChange = { _ -> onEvent(NotesEvent.ApplyBlockquote) },
                    icon = Icons.Default.FormatQuote,
                    description = "Blockquote",
                    isActive = false
                )
            }

            item {
                 VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
            }
            
            item {
                FormatToggleButton(
                    onCheckedChange = { _ -> onGrammarFixClick() },
                    icon = Icons.Outlined.AutoAwesome,
                    description = "Fix Grammar",
                    isActive = isFixingGrammar
                )
            }
        }
    }
}

@Composable
private fun FormatToggleButton(
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    isActive: Boolean
) {
    IconToggleButton(
        checked = isActive,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.size(40.dp).springPress()
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}
