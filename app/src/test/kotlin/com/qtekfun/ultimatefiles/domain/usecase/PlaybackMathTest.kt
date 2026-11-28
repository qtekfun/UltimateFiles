package com.qtekfun.ultimatefiles.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackMathTest {
    @Test
    fun timeLabels() {
        assertEquals("0:00", PlaybackMath.time(-5))
        assertEquals("1:05", PlaybackMath.time(65_000))
        assertEquals("1:00:01", PlaybackMath.time(3_601_000))
    }

    @Test
    fun skippingStaysInsideTheFile() {
        assertEquals(0, PlaybackMath.skip(3_000, -10_000, 60_000))
        assertEquals(60_000, PlaybackMath.skip(55_000, 10_000, 60_000))
        assertEquals(65_000, PlaybackMath.skip(55_000, 10_000, 0))
    }
}
