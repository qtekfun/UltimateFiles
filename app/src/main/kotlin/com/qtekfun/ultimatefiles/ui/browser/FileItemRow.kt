package com.qtekfun.ultimatefiles.ui.browser

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.domain.usecase.ArchiveFormat
import com.qtekfun.ultimatefiles.domain.usecase.ArchivePaths
import com.qtekfun.ultimatefiles.core.util.FileKind
import com.qtekfun.ultimatefiles.core.util.formatBytes
import com.qtekfun.ultimatefiles.core.util.kind
import com.qtekfun.ultimatefiles.ui.dualpanel.DragDropState
import java.text.DateFormat
import java.util.Date

/** Quick actions offered by the per-row menu (long press or the three-dot button). */
enum class FileItemAction { SELECT, OPEN_WITH, COPY, CUT, EXTRACT, COMPRESS, RENAME, DELETE, PROPERTIES }

/**
 * Whether [this] makes sense for [item]: archives are only extracted outside archives, and nothing is packed from
 * inside one. Starting a selection from the menu is pointless for an item that is already selected.
 */
internal fun FileItemAction.appliesTo(item: FileItem, selected: Boolean = false): Boolean {
    val insideArchive = ArchivePaths.isArchivePath(item.path)
    return when (this) {
        FileItemAction.SELECT -> !selected
        FileItemAction.OPEN_WITH -> !item.isDirectory
        FileItemAction.EXTRACT -> !item.isDirectory && !insideArchive && ArchiveFormat.of(item.name) != null
        FileItemAction.COMPRESS -> !insideArchive
        else -> true
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    selected: Boolean,
    dragAndDropEnabled: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onAction: (FileItemAction) -> Unit,
    onDragStart: () -> Unit,
    onDropInside: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Entries in the folder, when known. */
    entryCount: Int? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }

    val dropTarget = remember(item.path) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                onDropInside(item.path)
                return true
            }
        }
    }
    // The item the menu is about stays highlighted while the menu is open.
    val highlighted = MaterialTheme.colorScheme.secondaryContainer
    val container by animateColorAsState(if (selected || menuOpen) highlighted else highlighted.copy(alpha = 0f), label = "row")
    var rowModifier = Modifier
        .fillMaxWidth()
        .background(container)
        .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
    if (dragAndDropEnabled && item.isDirectory) {
        rowModifier = rowModifier.dragAndDropTarget(
            shouldStartDragAndDrop = { it.isOurDrag() },
            target = dropTarget,
        )
    }

    var leadingModifier: Modifier = Modifier
    if (dragAndDropEnabled) {
        leadingModifier = leadingModifier.dragAndDropSource { _ ->
            onDragStart()
            DragAndDropTransferData(ClipData.newPlainText(DragDropState.CLIP_LABEL, item.name))
        }
    }

    Column(modifier) {
        Row(
            modifier = rowModifier.heightIn(min = 64.dp).padding(start = 12.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileLeading(item, selected, leadingModifier.clickable(onClick = onToggleSelection))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row {
                    Text(
                        text = item.amount(entryCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = item.date(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    FileItemAction.entries
                        .filter { it.appliesTo(item, selected) }
                        .forEach { action ->
                            DropdownMenuItem(
                                text = { Text(stringResource(action.labelRes())) },
                                onClick = {
                                    menuOpen = false
                                    onAction(action)
                                },
                            )
                        }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** True when the drag was started by one of our rows (as opposed to another app). */
@OptIn(ExperimentalFoundationApi::class)
fun DragAndDropEvent.isOurDrag(): Boolean =
    toAndroidDragEvent().clipDescription?.label?.toString() == DragDropState.CLIP_LABEL

internal fun FileItemAction.labelRes(): Int = when (this) {
    FileItemAction.SELECT -> R.string.action_select
    FileItemAction.OPEN_WITH -> R.string.action_open_with
    FileItemAction.COPY -> R.string.action_copy
    FileItemAction.CUT -> R.string.action_cut
    FileItemAction.EXTRACT -> R.string.action_extract
    FileItemAction.COMPRESS -> R.string.action_compress
    FileItemAction.RENAME -> R.string.action_rename
    FileItemAction.DELETE -> R.string.action_delete
    FileItemAction.PROPERTIES -> R.string.action_properties
}

internal fun FileKind.icon(): ImageVector = when (this) {
    FileKind.FOLDER -> Icons.Filled.Folder
    FileKind.IMAGE -> Icons.Filled.Image
    FileKind.VIDEO -> Icons.Filled.Movie
    FileKind.AUDIO -> Icons.Filled.AudioFile
    FileKind.PDF -> Icons.Filled.PictureAsPdf
    FileKind.TEXT -> Icons.Filled.Description
    FileKind.ARCHIVE -> Icons.Filled.FolderZip
    FileKind.APK -> Icons.Filled.Android
    FileKind.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
}

/** Size of a file, or how many entries a folder holds when that is known. */
@Composable
internal fun FileItem.amount(entryCount: Int?): String = when {
    !isDirectory -> formatBytes(sizeBytes)
    entryCount != null -> pluralStringResource(R.plurals.item_count, entryCount, entryCount)
    else -> ""
}

private fun FileItem.date(): String =
    if (lastModifiedMillis > 0) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastModifiedMillis))
    } else {
        ""
    }
