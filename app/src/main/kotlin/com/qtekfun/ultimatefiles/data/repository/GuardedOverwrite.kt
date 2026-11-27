package com.qtekfun.ultimatefiles.data.repository

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Overwrites a document that can only be rewritten in place (storage access framework), without losing it if that
 * fails: the previous content is first stored in [backupDir], the result is read back, and the previous content is put
 * back when it does not match. The backup outlives a killed process and is then offered through [pendingBackup].
 */
class GuardedOverwrite(private val backupDir: File) {

    fun overwrite(key: String, bytes: ByteArray, read: () -> ByteArray, write: (ByteArray) -> Unit) {
        val backup = backupFor(key)
        // A backup left by an interrupted save is the good copy: the current content must not replace it.
        if (!backup.exists()) store(backup, read())
        try {
            write(bytes)
            if (!read().contentEquals(bytes)) throw IOException("The saved content does not match")
        } catch (e: Exception) {
            try {
                val previous = backup.readBytes()
                write(previous)
                if (read().contentEquals(previous)) backup.delete()
            } catch (restore: Exception) {
                e.addSuppressed(restore)
            }
            throw e
        }
        backup.delete()
    }

    /** The content a save that never finished was replacing, or null. */
    fun pendingBackup(key: String): ByteArray? = backupFor(key).takeIf { it.isFile }?.readBytes()

    fun discard(key: String) {
        backupFor(key).delete()
    }

    private fun store(backup: File, bytes: ByteArray) {
        if (!backupDir.isDirectory && !backupDir.mkdirs()) throw IOException("Cannot keep a backup copy")
        val partial = File(backupDir, backup.name + ".part")
        FileOutputStream(partial).use {
            it.write(bytes)
            it.fd.sync()
        }
        if (!partial.renameTo(backup)) throw IOException("Cannot keep a backup copy")
    }

    private fun backupFor(key: String) = File(
        backupDir,
        MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) },
    )
}
