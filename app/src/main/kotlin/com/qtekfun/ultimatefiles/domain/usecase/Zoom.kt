package com.qtekfun.ultimatefiles.domain.usecase

import kotlin.math.sqrt

/** Arithmetic shared by the zoomable viewers. */
object Zoom {
    /** Offset into scaled content that keeps the point under [focus] (viewport coordinates) in place. */
    fun anchored(scroll: Float, focus: Float, factor: Float): Float = (scroll + focus) * factor - focus

    /** [scroll] limited to what content of size [content] allows in a viewport of size [viewport]. */
    fun clampScroll(scroll: Float, content: Float, viewport: Float): Float = scroll.coerceIn(0f, maxOf(0f, content - viewport))

    /** Translation, measured from the centred position, limited so that no gap appears at the edges. */
    fun clampPan(pan: Float, content: Float, viewport: Float): Float {
        val limit = maxOf(0f, (content - viewport) / 2)
        return pan.coerceIn(-limit, limit)
    }

    /** Size to render a page of [pageWidth] x [pageHeight] at [targetWidth], never above [maxPixels] pixels. */
    fun renderSize(pageWidth: Int, pageHeight: Int, targetWidth: Int, maxPixels: Int): Pair<Int, Int> {
        val ratio = pageHeight.toFloat() / pageWidth.coerceAtLeast(1)
        val width = minOf(targetWidth.toFloat(), sqrt(maxPixels / ratio.coerceAtLeast(0.001f))).toInt().coerceAtLeast(1)
        return width to (width * ratio).toInt().coerceAtLeast(1)
    }

    /** Zero-based page for what the user typed as a page number, or null when it is not one of [count] pages. */
    fun pageIndex(input: String, count: Int): Int? = input.trim().toIntOrNull()?.takeIf { it in 1..count }?.minus(1)
}
