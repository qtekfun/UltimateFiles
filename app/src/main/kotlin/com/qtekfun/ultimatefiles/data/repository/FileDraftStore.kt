package com.qtekfun.ultimatefiles.data.repository

import com.qtekfun.ultimatefiles.domain.repository.DraftStore
import java.io.File
import java.security.MessageDigest

/** One file per edited document in [directory], which is private to the app. */
class FileDraftStore(private val directory: File) : DraftStore {
    override suspend fun load(path: String): String? =
        ioResult { draftFor(path).takeIf { it.isFile }?.readText() }.getOrNull()

    override suspend fun save(path: String, text: String) {
        ioResult {
            directory.mkdirs()
            val draft = draftFor(path)
            val partial = File(directory, draft.name + ".part")
            partial.writeText(text)
            if (!partial.renameTo(draft)) partial.delete()
        }
    }

    override suspend fun delete(path: String) {
        ioResult { draftFor(path).delete() }
    }

    private fun draftFor(path: String) = File(
        directory,
        MessageDigest.getInstance("SHA-256").digest(path.toByteArray()).joinToString("") { "%02x".format(it) },
    )
}
