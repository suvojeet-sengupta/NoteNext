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

        if (spans.isEmpty()) {
            return text.replace("\n", "<br>").replace("\n", "<br>")
        }

        val events = mutableListOf<Pair<Int, String>>()
        spans.forEach {
            val style = it.item
            if (style.fontWeight == FontWeight.Bold) {
                events.add(it.start to "<b>")
                events.add(it.end to "</b>")
            }
            if (style.fontStyle == FontStyle.Italic) {
                events.add(it.start to "<i>")
                events.add(it.end to "</i>")
            }
            if (style.textDecoration == TextDecoration.Underline) {
                events.add(it.start to "<u>")
                events.add(it.end to "</u>")
            }
        }

        // Sort events by index. If indices are same, closing tags must come first to ensure proper nesting.
        events.sortWith(compareBy({ it.first }, { if (it.second.startsWith("</")) 0 else 1 }))

        val sb = StringBuilder()
        var lastIndex = 0
        events.forEach { (index, tag) ->
            if (index > lastIndex) {
                sb.append(text.substring(lastIndex, index))
            }
            sb.append(tag)
            lastIndex = index
        }

        if (lastIndex < text.length) {
            sb.append(text.substring(lastIndex))
        }

        return sb.toString().replace("\n", "<br>").replace("\n", "<br>")
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