@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AiSummarySheet(
    summary: String?,
    isSummarizing: Boolean,
    onDismiss: () -> Unit,
    onClearSummary: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Set skipPartiallyExpanded to true to avoid button issues in partially expanded state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Determine if we are in the expanded state to adjust UI elements
    val isExpanded = sheetState.targetValue == SheetValue.Expanded || sheetState.currentValue == SheetValue.Expanded
    
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 28.dp,
        label = "CornerRadius"
    )
    
    val horizontalPadding by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 24.dp,
        label = "HorizontalPadding"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isExpanded) Modifier.fillMaxHeight() else Modifier)
                .padding(horizontal = horizontalPadding)
                .padding(bottom = 24.dp)
        ) {
            // Header with Branding and Compact Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isExpanded) 24.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.ai_summary_brand),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (!isSummarizing && summary != null) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(context.getString(R.string.ai_summary_clip_label), summary)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.ai_summary_copied_toast), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.springPress()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(id = R.string.ai_summary_copy_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.ai_summary_share_subject))
                                putExtra(Intent.EXTRA_TEXT, summary)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.ai_summary_share_chooser)))
                        },
                        modifier = Modifier.springPress()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(id = R.string.ai_summary_share_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            onDismiss()
                        }
                    }, 
                    modifier = Modifier.springPress()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.ai_summary_close_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Content Area
            Surface(
                shape = if (isExpanded) RoundedCornerShape(0.dp) else MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isExpanded) 1f else 0.0001f, fill = false)
                    .heightIn(min = 200.dp, max = if (isExpanded) 2000.dp else 450.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.padding(4.dp),
                    contentAlignment = if (isSummarizing) Alignment.Center else Alignment.TopStart
                ) {
                    if (isSummarizing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.ai_summary_summarizing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        summary?.let { text ->
                            Column(
                                modifier = Modifier.verticalScroll(scrollState)
                            ) {
                                val (thinking, cleanSummary) = remember(text) {
                                    val thoughtRegex = Regex("<thought>(.*?)</thought>", RegexOption.DOT_MATCHES_ALL)
                                    val thoughtMatch = thoughtRegex.find(text)
                                    val thought = thoughtMatch?.groupValues?.get(1)?.let {
                                        // Strip HTML from thinking
                                        val plain = it.replace(Regex("<[^>]*>"), " ")
                                        androidx.core.text.HtmlCompat.fromHtml(plain, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                                    }
                                    
                                    val summaryWithoutThought = text.replace(thoughtRegex, "").trim()
                                    // Strip HTML from summary
                                    val plainSummary = summaryWithoutThought.replace(Regex("<[^>]*>"), " ")
                                    val clean = androidx.core.text.HtmlCompat.fromHtml(plainSummary, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                                    
                                    thought to clean
                                }

                                if (thinking != null) {
                                    var showThinking by remember { mutableStateOf(false) }
                                    
                                    Surface(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable { showThinking = !showThinking },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (showThinking) Icons.Default.Lightbulb else Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (showThinking) stringResource(id = R.string.ai_summary_hide_thinking) else stringResource(id = R.string.ai_summary_thinking),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(
                                                    imageVector = if (showThinking) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            AnimatedVisibility(
                                                visible = showThinking,
                                                enter = expandVertically() + fadeIn(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                Column {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = thinking,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                var visibleText by remember { mutableStateOf("") }
                                LaunchedEffect(cleanSummary) {
                                    visibleText = "" 
                                    val chunkSize = 12
                                    for (i in cleanSummary.indices step chunkSize) {
                                        val end = (i + chunkSize).coerceAtMost(cleanSummary.length)
                                        visibleText += cleanSummary.substring(i, end)
                                        delay(5) 
                                    }
                                    visibleText = cleanSummary 
                                }
                                
                                MarkdownPreview(content = visibleText)

                                // Disclaimer Section
                                Surface(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(id = R.string.ai_summary_disclaimer),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } ?: Text(
                            stringResource(id = R.string.ai_summary_unavailable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
