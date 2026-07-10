@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.SaveStatus
import kotlinx.coroutines.delay

@Composable
fun SavedStatusIndicator(
    status: SaveStatus,
    contentColor: Color
) {
    var showSavedIcon by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status == SaveStatus.SAVED) {
            showSavedIcon = true
            delay(2000)
            showSavedIcon = false
        } else if (status == SaveStatus.SAVING) {
            showSavedIcon = false
        }
    }

    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = if (status == SaveStatus.SAVING) SaveStatus.SAVING 
                         else if (status == SaveStatus.SAVED && showSavedIcon) SaveStatus.SAVED
                         else if (status == SaveStatus.ERROR) SaveStatus.ERROR
                         else null,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)))
                    .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300)))
            },
            label = "SaveStatusIndicator"
        ) { targetStatus ->
            when (targetStatus) {
                SaveStatus.SAVING -> {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp),
                        color = contentColor
                    )
                }
                SaveStatus.SAVED -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Saved",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                SaveStatus.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                null -> {
                    Spacer(modifier = Modifier.size(24.dp))
                }
                else -> {}
            }
        }
    }
}

