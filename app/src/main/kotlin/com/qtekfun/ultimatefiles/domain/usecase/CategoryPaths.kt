package com.qtekfun.ultimatefiles.domain.usecase

/** The shortcuts that gather files of one kind from all of the device's storage. */
enum class Category { IMAGES, VIDEO, AUDIO, DOCUMENTS, APKS, RECENTS }

/**
 * A category is browsed like a folder, at `category://<name>`. It is not one: the files in it stay where they are, so
 * nothing can be created, pasted or packed there.
 */
object CategoryPaths {
    const val SCHEME = "category://"

    fun pathOf(category: Category) = SCHEME + category.name.lowercase()

    fun isCategoryPath(path: String) = path.startsWith(SCHEME)

    fun categoryOf(path: String): Category? =
        if (isCategoryPath(path)) Category.entries.firstOrNull { pathOf(it) == path } else null
}
