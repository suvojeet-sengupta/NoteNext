package com.suvojeet.notenext.util

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HtmlConverter {

    suspend fun annotatedStringToHtml(annotatedString: AnnotatedString): String = withContext(Dispatchers.Default) {
        val spanned = annotatedStringToSpanned(annotatedString)
        HtmlCompat.toHtml(spanned, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
    }

    private fun annotatedStringToSpanned(annotatedString: AnnotatedString): Spanned {
        val spannable = SpannableStringBuilder(annotatedString.text)
        annotatedString.spanStyles.forEach { range ->
            val style = range.item
            if (style.fontWeight == FontWeight.Bold && style.fontStyle == FontStyle.Italic) {
                spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD_ITALIC), range.start, range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (style.fontWeight == FontWeight.Bold) {
                spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), range.start, range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (style.fontStyle == FontStyle.Italic) {
                spannable.setSpan(StyleSpan(android.graphics.Typeface.ITALIC), range.start, range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (style.textDecoration == TextDecoration.Underline) {
                spannable.setSpan(UnderlineSpan(), range.start, range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spannable
    }

    suspend fun htmlToPlainText(html: String): String = withContext(Dispatchers.Default) {
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
    }

    suspend fun htmlToAnnotatedString(html: String): AnnotatedString = withContext(Dispatchers.Default) {
        val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        buildAnnotatedString {
            append(spanned.toString())
            spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                when (span) {
                    is StyleSpan -> {
                        when (span.style) {
                            android.graphics.Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                            android.graphics.Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                            android.graphics.Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                        }
                    }
                    is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                }
            }
            
            // Detect [[Note Title]] and add styling/annotation
            val text = spanned.toString()
            val noteLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()
            noteLinkRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val title = matchResult.groupValues[1]
                addStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFFD0BCFF), // Light Purple for Note Links
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "NOTE_LINK",
                    annotation = title,
                    start = start,
                    end = end
                )
            }

            // Detect URLs
            val urlRegex = "(https?://\\S+|www\\.\\S+)".toRegex()
            urlRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val url = matchResult.value
                addStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFF64B5F6), // Light Blue for URLs
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = url,
                    start = start,
                    end = end
                )
            }

            // Detect Emails
            val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
            emailRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val email = matchResult.value
                addStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFF64B5F6),
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "EMAIL",
                    annotation = "mailto:$email",
                    start = start,
                    end = end
                )
            }

            // Detect Phone Numbers - ONLY 10 consecutive digits (Indian phone numbers)
            // Matches: 9876543210, +919876543210 (not shorter or longer numbers)
            val phoneRegex = "(?<!\\d)(\\+91)?\\d{10}(?!\\d)".toRegex()
            phoneRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val phone = matchResult.value
                addStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFF64B5F6),
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "PHONE",
                    annotation = "tel:$phone",
                    start = start,
                    end = end
                )
            }
        }
    }
}
