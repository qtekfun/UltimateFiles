package com.qtekfun.ultimatefiles.ui.browser

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import android.content.ClipData
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.core.util.kind
import com.qtekfun.ultimatefiles.ui.dualpanel.DragDropState

/** Grid counterpart of [FileItemRow]: same gestures (tap, long-press menu, icon tap selects) and drag and drop. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    selected: Boolean,
    dragAndDropEnabled: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onAction: (FileItemAction) -> Unit,
    onDragStart: () -> Unit,
    onDropInside: (String) -> Unit,
    modifier: Modifier = Modifier,
    entryCount: Int? = null,
    favorite: Boolean = false,
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

    val colors = MaterialTheme.colorScheme
    val highlighted = selected || menuOpen
    val container by animateColorAsState(if (highlighted) colors.secondaryContainer else colors.surfaceContainerHighest, label = "cell")
    val outline by animateColorAsState(if (selected) colors.primary else Color.Transparent, label = "cellOutline")
    var cell = modifier
        .padding(4.dp)
        .clip(MaterialTheme.shapes.medium)
        .background(container)
        .border(2.dp, outline, MaterialTheme.shapes.medium)
        .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
    if (dragAndDropEnabled && item.isDirectory) {
        cell = cell.dragAndDropTarget(shouldStartDragAndDrop = { it.isOurDrag() }, target = dropTarget)
    }

    var tileModifier: Modifier = Modifier
    if (dragAndDropEnabled) {
        tileModifier = tileModifier.dragAndDropSource { _ ->
            onDragStart()
            DragAndDropTransferData(ClipData.newPlainText(DragDropState.CLIP_LABEL, item.name))
        }
    }

    Box(cell) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FileTile(item, selected, tileModifier.clickable(onClick = onToggleSelection))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.amount(entryCount),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            FileItemAction.entries
                .filter { it.appliesTo(item, selected, favorite) }
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
