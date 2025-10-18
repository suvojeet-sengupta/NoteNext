package com.example.notenext.ui.notes

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

object HtmlConverter {

    fun annotatedStringToHtml(annotatedString: AnnotatedString): String {
        val text = annotatedString.text
        val spans = annotatedString.spanStyles
        val sb = StringBuilder()
        var lastIndex = 0

        spans.sortedBy { it.start }.forEach { span ->
            if (span.start > lastIndex) {
                sb.append(text.substring(lastIndex, span.start))
            }
            val styledText = text.substring(span.start, span.end)
            val style = span.item
            val openTags = mutableListOf<String>()
            val closeTags = mutableListOf<String>()

            if (style.fontWeight == FontWeight.Bold) {
                openTags.add("<b>")
                closeTags.add("</b>")
            }
            if (style.fontStyle == FontStyle.Italic) {
                openTags.add("<i>")
                closeTags.add("</i>")
            }
            if (style.textDecoration == TextDecoration.Underline) {
                openTags.add("<u>")
                closeTags.add("</u>")
            }
            sb.append(openTags.joinToString(""))
            sb.append(styledText)
            sb.append(closeTags.reversed().joinToString(""))
            lastIndex = span.end
        }

        if (lastIndex < text.length) {
            sb.append(text.substring(lastIndex))
        }

        return sb.toString().replace("\r\n", "<br>").replace("\n", "<br>")
    }

    fun htmlToAnnotatedString(html: String): AnnotatedString {
        val spanned = HtmlCompat.fromHtml(html.replace("<br>", "\n"), HtmlCompat.FROM_HTML_MODE_LEGACY)
        return buildAnnotatedString {
            append(spanned.toString())
            spanned.getSpans(0, spanned.length, Any::class.java).forEach {
                val start = spanned.getSpanStart(it)
                val end = spanned.getSpanEnd(it)
                when (it) {
                    is StyleSpan -> {
                        when (it.style) {
                            android.graphics.Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                            android.graphics.Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                            android.graphics.Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                        }
                    }
                    is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                }
            }
        }
    }
}