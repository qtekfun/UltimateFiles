package com.qtekfun.ultimatefiles.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class SafeSaveTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun localReplaceSwapsTheContentAndLeavesNoTemporaryFile() = runTest {
        val file = folder.newFile("note.txt").apply { writeText("old content") }
        val repository = LocalFileSystemRepository(folder.root, "test", mimeOf = { null })
        val item = repository.stat(file.path).getOrThrow()

        repository.replaceContent(item, "new".toByteArray()).getOrThrow()

        assertEquals("new", file.readText())
        assertEquals(listOf("note.txt"), folder.root.list()!!.toList())
    }

    @Test
    fun failedOverwriteRestoresThePreviousContent() {
        var content = "original".toByteArray()
        var failures = 1
        val guard = GuardedOverwrite(folder.newFolder("backups"))

        val result = runCatching {
            guard.overwrite("doc", "replacement".toByteArray(), read = { content }) {
                if (failures-- > 0) {
                    content = ByteArray(0)
                    throw IOException("full")
                }
                content = it
            }
        }

        assertTrue(result.isFailure)
        assertEquals("original", String(content))
        assertNull(guard.pendingBackup("doc"))
    }

    @Test
    fun theBackupStaysWhenTheContentCannotBeRestored() {
        var content = "original".toByteArray()
        val dir = File(folder.root, "backups")
        val guard = GuardedOverwrite(dir)

        runCatching {
            guard.overwrite("doc", "replacement".toByteArray(), read = { content }) {
                content = ByteArray(0)
                throw IOException("unplugged")
            }
        }
        assertEquals("original", String(guard.pendingBackup("doc")!!))

        guard.overwrite("doc", "again".toByteArray(), read = { content }) { content = it }
        assertNull(guard.pendingBackup("doc"))
    }
}
