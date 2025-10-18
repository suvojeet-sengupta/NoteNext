package com.example.notenext.ui.notes

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

object HtmlConverter {

    fun annotatedStringToHtml(annotatedString: AnnotatedString): String {
        val spanned = annotatedStringToSpanned(annotatedString)
        return HtmlCompat.toHtml(spanned, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
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

    fun htmlToAnnotatedString(html: String): AnnotatedString {
        val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        return buildAnnotatedString {
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
        }
    }
}
