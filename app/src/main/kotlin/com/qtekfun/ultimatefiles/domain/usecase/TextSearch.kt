package com.qtekfun.ultimatefiles.domain.usecase

/** Plain-text find and replace. */
object TextSearch {
    /** Start offsets of the non-overlapping matches of [query]. */
    fun matches(text: CharSequence, query: String, ignoreCase: Boolean = true): List<Int> {
        if (query.isEmpty()) return emptyList()
        val found = mutableListOf<Int>()
        var from = text.indexOf(query, 0, ignoreCase)
        while (from >= 0) {
            found += from
            from = text.indexOf(query, from + query.length, ignoreCase)
        }
        return found
    }

    /** Index in [matches] of the first match at or after [offset], wrapping around; -1 without matches. */
    fun indexFrom(matches: List<Int>, offset: Int): Int {
        if (matches.isEmpty()) return -1
        val index = matches.indexOfFirst { it >= offset }
        return if (index >= 0) index else 0
    }

    fun replaceAll(text: CharSequence, query: String, replacement: String, ignoreCase: Boolean = true): String =
        if (query.isEmpty()) text.toString() else text.toString().replace(query, replacement, ignoreCase)

    /** Offset where each line of [text] starts. */
    fun lineStarts(text: CharSequence): IntArray {
        val starts = mutableListOf(0)
        for (i in text.indices) if (text[i] == '\n') starts += i + 1
        return starts.toIntArray()
    }
}
