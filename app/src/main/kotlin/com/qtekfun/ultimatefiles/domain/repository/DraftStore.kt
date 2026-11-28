package com.qtekfun.ultimatefiles.domain.repository

/** Unsaved editor text, kept on disk so it survives the editor being destroyed. */
interface DraftStore {
    suspend fun load(path: String): String?

    suspend fun save(path: String, text: String)

    suspend fun delete(path: String)
}
