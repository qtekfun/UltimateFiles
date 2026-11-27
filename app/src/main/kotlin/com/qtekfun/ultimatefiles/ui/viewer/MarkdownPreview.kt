package com.qtekfun.ultimatefiles.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.domain.usecase.InlineStyle
import com.qtekfun.ultimatefiles.domain.usecase.Markdown
import com.qtekfun.ultimatefiles.domain.usecase.MarkdownBlock
import com.qtekfun.ultimatefiles.domain.usecase.StyledText

@Composable
internal fun MarkdownPreview(source: String, modifier: Modifier) {
    val blocks = remember(source) { Markdown.parse(source) }
    val type = MaterialTheme.typography
    val shade = MaterialTheme.colorScheme.surfaceVariant
    val link = MaterialTheme.colorScheme.primary

    @Composable
    fun Styled(content: StyledText, style: TextStyle = type.bodyLarge, modifier: Modifier = Modifier) {
        Text(annotate(content, shade, link), style = style, modifier = modifier)
    }

    SelectionContainer {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(blocks) { block ->
                when (block) {
                    is MarkdownBlock.Heading -> Styled(
                        block.content,
                        when (block.level) {
                            1 -> type.headlineLarge
                            2 -> type.headlineMedium
                            3 -> type.headlineSmall
                            else -> type.titleMedium
                        },
                    )
                    is MarkdownBlock.Paragraph -> Styled(block.content)
                    is MarkdownBlock.ListItem -> Row(Modifier.padding(start = (16 * block.depth).dp)) {
                        Text(block.marker, style = type.bodyLarge, modifier = Modifier.padding(end = 8.dp))
                        Styled(block.content)
                    }
                    is MarkdownBlock.Quote -> Styled(
                        block.content,
                        type.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        Modifier.fillMaxWidth().background(shade).padding(8.dp),
                    )
                    is MarkdownBlock.Code -> Box(Modifier.fillMaxWidth().background(shade).horizontalScroll(rememberScrollState())) {
                        Text(block.text, style = type.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                    }
                    MarkdownBlock.Rule -> HorizontalDivider()
                }
            }
        }
    }
}

private fun annotate(content: StyledText, shade: Color, link: Color): AnnotatedString {
    val builder = AnnotatedString.Builder(content.text)
    content.spans.forEach {
        val style = when (it.style) {
            InlineStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            InlineStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            InlineStyle.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = shade)
            InlineStyle.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            InlineStyle.LINK -> SpanStyle(color = link, textDecoration = TextDecoration.Underline)
        }
        builder.addStyle(style, it.start, it.end)
    }
    return builder.toAnnotatedString()
}
