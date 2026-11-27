package com.qtekfun.ultimatefiles.ui.viewer

import com.qtekfun.ultimatefiles.data.repository.LocalFileSystemRepository
import kotlinx.coroutines.Dispatchers
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

@OptIn(ExperimentalCoroutinesApi::class)
class TextEditorViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun editUndoRedoSaveAndExit() = runBlocking {
        val file = folder.newFile("a.txt").apply { writeText("one\r\ntwo one\r\n") }
        val viewModel = TextEditorViewModel(LocalFileSystemRepository(folder.root, "test", mimeOf = { null }), file.path)
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
