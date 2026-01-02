package com.suvojeet.notenext.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

object SimpleDiffUtils {

    enum class DiffType {
        EQUAL, INSERT, DELETE
    }

    data class Diff(val type: DiffType, val text: String)

    /**
     * Generates a list of differences between two strings (word-based).
     */
    fun computeDiff(oldText: String, newText: String): List<Diff> {
        val oldWords = oldText.split(Regex("(?<=\\s)|(?=\\s)"))
        val newWords = newText.split(Regex("(?<=\\s)|(?=\\s)"))

        // Using a simple LCS (Longest Common Subsequence) based approach for diffing
        val lcsMatrix = Array(oldWords.size + 1) { IntArray(newWords.size + 1) }

        for (i in 1..oldWords.size) {
            for (j in 1..newWords.size) {
                if (oldWords[i - 1] == newWords[j - 1]) {
                    lcsMatrix[i][j] = lcsMatrix[i - 1][j - 1] + 1
                } else {
                    lcsMatrix[i][j] = maxOf(lcsMatrix[i - 1][j], lcsMatrix[i][j - 1])
                }
            }
        }

        val diffs = mutableListOf<Diff>()
        var i = oldWords.size
        var j = newWords.size

        while (i > 0 && j > 0) {
            if (oldWords[i - 1] == newWords[j - 1]) {
                diffs.add(Diff(DiffType.EQUAL, oldWords[i - 1]))
                i--
                j--
            } else if (lcsMatrix[i - 1][j] >= lcsMatrix[i][j - 1]) {
                diffs.add(Diff(DiffType.DELETE, oldWords[i - 1]))
                i--
            } else {
                diffs.add(Diff(DiffType.INSERT, newWords[j - 1]))
                j--
            }
        }

        while (i > 0) {
            diffs.add(Diff(DiffType.DELETE, oldWords[i - 1]))
            i--
        }
        while (j > 0) {
            diffs.add(Diff(DiffType.INSERT, newWords[j - 1]))
            j--
        }

        return diffs.reversed()
    }

    /**
     * Creates an AnnotatedString highlighting changes:
     * - Insertions: Green + Underline
     * - Deletions: Red + Strikethrough
     * - Equal: Default color
     */
    fun generateDiffString(diffs: List<Diff>): AnnotatedString {
        return buildAnnotatedString {
            diffs.forEach { diff ->
                when (diff.type) {
                    DiffType.EQUAL -> {
                        append(diff.text)
                    }
                    DiffType.INSERT -> {
                        withStyle(SpanStyle(color = Color(0xFF4CAF50), background = Color(0x334CAF50), textDecoration = TextDecoration.None)) { // Green
                            append(diff.text)
                        }
                    }
                    DiffType.DELETE -> {
                        withStyle(SpanStyle(color = Color(0xFFE57373), textDecoration = TextDecoration.LineThrough)) { // Red
                            append(diff.text)
                        }
                    }
                }
            }
        }
    }
}
