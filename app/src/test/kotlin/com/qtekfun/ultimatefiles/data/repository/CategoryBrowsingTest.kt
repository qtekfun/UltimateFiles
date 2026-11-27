package com.qtekfun.ultimatefiles.data.repository

import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.domain.usecase.Category
import com.qtekfun.ultimatefiles.domain.usecase.CategoryPaths
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CategoryBrowsingTest {
    @get:Rule val tmp = TemporaryFolder()

    private val song = FileItem("/storage/song.mp3", "song.mp3", false, 1, 1, "audio/mpeg")
    private val categories = CategoryFileSystemRepository(
        source = { if (it == Category.AUDIO) listOf(song) else emptyList() },
        label = { it.name },
    )

    private fun router(): RoutingFileSystemRepository {
        val local = LocalFileSystemRepository(tmp.root, "Internal") { null }
        return RoutingFileSystemRepository(local, local, local, local, local, local, categories)
    }

    @Test
    fun `every category has its own path`() {
        Category.entries.forEach { assertEquals(it, CategoryPaths.categoryOf(CategoryPaths.pathOf(it))) }
        assertNull(CategoryPaths.categoryOf("category://nothing"))
        assertNull(CategoryPaths.categoryOf("/storage/emulated/0"))
    }

    @Test
    fun `a category lists through the router like a folder without a parent`() = runBlocking {
        val path = CategoryPaths.pathOf(Category.AUDIO)
        val router = router()

        assertEquals(listOf(song), router.listFiles(path).getOrThrow())
        assertEquals("AUDIO", router.stat(path).getOrThrow().name)
        assertNull(router.parentOf(path))
    }

    @Test
    fun `nothing can be created in a category`() = runBlocking {
        val path = CategoryPaths.pathOf(Category.AUDIO)

        assertTrue(router().createDirectory(path, "new").isFailure)
        assertTrue(router().openOutput(path, "a.txt", "text/plain", false).isFailure)
    }
}
