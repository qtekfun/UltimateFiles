package com.qtekfun.ultimatefiles.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/**
 * Space that a bar docked at the bottom edge must leave free. Foldables and tablets draw a taskbar or dock that
 * is not always reported as a navigation bar, so the tappable-element and cutout insets are added to it.
 */
val dockBottomInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars
        .union(WindowInsets.tappableElement)
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Bottom)

/** [dockBottomInsets] plus the side bars and cutouts, for a bar that spans the whole screen width. */
val dockFullWidthInsets: WindowInsets
    @Composable get() = dockBottomInsets.union(
        WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    )
