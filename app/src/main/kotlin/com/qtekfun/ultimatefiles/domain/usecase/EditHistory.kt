package com.qtekfun.ultimatefiles.domain.usecase

/**
 * Undo and redo for a text buffer. It is fed whole snapshots with [record] and stores only the span that changed;
 * consecutive typing of a word is one step. Bounded both in steps and in stored characters.
 */
class EditHistory(
    initial: String,
    private val maxSteps: Int = 500,
    private val maxChars: Int = 4 * 1024 * 1024,
) {
    /** Replace [length] characters at [start] with [text]. */
    data class Edit(val start: Int, val length: Int, val text: String)

    private data class Change(val start: Int, val removed: String, val inserted: String) {
        val size get() = removed.length + inserted.length
    }

    private var current = initial
    private val undo = ArrayDeque<Change>()
    private val redo = ArrayDeque<Change>()
    private var mergeable = false

    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()

    fun record(text: String) {
        if (text == current) return
        val change = diff(current, text)
        current = text
        redo.clear()
        val last = undo.lastOrNull()
        if (mergeable && last != null && isTyping(last, change)) {
            undo[undo.lastIndex] = last.copy(inserted = last.inserted + change.inserted)
        } else {
            undo.addLast(change)
        }
        mergeable = true
        while (undo.size > 1 && (undo.size > maxSteps || undo.sumOf { it.size } > maxChars)) undo.removeFirst()
    }

    /** The edit that takes the buffer one step back, or null when there is nothing to undo. */
    fun undo(): Edit? {
        val change = undo.removeLastOrNull() ?: return null
        redo.addLast(change)
        return apply(Edit(change.start, change.inserted.length, change.removed))
    }

    fun redo(): Edit? {
        val change = redo.removeLastOrNull() ?: return null
        undo.addLast(change)
        return apply(Edit(change.start, change.removed.length, change.inserted))
    }

    private fun apply(edit: Edit): Edit {
        current = current.replaceRange(edit.start, edit.start + edit.length, edit.text)
        mergeable = false
        return edit
    }

    private fun isTyping(last: Change, next: Change) = next.removed.isEmpty() && next.inserted.length == 1 &&
        last.removed.isEmpty() && next.start == last.start + last.inserted.length &&
        !next.inserted[0].isWhitespace() && !last.inserted.last().isWhitespace()

    private fun diff(old: String, new: String): Change {
        val shortest = minOf(old.length, new.length)
        var prefix = 0
        while (prefix < shortest && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (suffix < shortest - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        return Change(prefix, old.substring(prefix, old.length - suffix), new.substring(prefix, new.length - suffix))
    }
}
