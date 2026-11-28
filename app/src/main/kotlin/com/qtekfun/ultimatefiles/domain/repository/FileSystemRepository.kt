package com.qtekfun.ultimatefiles.domain.repository

import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.core.model.StorageVolume
import java.io.InputStream
import java.io.OutputStream

/**
 * Abstraction over any file backend (local paths, SAF, future WebDAV…).
 *
 * Implementations must perform all I/O on `Dispatchers.IO`, so callers can invoke
 * these from any dispatcher, including Main. Failures are returned as [Result.failure].
 */
interface FileSystemRepository {
    /** Storage roots currently available (internal, USB OTG…). */
    suspend fun volumes(): List<StorageVolume>

    suspend fun listFiles(uriOrPath: String): Result<List<FileItem>>

    suspend fun stat(uriOrPath: String): Result<FileItem>

    /**
     * How many entries the folder holds, or null when finding out is not cheap (anything over the network, the system
     * picker, archives). Only a backend that can answer from local storage overrides this.
     */
    suspend fun childCount(uriOrPath: String): Int? = null

    /** Parent directory, or null when [uriOrPath] is a volume root. */
    suspend fun parentOf(uriOrPath: String): String?

    suspend fun createDirectory(parentUriOrPath: String, name: String): Result<FileItem>

    suspend fun createFile(parentUriOrPath: String, name: String, mimeType: String): Result<FileItem>

    suspend fun delete(items: List<FileItem>): Result<Unit>

    suspend fun rename(item: FileItem, newName: String): Result<FileItem>

    /** Opens [item] for reading; the caller closes the stream. */
    suspend fun openInput(item: FileItem): Result<InputStream>

    /**
     * Creates (or truncates when [overwrite]) a file named [name] inside [parentUriOrPath]
     * and opens it for writing; the caller closes the stream.
     */
    suspend fun openOutput(
        parentUriOrPath: String,
        name: String,
        mimeType: String,
        overwrite: Boolean,
    ): Result<OutputStream>

    /**
     * Replaces the content of the existing file [item] with [bytes] so that a failure never leaves it truncated.
     * Backends that cannot promise that fail without touching the file.
     */
    suspend fun replaceContent(item: FileItem, bytes: ByteArray): Result<Unit> =
        Result.failure(UnsupportedOperationException("Editing is not supported here"))

    /** Previous content of [item] when a [replaceContent] was interrupted before finishing, else null. */
    suspend fun interruptedSaveBackup(item: FileItem): ByteArray? = null
}
