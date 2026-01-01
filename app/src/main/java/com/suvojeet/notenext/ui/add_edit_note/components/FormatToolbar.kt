package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.FormatSize
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
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    // Determine if dark theme is active based on themeMode and system settings.
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.AMOLED -> true
    }
    // Choose button and icon colors based on the active theme.
    val buttonColor = if (useDarkTheme) dark_button_color else button_color
    val iconColor = if (useDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface

    var showHeadingPicker by remember { mutableStateOf(false) }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.editingNoteType == "CHECKLIST") {
            item {
                IconButton(
                    onClick = { state.focusedChecklistItemId?.let { onEvent(NotesEvent.OutdentChecklistItem(it)) } },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = buttonColor,
                        contentColor = iconColor
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, contentDescription = "Outdent", tint = iconColor)
                }
            }
            item {
                IconButton(
                    onClick = { state.focusedChecklistItemId?.let { onEvent(NotesEvent.IndentChecklistItem(it)) } },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = buttonColor,
                        contentColor = iconColor
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, contentDescription = "Indent", tint = iconColor)
                }
            }
        }
        // Bold formatting button.
        item {
            IconButton(
                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontWeight = FontWeight.Bold))) },
                colors = IconButtonDefaults.iconButtonColors(
                    // Highlight if bold is active.
                    containerColor = if (state.isBoldActive) MaterialTheme.colorScheme.primaryContainer else buttonColor,
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = stringResource(id = R.string.bold_description), tint = iconColor)
            }
        }
        // Italic formatting button.
        item {
            IconButton(
                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontStyle = FontStyle.Italic))) },
                colors = IconButtonDefaults.iconButtonColors(
                    // Highlight if italic is active.
                    containerColor = if (state.isItalicActive) MaterialTheme.colorScheme.primaryContainer else buttonColor,
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = stringResource(id = R.string.italic_description), tint = iconColor)
            }
        }
        // Underline formatting button.
        item {
            IconButton(
                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(textDecoration = TextDecoration.Underline))) },
                colors = IconButtonDefaults.iconButtonColors(
                    // Highlight if underline is active.
                    containerColor = if (state.isUnderlineActive) MaterialTheme.colorScheme.primaryContainer else buttonColor,
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.FormatUnderlined, contentDescription = stringResource(id = R.string.underline_description), tint = iconColor)
            }
        }
        // Heading style picker button and dropdown menu.
        item {
            Box {
                IconButton(
                    onClick = { showHeadingPicker = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        // Highlight if any heading style is active.
                        containerColor = if (state.activeHeadingStyle != 0) MaterialTheme.colorScheme.primaryContainer else buttonColor,
                        contentColor = iconColor
                    )
                ) {
                    Icon(Icons.Default.FormatSize, contentDescription = stringResource(id = R.string.heading_style_description), tint = iconColor)
                }
                DropdownMenu(
                    expanded = showHeadingPicker,
                    onDismissRequest = { showHeadingPicker = false },
                    offset = DpOffset(x = 16.dp, y = 0.dp) // Adjust offset for dropdown positioning.
                ) {
                    HeadingStylePickerContent(
                        onDismissRequest = { showHeadingPicker = false },
                        onEvent = onEvent
                    )
                }
            }
        }
        // Insert link button.
        item {
            IconButton(
                onClick = onInsertLinkClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = buttonColor,
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.AddLink, contentDescription = stringResource(id = R.string.insert_link_description), tint = iconColor)
            }
        }
    }
}