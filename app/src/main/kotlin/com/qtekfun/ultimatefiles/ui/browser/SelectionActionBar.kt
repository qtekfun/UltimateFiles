package com.qtekfun.ultimatefiles.ui.browser

import com.qtekfun.ultimatefiles.ui.components.dockBottomInsets
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.ui.components.PanelTopBar
import com.qtekfun.ultimatefiles.ui.theme.SelectionBarContainer
import com.qtekfun.ultimatefiles.ui.theme.SelectionBarContent

/** Contextual action bar that replaces the top bar while items are selected: close, counter and select all. */
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    PanelTopBar(
        modifier = modifier,
        windowInsets = windowInsets,
        containerColor = SelectionBarContainer,
        titleColor = SelectionBarContent,
        navigationIconColor = SelectionBarContent,
        actionIconColor = SelectionBarContent,
        title = { Text(stringResource(R.string.selection_count, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_selection))
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.action_select_all))
            }
        },
    )
}

/**
 * The actions on the selected items, docked under the list: the usual ones in sight and an overflow with the rest,
 * some of which only make sense for a single item.
 */
@Composable
fun SelectionDockBar(
    selectedCount: Int,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onProperties: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    canExtract: Boolean,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.windowInsetsPadding(dockBottomInsets)) {
            DockAction(Icons.Filled.ContentCopy, R.string.action_copy, onCopy)
            DockAction(Icons.Filled.ContentCut, R.string.action_cut, onCut)
            DockAction(Icons.Filled.Delete, R.string.action_delete, onDelete)
            DockAction(Icons.Filled.Share, R.string.action_share, onShare)
            Box(Modifier.weight(1f)) {
                Row { DockAction(Icons.Filled.MoreVert, R.string.action_more, { menuOpen = true }) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    val single = selectedCount == 1
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        enabled = single,
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_compress)) },
                        onClick = { menuOpen = false; onCompress() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_extract)) },
                        enabled = canExtract,
                        onClick = { menuOpen = false; onExtract() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_properties)) },
                        enabled = single,
                        onClick = { menuOpen = false; onProperties() },
                    )
                }
            }
        }
    }
}

/** An icon with its name under it, sharing the width of the bar equally; read out once, by the name. */
@Composable
private fun RowScope.DockAction(icon: ImageVector, label: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null)
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
