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

import androidx.compose.ui.graphics.Color

class RichTextController @Inject constructor() {

    private val WikiLinkStyle = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)

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
        
        return reapplyWikiLinks(newContent.copy(annotatedString = newAnnotatedString))
    }

    private fun reapplyWikiLinks(content: TextFieldValue): TextFieldValue {
        val text = content.text
        val styles = content.annotatedString.spanStyles
        
        // Find existing NOTE_LINK annotations to identify which styles to remove
        val oldLinkAnnotations = content.annotatedString.getStringAnnotations("NOTE_LINK", 0, text.length)
        
        // Filter out styles that exactly match the WikiLinkStyle and overlap with old link annotations
        val cleanedStyles = styles.filterNot { styleRange ->
            oldLinkAnnotations.any { linkRange ->
                linkRange.start == styleRange.start && linkRange.end == styleRange.end && styleRange.item == WikiLinkStyle
            }
        }
        
        // Filter out existing NOTE_LINK annotations
        val cleanedAnnotations = content.annotatedString.getStringAnnotations(0, text.length).filter { it.tag != "NOTE_LINK" }

        val builder = AnnotatedString.Builder(text)
        
        // Add back preserved styles
        cleanedStyles.forEach { builder.addStyle(it.item, it.start, it.end) }
        
        // Add back preserved annotations
        cleanedAnnotations.forEach { builder.addStringAnnotation(it.tag, it.item, it.start, it.end) }

        // Find and apply new Wiki Links
        val regex = "\\[\\[(.*?)\\]\\]".toRegex()
        regex.findAll(text).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            val title = matchResult.groupValues[1]
            if (title.isNotBlank()) {
                builder.addStringAnnotation("NOTE_LINK", title, start, end)
                builder.addStyle(WikiLinkStyle, start, end)
            }
        }

        return content.copy(annotatedString = builder.toAnnotatedString())
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

    fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentText = text

            // Match bold **text** or __text__
            val boldRegex = "(\\s|^)(\\*\\*|__)(.*?)\\2".toRegex()
            // Match italic *text* or _text_
            val italicRegex = "(\\s|^)(\\*|_)(.*?)\\2".toRegex()
            // Match links [text](url)
            val linkRegex = "\\[(.*?)\\]\\((.*?)\\)".toRegex()
            // Match wiki links [[text]]
            val wikiLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()

            var lastIndex = 0
            val allMatches = (boldRegex.findAll(text) + italicRegex.findAll(text) + linkRegex.findAll(text) + wikiLinkRegex.findAll(text))
                .sortedBy { it.range.first }

            allMatches.forEach { match ->
                if (match.range.first >= lastIndex) {
                    append(text.substring(lastIndex, match.range.first))

                    when {
                        match.value.startsWith("**") || match.value.startsWith("__") || (match.value.trim().startsWith("**")) -> {
                            val content = match.groupValues[3]
                            // preserve prefix space if any
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
                        match.value.startsWith("[[") -> {
                            val linkText = match.groupValues[1]
                            pushStringAnnotation(tag = "NOTE_LINK", annotation = linkText)
                            withStyle(WikiLinkStyle) {
                                append(linkText)
                            }
                            pop()
                        }
                        match.value.startsWith("[") -> {
                            val linkText = match.groupValues[1]
                            val url = match.groupValues[2]
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
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
}
