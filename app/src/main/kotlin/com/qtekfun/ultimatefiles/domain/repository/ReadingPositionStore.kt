package com.qtekfun.ultimatefiles.domain.repository

/** Where the user was in a paged document: the first visible page and how far into it, as a fraction of its height. */
data class ReadingPosition(val page: Int, val fraction: Float)

interface ReadingPositionStore {
    suspend fun load(path: String): ReadingPosition?

    suspend fun save(path: String, position: ReadingPosition)
}
