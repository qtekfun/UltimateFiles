package com.qtekfun.ultimatefiles.domain.usecase

import com.qtekfun.ultimatefiles.core.model.FileItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSiblingsTest {
    private fun item(name: String, directory: Boolean = false, hidden: Boolean = false) = FileItem(
        path = "/p/$name",
        name = name,
        isDirectory = directory,
        sizeBytes = 0,
        lastModifiedMillis = 0,
        mimeType = if (name.endsWith(".jpg")) "image/jpeg" else "text/plain",
        isWritable = true,
        isHidden = hidden,
    )

    @Test
    fun onlyVisibleImagesOrderedByName() {
        val folder = listOf(item("c.jpg"), item("a.txt"), item("B.jpg"), item("d.jpg", directory = true), item(".h.jpg", hidden = true))
        val (images, index) = ImageSiblings.of(item("c.jpg"), folder)
        assertEquals(listOf("B.jpg", "c.jpg"), images.map { it.name })
        assertEquals(1, index)
    }

    @Test
    fun anUnlistedImageIsShownAlone() {
        assertEquals(listOf("x.jpg"), ImageSiblings.of(item("x.jpg"), listOf(item("c.jpg"))).first.map { it.name })
    }
}
