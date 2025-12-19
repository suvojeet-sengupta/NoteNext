package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import javax.inject.Inject

class RichTextController @Inject constructor() {

    fun getHeadingStyle(level: Int): SpanStyle {
        return when (level) {
            1 -> SpanStyle(fontSize = 24.sp)
            2 -> SpanStyle(fontSize = 20.sp)
            3 -> SpanStyle(fontSize = 18.sp)
            4 -> SpanStyle(fontSize = 16.sp)
            5 -> SpanStyle(fontSize = 14.sp)
            6 -> SpanStyle(fontSize = 12.sp)
            else -> SpanStyle()
        }
    }

    fun processContentChange(
        oldContent: TextFieldValue,
        newContent: TextFieldValue,
        activeStyles: Set<SpanStyle>,
        activeHeadingStyle: Int
    ): TextFieldValue {
        if (newContent.text == oldContent.text) {
            return oldContent.copy(selection = newContent.selection)
        }

        val oldText = oldContent.text
        val newText = newContent.text

        val prefixLength = commonPrefixWith(oldText, newText).length
        val oldRemainder = oldText.substring(prefixLength)
        val newRemainder = newText.substring(prefixLength)
        val suffixLength = commonSuffixWith(oldRemainder, newRemainder).length
        val newChangedPart = newRemainder.substring(0, newRemainder.length - suffixLength)

        val newAnnotatedString = buildAnnotatedString {
            append(oldContent.annotatedString.subSequence(0, prefixLength))

            val headingSpanStyle = getHeadingStyle(activeHeadingStyle)
            val styleToApply = (activeStyles + headingSpanStyle).reduceOrNull { a, b -> a.merge(b) } ?: SpanStyle()

            withStyle(styleToApply) {
                append(newChangedPart)
            }

            append(oldContent.annotatedString.subSequence(oldText.length - suffixLength, oldText.length))
        }
        return newContent.copy(annotatedString = newAnnotatedString)
    }

    data class StyleToggleResult(
        val updatedContent: TextFieldValue? = null,
        val updatedActiveStyles: Set<SpanStyle>? = null
    )

    fun toggleStyle(
        content: TextFieldValue,
        styleToToggle: SpanStyle,
        currentActiveStyles: Set<SpanStyle>,
        isBoldActive: Boolean,
        isItalicActive: Boolean,
        isUnderlineActive: Boolean
    ): StyleToggleResult {
        val selection = content.selection
        if (selection.collapsed) {
            val activeStyles = currentActiveStyles.toMutableSet()

            val isBold = styleToToggle.fontWeight == FontWeight.Bold
            val isItalic = styleToToggle.fontStyle == FontStyle.Italic
            val isUnderline = styleToToggle.textDecoration == TextDecoration.Underline

            val wasBold = activeStyles.any { it.fontWeight == FontWeight.Bold }
            val wasItalic = activeStyles.any { it.fontStyle == FontStyle.Italic }
            val wasUnderline = activeStyles.any { it.textDecoration == TextDecoration.Underline }

            if (isBold) {
                if (wasBold) activeStyles.removeAll { it.fontWeight == FontWeight.Bold }
                else activeStyles.add(SpanStyle(fontWeight = FontWeight.Bold))
            }
            if (isItalic) {
                if (wasItalic) activeStyles.removeAll { it.fontStyle == FontStyle.Italic }
                else activeStyles.add(SpanStyle(fontStyle = FontStyle.Italic))
            }
            if (isUnderline) {
                if (wasUnderline) activeStyles.removeAll { it.textDecoration == TextDecoration.Underline }
                else activeStyles.add(SpanStyle(textDecoration = TextDecoration.Underline))
            }
            return StyleToggleResult(updatedActiveStyles = activeStyles)
        } else {
            val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                val isApplyingBold = styleToToggle.fontWeight == FontWeight.Bold
                val isApplyingItalic = styleToToggle.fontStyle == FontStyle.Italic
                val isApplyingUnderline = styleToToggle.textDecoration == TextDecoration.Underline

                val styleToApply = when {
                    isApplyingBold -> if (isBoldActive) SpanStyle(fontWeight = FontWeight.Normal) else SpanStyle(fontWeight = FontWeight.Bold)
                    isApplyingItalic -> if (isItalicActive) SpanStyle(fontStyle = FontStyle.Normal) else SpanStyle(fontStyle = FontStyle.Italic)
                    isApplyingUnderline -> if (isUnderlineActive) SpanStyle(textDecoration = TextDecoration.None) else SpanStyle(textDecoration = TextDecoration.Underline)
                    else -> styleToToggle
                }
                addStyle(styleToApply, selection.start, selection.end)
            }.toAnnotatedString()

            return StyleToggleResult(updatedContent = content.copy(annotatedString = newAnnotatedString))
        }
    }

    fun applyHeading(
        content: TextFieldValue,
        level: Int
    ): TextFieldValue? {
        val selection = content.selection
        
        val headingStyle = when (level) {
            1 -> SpanStyle(fontSize = 24.sp)
            2 -> SpanStyle(fontSize = 20.sp)
            3 -> SpanStyle(fontSize = 18.sp)
            4 -> SpanStyle(fontSize = 16.sp)
            5 -> SpanStyle(fontSize = 14.sp)
            6 -> SpanStyle(fontSize = 12.sp)
            else -> SpanStyle()
        }

        if (!selection.collapsed) {
             val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                addStyle(headingStyle, selection.start, selection.end)
            }.toAnnotatedString()
            return content.copy(annotatedString = newAnnotatedString)
        }
        return null
    }

    private fun commonPrefixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[i] != b[i]) {
                return a.substring(0, i)
            }
        }
        return a.substring(0, minLength)
    }

    private fun commonSuffixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[a.length - 1 - i] != b[b.length - 1 - i]) {
                return a.substring(a.length - i)
            }
        }
        return a.substring(a.length - minLength)
    }
}
