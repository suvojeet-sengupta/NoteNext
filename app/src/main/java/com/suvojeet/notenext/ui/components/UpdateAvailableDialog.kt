@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.util.UpdateChecker

@Composable
fun UpdateAvailableDialog(
    updateStatus: UpdateChecker.UpdateStatus,
    onUpdateClick: () -> Unit,
    onCompleteUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val isDownloading = updateStatus is UpdateChecker.UpdateStatus.Downloading || 
                        updateStatus is UpdateChecker.UpdateStatus.DownloadProgress
    val isDownloaded = updateStatus is UpdateChecker.UpdateStatus.Downloaded
    val isInstalling = updateStatus is UpdateChecker.UpdateStatus.Installing

    ModalBottomSheet(
        onDismissRequest = { if (!isDownloading && !isInstalling) onDismiss() },
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isDownloaded) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent<UpdateChecker.UpdateStatus>(
                        targetState = updateStatus,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "icon_anim"
                    ) { status ->
                        when (status) {
                            is UpdateChecker.UpdateStatus.Downloaded -> {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            is UpdateChecker.UpdateStatus.Downloading,
                            is UpdateChecker.UpdateStatus.DownloadProgress,
                            is UpdateChecker.UpdateStatus.Installing -> {
                                LoadingIndicator(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = when {
                    isDownloaded -> stringResource(R.string.update_ready)
                    isDownloading -> stringResource(R.string.downloading_update)
                    isInstalling -> stringResource(R.string.installing_update)
                    else -> stringResource(R.string.update_available)
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when {
                    isDownloaded -> stringResource(R.string.update_downloaded_ready)
                    isDownloading -> stringResource(R.string.downloading_msg)
                    isInstalling -> stringResource(R.string.installing_msg)
                    else -> stringResource(R.string.update_available_msg)
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent<UpdateChecker.UpdateStatus>(
                targetState = updateStatus,
                label = "progress_anim"
            ) { status ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (status) {
                        is UpdateChecker.UpdateStatus.DownloadProgress -> {
                            val progress = status.bytesDownloaded.toFloat() / status.totalBytes.toFloat()
                            val percentage = (progress * 100).toInt()
                            
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            WavyProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        is UpdateChecker.UpdateStatus.Downloading, is UpdateChecker.UpdateStatus.Installing -> {
                            WavyProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        else -> {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (isDownloaded) {
                        onCompleteUpdate()
                    } else if (!isDownloading && !isInstalling) {
                        onUpdateClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .springPress(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDownloaded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    contentColor = if (isDownloaded) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !isDownloading && !isInstalling || isDownloaded
            ) {
                val buttonText = when {
                    isDownloaded -> stringResource(R.string.restart_to_update)
                    isDownloading -> stringResource(R.string.downloading)
                    isInstalling -> stringResource(R.string.installing)
                    else -> stringResource(R.string.update_now)
                }
                
                if (isDownloading || isInstalling) {
                    LoadingIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Outlined.AutoAwesome else Icons.Default.SystemUpdate, 
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isDownloaded) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .springPress(),
                    enabled = !isInstalling,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = if (isDownloading) stringResource(R.string.run_in_background) else stringResource(R.string.later),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
