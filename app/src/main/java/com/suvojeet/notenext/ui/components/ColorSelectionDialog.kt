package com.suvojeet.notenext.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.settings.ThemeMode

@Composable
fun ColorSelectionDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
    themeMode: ThemeMode
) {
    val darkNoteColors = listOf(
        Color(0xFF212121).toArgb(), // Very Dark Gray
        Color(0xFFB71C1C).toArgb(), // Dark Red
        Color(0xFFE65100).toArgb(), // Dark Orange
        Color(0xFFF57F17).toArgb(), // Dark Yellow
        Color(0xFF2E7D32).toArgb(), // Dark Green
        Color(0xFF006064).toArgb(), // Dark Teal
        Color(0xFF01579B).toArgb(), // Dark Blue
        Color(0xFF1A237E).toArgb(), // Very Dark Blue
        Color(0xFF4A148C).toArgb(), // Dark Purple
        Color(0xFF880E4F).toArgb(), // Dark Pink
        Color(0xFF3E2723).toArgb(), // Dark Brown
        Color(0xFF424242).toArgb()  // Dark Gray
    )

    val lightNoteColors = listOf(
        Color.White.toArgb(),
        Color(0xFFF28B82).toArgb(), // Red
        Color(0xFFFCBC05).toArgb(), // Orange
        Color(0xFFFFF475).toArgb(), // Yellow
        Color(0xFFCCFF90).toArgb(), // Green
        Color(0xFFA7FFEB).toArgb(), // Teal
        Color(0xFFCBF0F8).toArgb(), // Blue
        Color(0xFFAFCBFA).toArgb(), // Dark Blue
        Color(0xFFD7AEFB).toArgb(), // Purple
        Color(0xFFFDCFE8).toArgb(), // Pink
        Color(0xFFE6C9A8).toArgb(), // Brown
        Color(0xFFE8EAED).toArgb()  // Gray
    )

    val systemInDarkTheme = isSystemInDarkTheme()
    val colors = when (themeMode) {
        ThemeMode.DARK -> darkNoteColors
        ThemeMode.SYSTEM -> if (systemInDarkTheme) darkNoteColors else lightNoteColors
        else -> lightNoteColors
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.toggle_color_picker)) }, // Using existing string "Toggle color picker" or you can add "Select Color"
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(colors) { colorInt ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable {
                                onColorSelected(colorInt)
                            }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

