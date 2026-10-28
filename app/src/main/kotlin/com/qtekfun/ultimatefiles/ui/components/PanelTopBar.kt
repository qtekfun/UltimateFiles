package com.qtekfun.ultimatefiles.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The top bar of a panel: navigation icon, title and actions in one row, looking like Material's small top app bar.
 *
 * It replaces `TopAppBar` inside the panels on purpose. Material's bar asks its title for the text baseline while it
 * measures, and when the pager swipes to a panel that has just been composed that forces Compose 1.12 to place the
 * breadcrumb's lazy list during the measure pass, which crashes with "LayoutNode … not found in RectList". This row never
 * asks for alignment lines, so there is nothing to force.
 */
@Composable
fun PanelTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    navigationIconColor: Color = MaterialTheme.colorScheme.onSurface,
    actionIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    // The container fills the area behind the status bar; the row itself stays below it.
    Surface(modifier = modifier, color = containerColor) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(windowInsets)
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides navigationIconColor) { navigationIcon() }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                CompositionLocalProvider(LocalContentColor provides titleColor) {
                    ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() }
                }
            }
            CompositionLocalProvider(LocalContentColor provides actionIconColor) {
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}

private val BAR_HEIGHT = 64.dp
