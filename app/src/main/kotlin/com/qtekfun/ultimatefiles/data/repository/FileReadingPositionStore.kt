package com.qtekfun.ultimatefiles.data.repository

import com.qtekfun.ultimatefiles.domain.repository.ReadingPosition
import com.qtekfun.ultimatefiles.domain.repository.ReadingPositionStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

/** The last [limit] documents read, one line each in [file], most recent last. Paths are stored hashed. */
class FileReadingPositionStore(private val file: File, private val limit: Int = 200) : ReadingPositionStore {
    private val lock = Mutex()

    override suspend fun load(path: String): ReadingPosition? = lock.withLock {
        val key = keyOf(path)
        ioResult {
            val fields = lines().lastOrNull { it.startsWith("$key\t") }?.split('\t') ?: return@ioResult null
            ReadingPosition(fields[1].toInt(), fields[2].toFloat())
        }.getOrNull()
    }

    override suspend fun save(path: String, position: ReadingPosition) {
        lock.withLock {
            val key = keyOf(path)
            ioResult {
                val others = lines().filterNot { it.startsWith("$key\t") }
                val updated = (others + "$key\t${position.page}\t${position.fraction}").takeLast(limit)
                file.writeText(updated.joinToString("\n"))
            }
        }
    }

    private fun lines() = if (file.isFile) file.readLines() else emptyList()

    private fun keyOf(path: String) =
        MessageDigest.getInstance("SHA-256").digest(path.toByteArray()).joinToString("") { "%02x".format(it) }
}
