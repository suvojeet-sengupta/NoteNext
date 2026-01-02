package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.ui.theme.button_color
import com.suvojeet.notenext.ui.theme.dark_button_color
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import androidx.compose.ui.unit.DpOffset

/**
 * A toolbar providing text formatting options for the note editor.
 * Includes buttons for bold, italic, underline, heading styles, and inserting links.
 *
 * @param state The current [NotesState] containing information about the note's formatting.
 * @param onEvent Lambda to dispatch [NotesEvent]s for applying formatting changes.
 * @param onInsertLinkClick Lambda to be invoked when the "Insert Link" button is clicked.
 * @param themeMode The current [ThemeMode] to adjust button and icon colors.
 */
@Composable
fun FormatToolbar(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onInsertLinkClick: () -> Unit,
    onGrammarFixClick: () -> Unit,
    isFixingGrammar: Boolean,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.AMOLED -> true
    }

    var showHeadingPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checklist Group
            if (state.editingNoteType == "CHECKLIST") {
                item {
                    FormatToggleButton(
                        onClick = { state.focusedChecklistItemId?.let { onEvent(NotesEvent.OutdentChecklistItem(it)) } },
                        icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                        description = "Outdent",
                        isActive = false, // Stateless action
                        useDarkTheme = useDarkTheme
                    )
                }
                item {
                    FormatToggleButton(
                        onClick = { state.focusedChecklistItemId?.let { onEvent(NotesEvent.IndentChecklistItem(it)) } },
                        icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                        description = "Indent",
                        isActive = false, // Stateless action
                        useDarkTheme = useDarkTheme
                    )
                }
                item {
                    // Separator
                     Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }

            // Style Group
            item {
                FormatToggleButton(
                    onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontWeight = FontWeight.Bold))) },
                    icon = Icons.Default.FormatBold,
                    description = stringResource(id = R.string.bold_description),
                    isActive = state.isBoldActive,
                    useDarkTheme = useDarkTheme
                )
            }
            item {
                FormatToggleButton(
                    onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontStyle = FontStyle.Italic))) },
                    icon = Icons.Default.FormatItalic,
                    description = stringResource(id = R.string.italic_description),
                    isActive = state.isItalicActive,
                    useDarkTheme = useDarkTheme
                )
            }
            item {
                FormatToggleButton(
                    onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(textDecoration = TextDecoration.Underline))) },
                    icon = Icons.Default.FormatUnderlined,
                    description = stringResource(id = R.string.underline_description),
                    isActive = state.isUnderlineActive,
                    useDarkTheme = useDarkTheme
                )
            }

            item {
                 Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            // Structure Group
            item {
                Box {
                    FormatToggleButton(
                        onClick = { showHeadingPicker = true },
                        icon = Icons.Default.FormatSize,
                        description = stringResource(id = R.string.heading_style_description),
                        isActive = state.activeHeadingStyle != 0,
                        useDarkTheme = useDarkTheme
                    )
                    DropdownMenu(
                        expanded = showHeadingPicker,
                        onDismissRequest = { showHeadingPicker = false },
                        offset = DpOffset(x = 0.dp, y = 8.dp)
                    ) {
                        HeadingStylePickerContent(
                            onDismissRequest = { showHeadingPicker = false },
                            onEvent = onEvent
                        )
                    }
                }
            }

            // Insert Group
            item {
                FormatToggleButton(
                    onClick = onInsertLinkClick,
                    icon = Icons.Default.AddLink,
                    description = stringResource(id = R.string.insert_link_description),
                    isActive = false,
                    useDarkTheme = useDarkTheme
                )
            }
            
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            
            // AI Grammar Fix
            item {
                FormatToggleButton(
                    onClick = onGrammarFixClick,
                    icon = Icons.Default.AutoAwesome,
                    description = "Fix Grammar",
                    isActive = isFixingGrammar,
                    useDarkTheme = useDarkTheme
                )
            }
        }
    }
}

@Composable
private fun FormatToggleButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    isActive: Boolean,
    useDarkTheme: Boolean
) {
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isActive) {
         MaterialTheme.colorScheme.onPrimaryContainer
    } else {
         if (useDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    }

    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(icon, contentDescription = description)
    }
}