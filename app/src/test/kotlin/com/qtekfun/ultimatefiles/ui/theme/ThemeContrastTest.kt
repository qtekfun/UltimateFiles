package com.qtekfun.ultimatefiles.ui.theme

import com.qtekfun.ultimatefiles.ui.browser.tileColor
import com.qtekfun.ultimatefiles.core.util.FileKind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.qtekfun.ultimatefiles.core.model.AccentColor
import org.junit.Assert.assertTrue
import org.junit.Test

/** Text must keep at least 4.5:1 against what it sits on, whatever accent is chosen. */
class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val (low, high) = listOf(a.luminance(), b.luminance()).sorted()
        return (high + 0.05f) / (low + 0.05f)
    }

    @Test
    fun `white icons are readable on every file tile`() {
        FileKind.entries.mapNotNull { it.tileColor() }.forEach { assertTrue("$it", contrast(Color.White, it) >= 4.5f) }
    }

    @Test
    fun `every accent keeps text readable in light and dark`() {
        for (accent in AccentColor.entries) {
            for (dark in listOf(false, true)) {
                val scheme = appColorScheme(accent, dark)
                val header = topBarColors(scheme, dark)
                val pairs = mapOf(
                    "button" to (scheme.onPrimary to scheme.primary),
                    "accent text" to (scheme.primary to scheme.surface),
                    "accent text on a card" to (scheme.primary to scheme.surfaceContainer),
                    "accent text on a dialog" to (scheme.primary to scheme.surfaceContainerHigh),
                    "selected row" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
                    "header" to (header.content to header.container),
                    "path chip" to (header.content to header.chip),
                    "current path chip" to (header.container to header.indicator),
                    "selection bar" to (SelectionBarContent to SelectionBarContainer),
                )
                pairs.forEach { (name, pair) ->
                    val ratio = contrast(pair.first, pair.second)
                    assertTrue("$accent dark=$dark $name is $ratio", ratio >= 4.5f)
                }
            }
        }
    }
}
