package com.qtekfun.ultimatefiles.ui.main

import com.qtekfun.ultimatefiles.ui.browser.tileColor
import com.qtekfun.ultimatefiles.ui.browser.icon
import com.qtekfun.ultimatefiles.data.system.labelRes
import com.qtekfun.ultimatefiles.core.util.FileKind
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Schedule
import com.qtekfun.ultimatefiles.domain.usecase.Category
import com.qtekfun.ultimatefiles.domain.usecase.CategoryPaths
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.BuildConfig
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.core.model.ConnectionProblem
import com.qtekfun.ultimatefiles.core.model.Favorite
import com.qtekfun.ultimatefiles.core.model.StorageKind
import com.qtekfun.ultimatefiles.core.model.StorageVolume
import com.qtekfun.ultimatefiles.core.util.RemoteAccounts
import com.qtekfun.ultimatefiles.core.util.formatBytes
import com.qtekfun.ultimatefiles.ui.components.connectionShortReason
import com.qtekfun.ultimatefiles.ui.components.dockBottomInsets
import com.qtekfun.ultimatefiles.ui.theme.LocalTopBarColors

/** The side drawer: header, storage volumes as cards, shortcuts and favorites, and the app's own places at the bottom. */
@Composable
fun AppDrawer(
    volumes: List<StorageVolume>,
    unreachable: Map<String, ConnectionProblem>,
    shortcuts: List<Shortcut>,
    favorites: List<Favorite>,
    onOpen: (String) -> Unit,
    onOpenFavorite: (Favorite) -> Unit,
    onRemoveFavorite: (Favorite) -> Unit,
    onAddStorage: () -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (StorageVolume) -> Unit,
    onRenameAccount: (StorageVolume) -> Unit,
    onEditAccount: (StorageVolume) -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    onAnalyze: (StorageVolume) -> Unit,
    onEject: () -> Unit,
) {
    // The header and the bottom row keep clear of the system bars themselves, so the header's colour reaches the top.
    ModalDrawerSheet(windowInsets = DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)) {
        Column(Modifier.fillMaxHeight()) {
            DrawerHeader()
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                volumes.forEach { volume ->
                    VolumeCard(
                        volume = volume,
                        problem = RemoteAccounts.idOfVolume(volume.id)?.let { unreachable[it] },
                        onOpen = { onOpen(volume.rootPath) },
                    ) {
                        if (volume.kind == StorageKind.NETWORK) {
                            AccountMenu(
                                onEdit = { onEditAccount(volume) },
                                onRename = { onRenameAccount(volume) },
                                onRemove = { onRemoveAccount(volume) },
                            )
                        } else {
                            IconButton(onClick = { onAnalyze(volume) }) {
                                Icon(Icons.Filled.DataUsage, contentDescription = stringResource(R.string.action_analyze_size))
                            }
                            if (volume.isEjectable) {
                                IconButton(onClick = onEject) {
                                    Icon(Icons.Filled.Eject, contentDescription = stringResource(R.string.drawer_eject))
                                }
                            }
                        }
                    }
                }
                DrawerRow(Icons.Filled.Add, stringResource(R.string.drawer_add_storage), onAddStorage)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                CategoryGrid { onOpen(CategoryPaths.pathOf(it)) }
                if (shortcuts.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                shortcuts.forEach { shortcut ->
                    DrawerRow(shortcut.kind.icon(), stringResource(shortcut.labelRes), { onOpen(shortcut.path) })
                }
                if (favorites.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(R.string.drawer_favorites),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                    )
                    favorites.forEach { favorite ->
                        DrawerRow(
                            icon = if (favorite.isDirectory) Icons.Filled.Folder else Icons.Filled.Star,
                            label = favorite.name,
                            onClick = { onOpenFavorite(favorite) },
                        ) {
                            IconButton(onClick = { onRemoveFavorite(favorite) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove_favorite))
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(Modifier.windowInsetsPadding(dockBottomInsets)) {
                BottomAction(Icons.Filled.Cloud, R.string.drawer_add_account, onAddAccount)
                BottomAction(Icons.Filled.History, R.string.history_title, onShowHistory)
                BottomAction(Icons.Filled.Settings, R.string.settings_title, onShowSettings)
            }
        }
    }
}

/** The app's icon, name and version on the header colour. */
@Composable
private fun DrawerHeader() {
    val colors = LocalTopBarColors.current
    Surface(color = colors.container, contentColor = colors.content, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.statusBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorResource(R.color.ic_launcher_background)),
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** A storage volume: its name and how full it is, with [actions] at the end. */
@Composable
private fun VolumeCard(
    volume: StorageVolume,
    problem: ConnectionProblem?,
    onOpen: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(volume.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(volume.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                    if (problem != null) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (problem != null) {
                    Text(
                        text = LocalResources.current.connectionShortReason(problem.kind),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    SpaceUsage(volume)
                }
            }
            actions()
        }
    }
}

/** The category shortcuts, three to a row, in the colours of the files they gather. */
@Composable
private fun CategoryGrid(onOpen: (Category) -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp)) {
        Category.entries.chunked(3).forEach { row ->
            Row {
                row.forEach { category ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = Role.Button) { onOpen(category) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val color = category.kind()?.tileColor()
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(color ?: MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = category.kind()?.icon() ?: Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = if (color != null) Color.White else MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Text(
                            text = stringResource(category.labelRes()),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** The kind of file a category gathers; null for the recent ones, which are of any kind. */
private fun Category.kind(): FileKind? = when (this) {
    Category.IMAGES -> FileKind.IMAGE
    Category.VIDEO -> FileKind.VIDEO
    Category.AUDIO -> FileKind.AUDIO
    Category.DOCUMENTS -> FileKind.TEXT
    Category.APKS -> FileKind.APK
    Category.RECENTS -> null
}

/** A thin bar of how full the volume is and how much room is left; nothing for volumes that cannot tell. */
@Composable
private fun SpaceUsage(volume: StorageVolume) {
    val total = volume.totalBytes ?: return
    val free = volume.freeBytes ?: return
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { ((total - free).toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Text(
            text = stringResource(R.string.drawer_space_free, formatBytes(free), formatBytes(total)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DrawerRow(icon: ImageVector, label: String, onClick: () -> Unit, badge: (@Composable () -> Unit)? = null) {
    NavigationDrawerItem(
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        icon = { Icon(icon, contentDescription = null) },
        badge = badge,
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun RowScope.BottomAction(icon: ImageVector, label: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null)
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The three-dot menu of a network account in the drawer. */
@Composable
private fun AccountMenu(onEdit: () -> Unit, onRename: () -> Unit, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.account_edit)) },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    open = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    open = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.account_remove)) },
                leadingIcon = { Icon(Icons.Filled.CloudOff, contentDescription = null) },
                onClick = {
                    open = false
                    onRemove()
                },
            )
        }
    }
}

private fun StorageVolume.icon(): ImageVector = when (kind) {
    StorageKind.INTERNAL -> Icons.Filled.PhoneAndroid
    StorageKind.NETWORK -> Icons.Filled.Cloud
    else -> Icons.Filled.Usb
}

private fun ShortcutKind.icon(): ImageVector = when (this) {
    ShortcutKind.DOWNLOADS -> Icons.Filled.Download
    ShortcutKind.DOCUMENTS -> Icons.Filled.Description
    ShortcutKind.PHOTOS -> Icons.Filled.PhotoLibrary
}
