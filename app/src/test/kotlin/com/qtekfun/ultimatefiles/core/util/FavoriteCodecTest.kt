package com.qtekfun.ultimatefiles.core.util

import com.qtekfun.ultimatefiles.core.datastore.FavoriteCodec
import com.qtekfun.ultimatefiles.core.model.Favorite
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteCodecTest {
    @Test
    fun `favorites survive a round trip in order`() {
        val favorites = listOf(Favorite("/storage/a, \"b\"", "a, \"b\"", true), Favorite("dav://nas/c.txt", "c.txt", false))
        assertEquals(favorites, FavoriteCodec.decode(FavoriteCodec.encode(favorites)))
    }

    @Test
    fun `an unreadable value gives none`() {
        assertEquals(emptyList<Favorite>(), FavoriteCodec.decode("not json"))
    }
}
