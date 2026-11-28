package com.qtekfun.ultimatefiles.core.util

import android.webkit.MimeTypeMap

object MimeTypes {
    const val OCTET_STREAM = "application/octet-stream"
    const val APK = "application/vnd.android.package-archive"

    /** Whether the file is an installable package: by its type or, as backends often report a generic one, its name. */
    fun isApk(name: String, mimeType: String?): Boolean = mimeType == APK || name.endsWith(".apk", ignoreCase = true)

    /** Best-effort MIME type from the file extension, or null when unknown. */
    fun fromName(name: String): String? {
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(extension)
    }
}
