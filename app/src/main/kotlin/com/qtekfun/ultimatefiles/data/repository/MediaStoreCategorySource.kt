package com.qtekfun.ultimatefiles.data.repository

import android.content.ContentResolver
import android.provider.MediaStore
import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.core.util.MimeTypes
import com.qtekfun.ultimatefiles.domain.usecase.Category
import java.io.File

/** Asks the system's media index, which already knows every file on shared storage, instead of walking it. */
class MediaStoreCategorySource(private val resolver: ContentResolver) : CategorySource {

    @Suppress("DEPRECATION") // DATA is the only way to the real path, which the app can open with its storage access.
    override fun files(category: Category): List<FileItem> {
        val columns = arrayOf(MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MIME_TYPE)
        val (selection, arguments) = filterOf(category)
        val limit = if (category == Category.RECENTS) RECENT_LIMIT else CATEGORY_LIMIT
        val found = mutableListOf<FileItem>()
        resolver.query(
            MediaStore.Files.getContentUri("external"),
            columns,
            selection,
            arguments,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            while (found.size < limit && cursor.moveToNext()) {
                val path = cursor.getString(0) ?: continue
                val file = File(path)
                // The index may lag behind: leave out what was deleted or moved, and folders.
                if (!file.isFile) continue
                val mime = if (MimeTypes.isApk(file.name, null)) MimeTypes.APK else cursor.getString(1) ?: MimeTypes.fromName(file.name)
                found += FileItem(path, file.name, false, file.length(), file.lastModified(), mime, isHidden = file.isHidden)
            }
        }
        return found
    }

    @Suppress("DEPRECATION")
    private fun filterOf(category: Category): Pair<String, Array<String>?> {
        val mime = MediaStore.Files.FileColumns.MIME_TYPE
        return when (category) {
            Category.IMAGES -> "$mime LIKE ?" to arrayOf("image/%")
            Category.VIDEO -> "$mime LIKE ?" to arrayOf("video/%")
            Category.AUDIO -> "$mime LIKE ?" to arrayOf("audio/%")
            Category.DOCUMENTS ->
                "$mime LIKE ? OR $mime IN (${DOCUMENT_TYPES.joinToString { "?" }})" to (listOf("text/%") + DOCUMENT_TYPES).toTypedArray()
            Category.APKS -> "${MediaStore.Files.FileColumns.DATA} LIKE ?" to arrayOf("%.apk")
            Category.RECENTS -> "$mime IS NOT NULL" to null
        }
    }

    private companion object {
        const val RECENT_LIMIT = 100
        const val CATEGORY_LIMIT = 5_000
        val DOCUMENT_TYPES = listOf(
            "application/pdf",
            "application/rtf",
            "application/epub+zip",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
        )
    }
}
