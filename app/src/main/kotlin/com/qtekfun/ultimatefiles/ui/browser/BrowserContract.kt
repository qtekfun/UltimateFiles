package com.qtekfun.ultimatefiles.ui.browser

import com.qtekfun.ultimatefiles.domain.usecase.Category
import com.qtekfun.ultimatefiles.domain.usecase.CategoryPaths
import androidx.annotation.StringRes
import com.qtekfun.ultimatefiles.core.model.BreadcrumbSegment
import com.qtekfun.ultimatefiles.core.model.ConnectionProblem
import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.core.model.HashState
import com.qtekfun.ultimatefiles.core.model.SortField
import com.qtekfun.ultimatefiles.core.model.SortOrder
import com.qtekfun.ultimatefiles.core.model.StorageVolume
import com.qtekfun.ultimatefiles.core.model.ViewMode

data class BrowserState(
    val currentPath: String? = null,
    val parentPath: String? = null,
    val breadcrumb: List<BreadcrumbSegment> = emptyList(),
    /** The volume the current folder is on, for its name and free space; null until read or when there is none. */
    val volume: StorageVolume? = null,
    /** Entries inside each listed folder, by path, filled after the listing where it is cheap to know. */
    val folderCounts: Map<String, Int> = emptyMap(),
    val favoritePaths: Set<String> = emptySet(),
    val items: List<FileItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sortOrder: SortOrder = SortOrder(),
    val viewMode: ViewMode = ViewMode.LIST,
    val dialog: BrowserDialog? = null,
    /** Null when the search field is closed; otherwise the name filter applied to [items]. */
    val searchQuery: String? = null,
) {
    val visibleItems: List<FileItem>
        get() = searchQuery?.takeIf { it.isNotBlank() }
            ?.let { query -> items.filter { it.name.contains(query.trim(), ignoreCase = true) } } ?: items
    /** A category gathers files from many places: nothing can be created, pasted or packed in it. */
    val isCategory: Boolean get() = currentPath?.let(CategoryPaths::isCategoryPath) == true

    /** The recent files are about when, so they stay newest first whatever order folders are in. */
    val isSortable: Boolean get() = currentPath != CategoryPaths.pathOf(Category.RECENTS)
    val selectedItems: List<FileItem> get() = items.filter { it.path in selectedPaths }
    val isSelecting: Boolean get() = selectedPaths.isNotEmpty()
}

sealed interface BrowserDialog {
    data object NewFolder : BrowserDialog
    data object NewFile : BrowserDialog
    data class Rename(val item: FileItem) : BrowserDialog
    data class ConfirmDelete(val items: List<FileItem>) : BrowserDialog
    data class Compress(val items: List<FileItem>) : BrowserDialog
    data class Properties(val item: FileItem, val hash: HashState = HashState.Idle) : BrowserDialog

    /** Explains why installing needs a system permission, before leaving for the settings to grant it. */
    data class InstallPermission(val item: FileItem, val chooser: Boolean) : BrowserDialog
}

/** Everything the screen can ask the view model to do. */
sealed interface BrowserEvent {
    data class Navigate(val path: String) : BrowserEvent
    data object NavigateUp : BrowserEvent
    data object Refresh : BrowserEvent
    data object ToggleSearch : BrowserEvent
    data object ToggleViewMode : BrowserEvent
    data class SetSearchQuery(val query: String) : BrowserEvent
    data class OpenItem(val item: FileItem) : BrowserEvent
    data class OpenWith(val item: FileItem) : BrowserEvent
    data class ToggleSelection(val item: FileItem) : BrowserEvent
    data object SelectAll : BrowserEvent
    data object ClearSelection : BrowserEvent
    data class SortBy(val field: SortField) : BrowserEvent
    data class Copy(val items: List<FileItem>) : BrowserEvent
    data class Cut(val items: List<FileItem>) : BrowserEvent
    data object Paste : BrowserEvent
    data object CancelClipboard : BrowserEvent
    data class Share(val items: List<FileItem>) : BrowserEvent
    data class RequestDelete(val items: List<FileItem>) : BrowserEvent
    data class RequestRename(val item: FileItem) : BrowserEvent
    data object RequestNewFolder : BrowserEvent
    data class RequestCompress(val items: List<FileItem>) : BrowserEvent
    data class Extract(val items: List<FileItem>) : BrowserEvent
    data object RequestNewFile : BrowserEvent
    data class ShowProperties(val item: FileItem) : BrowserEvent
    data class ConfirmName(val name: String) : BrowserEvent
    data object ConfirmDelete : BrowserEvent
    data object ComputeHash : BrowserEvent
    data object DismissDialog : BrowserEvent

    /** [item] cannot be opened until the user allows installing apps from here. */
    data class RequestInstallPermission(val item: FileItem, val chooser: Boolean) : BrowserEvent
    data object ConfirmInstallPermission : BrowserEvent

    /** The user came back from the system settings. */
    data class InstallPermissionResult(val granted: Boolean) : BrowserEvent
    data class ToggleFavorite(val item: FileItem) : BrowserEvent
    data class StartDrag(val item: FileItem) : BrowserEvent
    data class DropItems(val targetPath: String) : BrowserEvent

    /** Shows [item] in its own folder, selected, so the usual actions apply to it (used by the size analysis). */
    data class Reveal(val item: FileItem) : BrowserEvent
}

/** One-shot results that need an Android context (intents, snackbars). */
sealed interface BrowserEffect {
    data class OpenFile(val item: FileItem, val chooser: Boolean) : BrowserEffect
    data class OpenViewer(val item: FileItem) : BrowserEffect
    data class ShareFiles(val items: List<FileItem>) : BrowserEffect
    data class Message(@StringRes val resId: Int, val detail: String? = null) : BrowserEffect

    /** A server could not be used; [retryPath] is the folder that was being opened. */
    data class ConnectionNotice(val problem: ConnectionProblem, val accountLabel: String?, val retryPath: String) : BrowserEffect
}
