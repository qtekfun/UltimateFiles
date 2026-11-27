package com.qtekfun.ultimatefiles.ui.dualpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.core.model.PanelBarPosition
import com.qtekfun.ultimatefiles.core.model.PanelId
import com.qtekfun.ultimatefiles.ui.components.dockBottomInsets

/**
 * The open panels as tabs named after their folders, for the one-panel-at-a-time layout. The button at the end holds
 * adding a panel and closing the current one.
 */
@Composable
fun PanelTabs(
    panels: List<PanelEntry>,
    activeIndex: Int,
    position: PanelBarPosition,
    onSelect: (PanelId) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atTop = position == PanelBarPosition.TOP
    val colors = panelBarColors(position)
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex, panels.size) {
        val visible = listState.layoutInfo.visibleItemsInfo
        // Leave the row still when the tab is already in sight.
        if (visible.drop(1).dropLast(1).none { it.index == activeIndex }) listState.animateScrollToItem(activeIndex)
    }
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = colors.container, contentColor = colors.content, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = if (atTop) Modifier.statusBarsPadding() else Modifier.windowInsetsPadding(dockBottomInsets),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(state = listState, modifier = Modifier.weight(1f).selectableGroup()) {
                itemsIndexed(panels, key = { _, entry -> entry.id.value }) { index, entry ->
                    val selected = index == activeIndex
                    Box(
                        modifier = Modifier
                            .widthIn(min = 72.dp, max = 200.dp)
                            .width(IntrinsicSize.Max)
                            .height(48.dp)
                            .selectable(selected = selected, role = Role.Tab) { onSelect(entry.id) },
                    ) {
                        Text(
                            text = entry.folderName(index),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
                        )
                        if (selected) {
                            Box(
                                Modifier
                                    .align(if (atTop) Alignment.BottomCenter else Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(colors.indicator),
                            )
                        }
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.VerticalSplit, contentDescription = stringResource(R.string.panel_menu))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.panel_new)) },
                        leadingIcon = { Icon(Icons.Filled.VerticalSplit, contentDescription = null) },
                        onClick = { menuOpen = false; onAdd() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.panel_close)) },
                        leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                        enabled = panels.size > MIN_PANELS,
                        onClick = { menuOpen = false; onClose() },
                    )
                }
            }
        }
    }
}
