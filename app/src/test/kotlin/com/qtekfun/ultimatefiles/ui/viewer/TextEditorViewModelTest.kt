package com.qtekfun.ultimatefiles.ui.viewer

import com.qtekfun.ultimatefiles.data.repository.FileDraftStore
import com.qtekfun.ultimatefiles.data.repository.LocalFileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TextEditorViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun editor(path: String) = TextEditorViewModel(
        LocalFileSystemRepository(folder.root, "test", mimeOf = { null }),
        path,
        FileDraftStore(File(folder.root, "drafts")),
    )

    @Test
    fun unsavedTextSurvivesTheEditorBeingDestroyed() = runBlocking {
        val file = folder.newFile("b.txt").apply { writeText("saved") }
        val first = editor(file.path)
        first.state.first { !it.loading }
        first.onEvent(EditorEvent.ReplaceAll("saved", "unsaved"))
        first.onEvent(EditorEvent.Persist)
        val drafts = FileDraftStore(File(folder.root, "drafts"))
        while (drafts.load(file.path) == null) delay(10)

        val second = editor(file.path)
        second.state.first { !it.loading }
        assertEquals("unsaved", second.field.text.toString())
        assertTrue(second.state.value.dirty)
        second.onEvent(EditorEvent.Undo)
        assertEquals("saved", second.field.text.toString())
        assertFalse(second.state.value.dirty)
    }

    @Test
    fun editUndoRedoSaveAndExit() = runBlocking {
        val file = folder.newFile("a.txt").apply { writeText("one\r\ntwo one\r\n") }
        val viewModel = editor(file.path)
        viewModel.state.first { !it.loading }

        viewModel.onEvent(EditorEvent.ReplaceAll("one", "1"))
        assertEquals("1\ntwo 1\n", viewModel.field.text.toString())
        viewModel.onEvent(EditorEvent.Undo)
        assertFalse(viewModel.state.value.dirty)
        viewModel.onEvent(EditorEvent.Redo)
        assertTrue(viewModel.state.value.dirty)

        viewModel.onEvent(EditorEvent.Exit)
        assertTrue(viewModel.state.value.confirmExit)
        viewModel.onEvent(EditorEvent.SaveAndExit)
        viewModel.state.first { it.closed }

        assertEquals("1\r\ntwo 1\r\n", file.readText())
    }
}
