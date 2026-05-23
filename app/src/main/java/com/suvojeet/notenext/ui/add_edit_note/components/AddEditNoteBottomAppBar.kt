@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.suvojeet.notenext.R
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.theme.Fraunces
import com.suvojeet.notenext.ui.theme.ThemeMode
import kotlin.math.roundToInt

// Obsidian glass — the floating dark editing toolbar from the mockup.
private val Obsidian = Color(0xFF14110E)
private val OnObsidian = Color(0xFFF2EDE3)
private val ObsidianAccent = Color(0xFFE47A5C)

@Composable
fun AddEditNoteBottomAppBar(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    showColorPicker: (Boolean) -> Unit,
    showFormatBar: (Boolean) -> Unit,
    showReminderDialog: (Boolean) -> Unit,
    showMoreOptions: (Boolean) -> Unit,
    onImageClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onAudioClick: () -> Unit,
    themeMode: ThemeMode,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showHeadingPicker by remember { mutableStateOf(false) }
    val isChecklist = state.editingNoteType == NoteType.CHECKLIST

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .imePadding(),
        shape = RoundedCornerShape(18.dp),
        color = Obsidian.copy(alpha = 0.96f),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!isChecklist) {
                GlyphTool(glyph = "B", weight = FontWeight.Bold, active = state.isBoldActive) {
                    onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontWeight = FontWeight.Bold)))
                }
                GlyphTool(glyph = "I", style = FontStyle.Italic, active = state.isItalicActive) {
                    onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontStyle = FontStyle.Italic)))
                }
                GlyphTool(glyph = "U", decoration = TextDecoration.Underline, active = state.isUnderlineActive) {
                    onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(textDecoration = TextDecoration.Underline)))
                }
                ToolDivider()
                Box {
                    IconTool(icon = Icons.Default.FormatSize, description = stringResource(id = R.string.heading_style_description), active = state.activeHeadingStyle != 0) {
                        showHeadingPicker = true
                    }
                    DropdownMenu(
                        expanded = showHeadingPicker,
                        onDismissRequest = { showHeadingPicker = false },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        HeadingStylePickerContent(
                            onDismissRequest = { showHeadingPicker = false },
                            onEvent = onEvent
                        )
                    }
                }
                IconTool(icon = Icons.Default.FormatListBulleted, description = "Bulleted List") {
                    onEvent(NotesEvent.ApplyBulletedList)
                }
                IconTool(icon = Icons.Default.FormatQuote, description = "Blockquote") {
                    onEvent(NotesEvent.ApplyBlockquote)
                }
                ToolDivider()
            }

            Box {
                var fabCoordinates by remember { mutableStateOf<IntOffset?>(null) }
                var fabSize by remember { mutableStateOf<IntSize?>(null) }
                IconTool(
                    icon = Icons.Default.Image,
                    description = stringResource(id = R.string.add_attachment),
                    modifier = Modifier.onGloballyPositioned { c ->
                        fabCoordinates = IntOffset(c.positionInWindow().x.roundToInt(), c.positionInWindow().y.roundToInt())
                        fabSize = c.size
                    }
                ) { showAttachmentMenu = true }

                if (showAttachmentMenu) {
                    val coords = fabCoordinates
                    val size = fabSize
                    if (coords != null && size != null) {
                        AttachmentMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false },
                            offset = IntOffset(x = coords.x, y = coords.y - size.height),
                            themeMode = themeMode,
                            onImageClick = onImageClick,
                            onTakePhotoClick = onTakePhotoClick,
                            onAudioClick = onAudioClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlyphTool(
    glyph: String,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    decoration: TextDecoration = TextDecoration.None,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (active) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .springPress(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = OnObsidian.copy(alpha = if (active) 1f else 0.72f),
            fontFamily = Fraunces,
            fontWeight = weight,
            fontStyle = style,
            textDecoration = decoration,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun IconTool(
    icon: ImageVector,
    description: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (active) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .springPress(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) ObsidianAccent else OnObsidian.copy(alpha = 0.72f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(width = 1.dp, height = 18.dp)
            .background(Color.White.copy(alpha = 0.18f))
    )
}

@Composable
private fun AttachmentMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: IntOffset,
    themeMode: ThemeMode,
    onImageClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        offset = offset
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = spring()),
            exit = fadeOut(animationSpec = spring())
        ) {
            val isDark = when (themeMode) {
                ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.MOCHA, ThemeMode.SAGE -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                else -> false
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max)
                    .then(
                        if (isDark) {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.extraLarge
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.add_image)) },
                        onClick = {
                            onImageClick()
                            onDismissRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.take_photo)) },
                        onClick = {
                            onTakePhotoClick()
                            onDismissRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.audio_recording)) },
                        onClick = {
                            onAudioClick()
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}
