package com.suvojeet.notenext.ui.add_edit_note.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = content.split("\n")
        
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("## ")),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("### ")),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = "• ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = parseInlineMarkdown(line.substring(2)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.startsWith("> ") -> {
                    Surface(
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = parseInlineMarkdown(line.removePrefix("> ")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                line.trim() == "---" -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var currentText = text
        
        // Match bold **text** or __text__
        val boldRegex = "(\\s|^)(\\*\\*|__)(.*?)\\2".toRegex()
        // Match italic *text* or _text_
        val italicRegex = "(\\s|^)(\\*|_)(.*?)\\2".toRegex()
        // Match links [text](url)
        val linkRegex = "\\[(.*?)\\]\\((.*?)\\)".toRegex()

        var lastIndex = 0
        val allMatches = (boldRegex.findAll(text) + italicRegex.findAll(text) + linkRegex.findAll(text))
            .sortedBy { it.range.first }

        allMatches.forEach { match ->
            if (match.range.first >= lastIndex) {
                append(text.substring(lastIndex, match.range.first))
                
                when {
                    match.value.startsWith("**") || match.value.startsWith("__") || (match.value.trim().startsWith("**")) -> {
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        append(prefix)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(content)
                        }
                    }
                    match.value.startsWith("*") || match.value.startsWith("_") || (match.value.trim().startsWith("*")) -> {
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        append(prefix)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(content)
                        }
                    }
                    match.value.startsWith("[") -> {
                        val linkText = match.groupValues[1]
                        val url = match.groupValues[2]
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append(linkText)
                        }
                        pop()
                    }
                }
                lastIndex = match.range.last + 1
            }
        }
        
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
