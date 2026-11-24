package com.example.keyboard

import java.util.Locale

/**
 * Pure deterministic helpers for Simple Mode daily typing smoothness:
 * - Sentence-start / newline auto-capitalization detection
 * - Double-space to period (". ") eligibility & URL/punctuation guards
 * - Smart punctuation triggers & solo "i" -> "I" cleanup
 */
object SimpleSmartTyping {

    const val DOUBLE_SPACE_WINDOW_MS = 650L

    private val NON_CAPITALIZING_ABBREVIATIONS = setOf(
        "e.g.", "i.e.", "etc.", "vs.", "mr.", "mrs.", "ms.", "dr.", "prof.", "st.", "jr.", "sr.",
    )

    private val SMART_TRAILING_SPACE_PUNCTUATION = charArrayOf('.', ',', '!', '?', ';', ':')
    private val SENTENCE_END_PUNCTUATION = charArrayOf('.', '!', '?')

    fun isSmartPunctuationMark(ch: Char): Boolean = ch in SMART_TRAILING_SPACE_PUNCTUATION

    fun isSentenceEndingPunctuation(ch: Char): Boolean = ch in SENTENCE_END_PUNCTUATION

    /**
     * Returns true when Simple Mode should automatically engage one-shot Shift
     * based on the text immediately before the caret.
     */
    fun shouldAutoCapitalize(beforeCursor: String?): Boolean {
        if (beforeCursor == null) return false
        if (beforeCursor.isEmpty()) return true
        if (beforeCursor.endsWith('\n')) return true

        val trimmedEnd = beforeCursor.trimEnd { it == ' ' || it == '\t' }
        val hadTrailingHorizontalSpace = trimmedEnd.length < beforeCursor.length
        if (trimmedEnd.isEmpty()) return true
        if (trimmedEnd.endsWith('\n')) return true
        if (!hadTrailingHorizontalSpace) return false

        val core = trimmedEnd.trimEnd { it == '"' || it == '\'' || it == ')' || it == ']' }
        val lastPunct = core.lastOrNull() ?: return false
        if (lastPunct == '!' || lastPunct == '?') return true
        if (lastPunct != '.') return false

        val lastToken = core.substringAfterLast(' ').substringAfterLast('\n').lowercase(Locale.US)
        if (lastToken in NON_CAPITALIZING_ABBREVIATIONS) return false

        // Allow ellipses "..." -> capitalize, but avoid URLs/decimals like "v1.2." or "a.b."
        if (!lastToken.endsWith("..")) {
            val dotCount = lastToken.count { it == '.' }
            if (dotCount > 1) return false
        }
        return true
    }

    /**
     * Returns true when a rapid second Space tap after [beforeCursor] should convert
     * the preceding single space into ". ".
     */
    fun canConvertDoubleSpaceToPeriod(
        beforeCursor: String?,
        lastSpaceTapAtMs: Long,
        nowMs: Long,
    ): Boolean {
        if (beforeCursor == null) return false
        if (lastSpaceTapAtMs <= 0L || (nowMs - lastSpaceTapAtMs) > DOUBLE_SPACE_WINDOW_MS) {
            return false
        }
        if (!beforeCursor.endsWith(' ')) return false
        if (beforeCursor.length < 2) return false

        val charBeforeSpace = beforeCursor[beforeCursor.length - 2]
        if (!charBeforeSpace.isLetterOrDigit()) return false

        val tokenBeforeSpace = beforeCursor
            .dropLast(1)
            .substringAfterLast(' ')
            .substringAfterLast('\n')
            .lowercase(Locale.US)
        if (tokenBeforeSpace.startsWith("http://") ||
            tokenBeforeSpace.startsWith("https://") ||
            tokenBeforeSpace.startsWith("www.")
        ) {
            return false
        }
        return true
    }
}
