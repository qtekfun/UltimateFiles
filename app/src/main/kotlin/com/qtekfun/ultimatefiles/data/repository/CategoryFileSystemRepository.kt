package com.qtekfun.ultimatefiles.data.repository

import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.core.model.StorageVolume
import com.qtekfun.ultimatefiles.domain.repository.FileSystemRepository
import com.qtekfun.ultimatefiles.domain.usecase.Category
import com.qtekfun.ultimatefiles.domain.usecase.CategoryPaths
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Finds the files of a category. The items carry real local paths, so every operation on them works as usual. */
fun interface CategorySource {
    fun files(category: Category): List<FileItem>
}

/** Presents each [Category] as a read-only folder. Only listing it and naming it happen here. */
class CategoryFileSystemRepository(
    private val source: CategorySource,
    private val label: (Category) -> String,
) : FileSystemRepository {

    override suspend fun volumes(): List<StorageVolume> = emptyList()

    override suspend fun listFiles(uriOrPath: String): Result<List<FileItem>> = query(uriOrPath) { source.files(it) }

    override suspend fun stat(uriOrPath: String): Result<FileItem> =
        query(uriOrPath) { FileItem(uriOrPath, label(it), true, 0L, 0L, null, isWritable = false) }

    override suspend fun parentOf(uriOrPath: String): String? = null

    override suspend fun createDirectory(parentUriOrPath: String, name: String): Result<FileItem> = readOnly()

    override suspend fun createFile(parentUriOrPath: String, name: String, mimeType: String): Result<FileItem> = readOnly()

    override suspend fun delete(items: List<FileItem>): Result<Unit> = readOnly()

    override suspend fun rename(item: FileItem, newName: String): Result<FileItem> = readOnly()

    override suspend fun openInput(item: FileItem): Result<InputStream> = readOnly()

    override suspend fun openOutput(
        parentUriOrPath: String,
        name: String,
        mimeType: String,
        overwrite: Boolean,
    ): Result<OutputStream> = readOnly()

    private fun <T> readOnly(): Result<T> = Result.failure(IOException("A category is not a folder"))

    private suspend fun <T> query(path: String, block: (Category) -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            val category = CategoryPaths.categoryOf(path) ?: throw IOException("Unknown category: $path")
            Result.success(block(category))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
