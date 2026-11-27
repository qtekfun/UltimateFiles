package com.qtekfun.ultimatefiles.domain.usecase

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TextEditingTest {
    private fun roundTrip(bytes: ByteArray): TextDocument {
        val document = TextDocument.decode(bytes)
        assertArrayEquals(bytes, document.encode(document.text))
        return document
    }

    @Test
    fun crlfAndBomSurviveASave() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val document = roundTrip(bom + "uno\r\ndós\r\n".toByteArray())
        assertEquals("uno\ndós\n", document.text)
        assertArrayEquals(bom + "a\r\nb".toByteArray(), document.encode("a\nb"))
    }

    @Test
    fun otherEncodingsAndMixedEndingsSurviveASave() {
        assertEquals(Charsets.ISO_8859_1, roundTrip("año".toByteArray(Charsets.ISO_8859_1)).charset)
        assertEquals("hi", roundTrip(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "hi".toByteArray(Charsets.UTF_16LE)).text)
        assertEquals("a\r\nb\nc", roundTrip("a\r\nb\nc".toByteArray()).text)
        assertEquals("a\nb", roundTrip("a\rb".toByteArray()).text)
    }

    @Test
    fun unrepresentableCharactersAreNotSaved() {
        assertNull(TextDocument.decode("año".toByteArray(Charsets.ISO_8859_1)).encode("€"))
    }

    @Test
    fun largeBinaryAndProtectedFilesAreReadOnly() {
        val large = ByteArray(TextDocument.MAX_EDIT_BYTES + 1) { 'a'.code.toByte() }
        assertEquals(TextReadOnly.TOO_LARGE, TextDocument.decode(large).readOnly)
        assertEquals(TextReadOnly.BINARY, TextDocument.decode(byteArrayOf(1, 0, 2)).readOnly)
        assertEquals(TextReadOnly.NOT_WRITABLE, TextDocument.decode("a".toByteArray(), writable = false).readOnly)
        assertNull(TextDocument.decode(large.copyOf(TextDocument.MAX_EDIT_BYTES)).readOnly)
    }

    @Test
    fun aLargeFileCutInsideACharacterStaysUtf8() {
        val document = TextDocument.decode("é".repeat(TextDocument.MAX_EDIT_BYTES).toByteArray().copyOfRange(0, TextDocument.MAX_EDIT_BYTES + 1))
        assertEquals(Charsets.UTF_8, document.charset)
        val odd = TextDocument.decode(("a" + "é".repeat(TextDocument.MAX_EDIT_BYTES)).toByteArray())
        assertEquals(Charsets.UTF_8, odd.charset)
    }

    @Test
    fun typingAWordIsOneUndoStep() {
        var text = ""
        val history = EditHistory(text)
        for (c in "hola mundo") {
            text += c
            history.record(text)
        }
        fun apply(edit: EditHistory.Edit?) {
            text = text.replaceRange(edit!!.start, edit.start + edit.length, edit.text)
        }
        apply(history.undo())
        assertEquals("hola ", text)
        apply(history.undo())
        assertEquals("hola", text)
        apply(history.undo())
        assertEquals("", text)
        assertFalse(history.canUndo)
        apply(history.redo())
        assertEquals("hola", text)
        history.record("holaX")
        assertFalse(history.canRedo)
    }

    @Test
    fun findAndReplace() {
        assertEquals(listOf(0, 2), TextSearch.matches("aaAA", "aa"))
        assertEquals(0, TextSearch.indexFrom(listOf(0, 2), 3))
        assertEquals("b b", TextSearch.replaceAll("a A", "a", "b"))
        assertEquals(listOf(0, 2, 3), TextSearch.lineStarts("a\n\nb").toList())
    }
}
