package com.qtekfun.ultimatefiles.core.model

/** Identifies one browsing panel; the user can create more than the initial two. */
data class PanelId(val value: Int) {
    companion object {
        val LEFT = PanelId(0)
        val RIGHT = PanelId(1)
        val DEFAULTS = listOf(LEFT, RIGHT)
    }
}

enum class ViewMode { LIST, GRID }

/** Where the bar that picks a panel sits: above or below the panels. */
enum class PanelBarPosition { TOP, BOTTOM }

/** [AMOLED] is the dark theme with pure black backgrounds. */
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/** The colour the app is dressed in, unless dynamic colours are on. */
enum class AccentColor { CYAN, BLUE, PURPLE, SLATE }

/** A file or folder the user keeps at hand in the drawer. */
data class Favorite(val path: String, val name: String, val isDirectory: Boolean)

enum class SortField { NAME, SIZE, DATE }

data class SortOrder(val field: SortField = SortField.NAME, val ascending: Boolean = true)

data class UserPreferences(
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOrder: SortOrder = SortOrder(),
    /** The open panels, in display order; there are always at least two. */
    val panelIds: List<PanelId> = PanelId.DEFAULTS,
    /** Last opened directory path per panel; null means "use the default root". */
    val lastDirectoryPaths: Map<PanelId, String?> = emptyMap(),
    val panelBarPosition: PanelBarPosition = PanelBarPosition.TOP,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You colours from the wallpaper (Android 12+). */
    val dynamicColor: Boolean = false,
    val accentColor: AccentColor = AccentColor.CYAN,
    /** In the order they were added. */
    val favorites: List<Favorite> = emptyList(),
    /** Read back and checksum every copied file (slower, safer for big or irreplaceable files). */
    val verifyCopies: Boolean = false,
    /** Draw thumbnails of photos on WebDAV, SFTP and SMB accounts too; it downloads each photo, so it is off by default. */
    val thumbnailsOnNetwork: Boolean = false,
)
