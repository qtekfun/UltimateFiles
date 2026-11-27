package com.qtekfun.ultimatefiles.domain.usecase

import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.core.util.MimeTypes

/** The images of a folder in the order they are swiped through, and where [current] is among them. */
object ImageSiblings {
    fun of(current: FileItem, folder: List<FileItem>): Pair<List<FileItem>, Int> {
        val images = folder
            .filter { !it.isDirectory && (it.path == current.path || (!it.isHidden && isImage(it))) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        val index = images.indexOfFirst { it.path == current.path }
        return if (index >= 0) images to index else listOf(current) to 0
    }

    private fun isImage(item: FileItem) =
        ViewerKind.of(item.name, item.mimeType ?: MimeTypes.fromName(item.name)) == ViewerKind.IMAGE
}
