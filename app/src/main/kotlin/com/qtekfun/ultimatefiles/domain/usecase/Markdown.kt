package com.qtekfun.ultimatefiles.domain.usecase

enum class InlineStyle { BOLD, ITALIC, CODE, STRIKE, LINK }

data class Span(val start: Int, val end: Int, val style: InlineStyle)

/** Text without its Markdown marks, and the ranges they styled. */
data class StyledText(val text: String, val spans: List<Span> = emptyList())

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val content: StyledText) : MarkdownBlock
    data class Paragraph(val content: StyledText) : MarkdownBlock
    data class ListItem(val marker: String, val depth: Int, val content: StyledText) : MarkdownBlock
    data class Quote(val content: StyledText) : MarkdownBlock
    data class Code(val text: String) : MarkdownBlock
    data object Rule : MarkdownBlock
}

/**
 * The common subset of Markdown, for previews: headings, paragraphs, lists, quotes, fenced code, rules and inline
 * emphasis, code and links. Nothing is fetched or executed; HTML and images stay as text.
 */
object Markdown {
    private val heading = Regex("""^(#{1,6})\s+(.*?)\s*#*$""")
    private val item = Regex("""^(\s*)([-*+]|\d{1,9}[.)])\s+(.*)$""")
    private val rule = Regex("""^\s*(?:(?:-\s*){3,}|(?:\*\s*){3,}|(?:_\s*){3,})$""")
    private val inline = Regex(
        """`([^`]+)`|\*\*(.+?)\*\*|__(.+?)__|~~(.+?)~~|\*([^*\s][^*]*)\*|(?<!\w)_([^_\s][^_]*)_(?!\w)|(?<!!)\[([^\]]*)]\([^)]*\)""",
    )

    fun parse(source: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = mutableListOf<String>()
        val quote = mutableListOf<String>()
        var code: MutableList<String>? = null

        fun flush() {
            if (paragraph.isNotEmpty()) blocks += MarkdownBlock.Paragraph(styled(paragraph.joinToString(" ")))
            if (quote.isNotEmpty()) blocks += MarkdownBlock.Quote(styled(quote.joinToString(" ")))
            paragraph.clear()
            quote.clear()
        }

        for (line in source.lineSequence()) {
            val fence = line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")
            val open = code
            if (open != null) {
                if (fence) {
                    blocks += MarkdownBlock.Code(open.joinToString("\n"))
                    code = null
                } else {
                    open += line
                }
                continue
            }
            val title = heading.find(line)
            val listed = item.find(line)
            when {
                fence -> {
                    flush()
                    code = mutableListOf()
                }
                line.isBlank() -> flush()
                title != null -> {
                    flush()
                    blocks += MarkdownBlock.Heading(title.groupValues[1].length, styled(title.groupValues[2]))
                }
                rule.matches(line) -> {
                    flush()
                    blocks += MarkdownBlock.Rule
                }
                listed != null -> {
                    flush()
                    val (indent, marker, content) = listed.destructured
                    val bullet = if (marker.length == 1 && marker[0] in "-*+") "•" else marker
                    blocks += MarkdownBlock.ListItem(bullet, indent.length / 2, styled(content))
                }
                line.startsWith(">") -> {
                    if (paragraph.isNotEmpty()) flush()
                    quote += line.removePrefix(">").trim()
                }
                else -> {
                    if (quote.isNotEmpty()) flush()
                    paragraph += line.trim()
                }
            }
        }
        // A fence that is never closed still shows its content.
        code?.let { blocks += MarkdownBlock.Code(it.joinToString("\n")) }
        flush()
        return blocks
    }

    fun styled(source: String): StyledText {
        val text = StringBuilder()
        val spans = mutableListOf<Span>()
        var last = 0
        for (match in inline.findAll(source)) {
            text.append(source, last, match.range.first)
            val group = (1..7).first { match.groups[it] != null }
            val style = when (group) {
                1 -> InlineStyle.CODE
                2, 3 -> InlineStyle.BOLD
                4 -> InlineStyle.STRIKE
                5, 6 -> InlineStyle.ITALIC
                else -> InlineStyle.LINK
            }
            val raw = match.groupValues[group]
            val inner = if (style == InlineStyle.CODE) StyledText(raw) else styled(raw)
            val start = text.length
            text.append(inner.text)
            spans += Span(start, text.length, style)
            inner.spans.forEach { spans += Span(start + it.start, start + it.end, it.style) }
            last = match.range.last + 1
        }
        text.append(source, last, source.length)
        return StyledText(text.toString(), spans)
    }
}
