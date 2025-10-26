package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.suvojeet.notenext.ui.settings.ThemeMode
import com.suvojeet.notenext.ui.theme.button_color
import com.suvojeet.notenext.ui.theme.dark_button_color

@Composable
fun FormatToolbar(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onInsertLinkClick: () -> Unit,
    themeMode: ThemeMode
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDarkTheme
    }
    val buttonColor = if (useDarkTheme) dark_button_color else button_color
    val iconColor = if (useDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            IconButton(
                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontWeight = FontWeight.Bold))) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (state.isBoldActive) MaterialTheme.colorScheme.primaryContainer else buttonColor,
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold")
            }
        }
        item {
            IconButton(
                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontStyle = FontStyle.Italic))) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (state.isItalicActive) MaterialTheme.colorScheme.primaryContainer else buttonColor,
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
            }
        }
        item {
            IconButton(
                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(textDecoration = TextDecoration.Underline))) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (state.isUnderlineActive) MaterialTheme.colorScheme.primaryContainer else buttonColor,
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
            }
        }
        item {
            IconButton(
                onClick = onInsertLinkClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = buttonColor,
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.AddLink, contentDescription = "Insert link")
            }
        }
    }
}