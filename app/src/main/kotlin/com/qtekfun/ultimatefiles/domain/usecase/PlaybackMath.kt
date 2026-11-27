package com.qtekfun.ultimatefiles.domain.usecase

/** Arithmetic and labels for the media player. */
object PlaybackMath {
    const val SKIP_MILLIS = 10_000L
    val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    /** `m:ss`, or `h:mm:ss` from one hour on. */
    fun time(millis: Long): String {
        val seconds = millis.coerceAtLeast(0) / 1000
        val hours = seconds / 3600
        val minutes = seconds / 60 % 60
        val rest = (seconds % 60).toString().padStart(2, '0')
        return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$rest" else "$minutes:$rest"
    }

    /** [position] moved by [delta], inside a file of [duration] (zero or less while unknown). */
    fun skip(position: Long, delta: Long, duration: Long): Long {
        val target = (position + delta).coerceAtLeast(0)
        return if (duration > 0) target.coerceAtMost(duration) else target
    }
}
