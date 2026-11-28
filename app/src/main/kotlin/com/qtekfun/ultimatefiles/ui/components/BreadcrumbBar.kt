package com.qtekfun.ultimatefiles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.core.model.BreadcrumbSegment
import com.qtekfun.ultimatefiles.ui.theme.LocalTopBarColors

/** Segmented path as chips, drawn on the header colour; tapping any segment but the last jumps to that directory. */
@Composable
fun BreadcrumbBar(
    segments: List<BreadcrumbSegment>,
    onSegmentClick: (BreadcrumbSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Keep the current directory visible when the path is longer than the bar.
    LaunchedEffect(segments) {
        if (segments.isNotEmpty()) listState.scrollToItem(segments.lastIndex)
    }
    LazyRow(state = listState, modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        itemsIndexed(segments, key = { _, segment -> segment.path }) { index, segment ->
            val isCurrent = index == segments.lastIndex
            val colors = LocalTopBarColors.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.content,
                    )
                }
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.labelLarge,
                    // The current folder is the filled chip; the ones that lead back to it are tinted.
                    color = if (isCurrent) colors.container else colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isCurrent) colors.indicator else colors.chip)
                        .clickable(enabled = !isCurrent) { onSegmentClick(segment) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
