package com.qtekfun.ultimatefiles.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerExtrasTest {
    private fun highlighted(name: String, text: String) =
        SyntaxHighlighter.tokens(name, text).map { text.substring(it.start, it.end) to it.kind }

    @Test
    fun tokensDoNotOverlapStringsAndComments() {
        assertEquals(
            listOf("val" to TokenKind.KEYWORD, "\"// no\"" to TokenKind.STRING, "// 1" to TokenKind.COMMENT),
            highlighted("a.kt", "val s = \"// no\" // 1"),
        )
        assertEquals(listOf("<a" to TokenKind.TAG, ">" to TokenKind.TAG), highlighted("a.xml", "<a>"))
        assertTrue(highlighted("a.txt", "val").isEmpty())
        assertTrue(highlighted("a.kt", " ".repeat(SyntaxHighlighter.MAX_CHARS + 1)).isEmpty())
    }

    @Test
    fun markdownBlocksAndInlineStyles() {
        val blocks = Markdown.parse("# Title\n\none\ntwo\n- item\n```\n**raw**\n```\n---")
        assertEquals(MarkdownBlock.Heading(1, StyledText("Title")), blocks[0])
        assertEquals(MarkdownBlock.Paragraph(StyledText("one two")), blocks[1])
        assertEquals(MarkdownBlock.ListItem("•", 0, StyledText("item")), blocks[2])
        assertEquals(MarkdownBlock.Code("**raw**"), blocks[3])
        assertEquals(MarkdownBlock.Rule, blocks[4])
        assertEquals(
            StyledText("a b c", listOf(Span(2, 3, InlineStyle.BOLD), Span(4, 5, InlineStyle.LINK))),
            Markdown.styled("a **b** [c](https://example.org)"),
        )
    }

    @Test
    fun onlyFilesOfUnknownTypeAreAskedAbout() {
        assertTrue(ViewerKind.isUnknown("data.xyz", null))
        assertTrue(ViewerKind.isUnknown("blob", "application/octet-stream"))
        assertFalse(ViewerKind.isUnknown("a.txt", null))
        assertFalse(ViewerKind.isUnknown("a.apk", "application/vnd.android.package-archive"))
    }
}
