package com.qtekfun.ultimatefiles.domain.usecase

import com.qtekfun.ultimatefiles.data.repository.FileReadingPositionStore
import com.qtekfun.ultimatefiles.domain.repository.ReadingPosition
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ZoomTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun zoomKeepsTheFocusAndStaysInBounds() {
        assertEquals(100f, Zoom.anchored(0f, 100f, 2f), 0f)
        assertEquals(100f, Zoom.clampScroll(500f, 200f, 100f), 0f)
        assertEquals(-50f, Zoom.clampPan(-500f, 200f, 100f), 0f)
        assertEquals(0f, Zoom.clampPan(30f, 80f, 100f), 0f)
    }

    @Test
    fun renderSizeNeverExceedsThePixelBudget() {
        assertEquals(1000 to 2000, Zoom.renderSize(100, 200, 1000, 10_000_000))
        val (width, height) = Zoom.renderSize(100, 200, 10_000, 2_000_000)
        assertTrue(width * height <= 2_000_000)
    }

    @Test
    fun typedPageNumbers() {
        assertEquals(0, Zoom.pageIndex(" 1 ", 3))
        assertNull(Zoom.pageIndex("4", 3))
        assertNull(Zoom.pageIndex("0", 3))
        assertNull(Zoom.pageIndex("x", 3))
    }

    @Test
    fun readingPositionsAreRememberedAndLimited() = runTest {
        val store = FileReadingPositionStore(File(folder.root, "positions"), limit = 2)
        store.save("a", ReadingPosition(1, 0.5f))
        store.save("b", ReadingPosition(2, 0f))
        store.save("a", ReadingPosition(3, 0.25f))
        assertEquals(ReadingPosition(3, 0.25f), store.load("a"))
        store.save("c", ReadingPosition(1, 0f))
        assertNull(store.load("b"))
    }
}
