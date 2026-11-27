package com.qtekfun.ultimatefiles.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.qtekfun.ultimatefiles.core.model.AccentColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.core.model.ThemeMode

/** Rounder than the Material defaults, for a friendlier look: cards, dialogs and menus follow these. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** The Material scale with a little more weight on titles and labels, so headings stand out from the body text. */
private val AppTypography = Typography().let { base ->
    base.copy(
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** The tones one accent needs; everything else in the schemes is neutral and shared. */
private class AccentTones(
    val light: Color,
    val lightContainer: Color,
    val onLightContainer: Color,
    val dark: Color,
    val onDark: Color,
    val darkContainer: Color,
    val onDarkContainer: Color,
)

private fun AccentColor.tones(): AccentTones = when (this) {
    AccentColor.CYAN -> AccentTones(
        Color(0xFF00838F), Color(0xFFD3F0F3), Color(0xFF00363D),
        Color(0xFF4DD0E1), Color(0xFF00363D), Color(0xFF004F58), Color(0xFFC4F3F9),
    )
    AccentColor.BLUE -> AccentTones(
        Color(0xFF1E5AA8), Color(0xFFD8E6FA), Color(0xFF0A2A52),
        Color(0xFFA8C8FF), Color(0xFF08305F), Color(0xFF1B4680), Color(0xFFD6E3FF),
    )
    AccentColor.PURPLE -> AccentTones(
        Color(0xFF6A3FA0), Color(0xFFEADDF8), Color(0xFF2A1048),
        Color(0xFFD3B8F5), Color(0xFF3A1C66), Color(0xFF52327F), Color(0xFFEEDCFF),
    )
    AccentColor.SLATE -> AccentTones(
        Color(0xFF37474F), Color(0xFFDDE5E9), Color(0xFF101D23),
        Color(0xFFB0BEC5), Color(0xFF1C2A31), Color(0xFF374850), Color(0xFFD6E4EB),
    )
}

/** The colour that stands for [accent] in the picker. */
fun accentSwatch(accent: AccentColor): Color = accent.tones().light

/** The app's own scheme for [accent]: white or neutral dark surfaces with the accent on top. */
fun appColorScheme(accent: AccentColor, dark: Boolean): ColorScheme {
    val tones = accent.tones()
    return if (dark) {
        darkColorScheme(
            primary = tones.dark,
            onPrimary = tones.onDark,
            primaryContainer = tones.darkContainer,
            onPrimaryContainer = tones.onDarkContainer,
            inversePrimary = tones.light,
            secondary = tones.dark,
            onSecondary = tones.onDark,
            secondaryContainer = tones.darkContainer,
            onSecondaryContainer = tones.onDarkContainer,
            tertiary = Color(0xFFC3C5E8),
            onTertiary = Color(0xFF2C2F4C),
            tertiaryContainer = Color(0xFF434664),
            onTertiaryContainer = Color(0xFFE0E1FF),
            background = Color(0xFF121314),
            onBackground = Color(0xFFE3E3E3),
            surface = Color(0xFF121314),
            onSurface = Color(0xFFE3E3E3),
            surfaceVariant = Color(0xFF43484A),
            onSurfaceVariant = Color(0xFFC3C7C9),
            surfaceTint = tones.dark,
            surfaceDim = Color(0xFF121314),
            surfaceBright = Color(0xFF383A3C),
            surfaceContainerLowest = Color(0xFF0D0E0F),
            surfaceContainerLow = Color(0xFF1A1C1D),
            surfaceContainer = Color(0xFF1E2021),
            surfaceContainerHigh = Color(0xFF282A2C),
            surfaceContainerHighest = Color(0xFF333537),
            outline = Color(0xFF8D9294),
            outlineVariant = Color(0xFF43484A),
        )
    } else {
        // Containers stay white so that text in the accent keeps its contrast on menus, dialogs and cards.
        lightColorScheme(
            primary = tones.light,
            onPrimary = Color.White,
            primaryContainer = tones.lightContainer,
            onPrimaryContainer = tones.onLightContainer,
            inversePrimary = tones.dark,
            secondary = tones.light,
            onSecondary = Color.White,
            secondaryContainer = tones.lightContainer,
            onSecondaryContainer = tones.onLightContainer,
            tertiary = Color(0xFF4F5379),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFE0E1FF),
            onTertiaryContainer = Color(0xFF1B1E39),
            background = Color.White,
            onBackground = Color(0xFF1A1C1E),
            surface = Color.White,
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFECEFF0),
            onSurfaceVariant = Color(0xFF44494B),
            surfaceTint = tones.light,
            surfaceDim = Color(0xFFDADDDE),
            surfaceBright = Color.White,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color.White,
            surfaceContainer = Color.White,
            surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color(0xFFEEF1F2),
            outline = Color(0xFF6F7779),
            outlineVariant = Color(0xFFCBD2D4),
        )
    }
}

/** Colours of the header of the browser: the top bar, its chips and the panel tabs. */
@Immutable
data class TopBarColors(val container: Color, val content: Color, val chip: Color, val indicator: Color)

/** In light the header is filled with the accent; in dark it stays neutral and the accent only marks what is active. */
fun topBarColors(scheme: ColorScheme, dark: Boolean): TopBarColors =
    if (dark) {
        TopBarColors(scheme.surfaceContainer, scheme.onSurface, scheme.surfaceContainerHighest, scheme.primary)
    } else {
        TopBarColors(scheme.primary, scheme.onPrimary, lerp(scheme.primary, Color.Black, 0.18f), scheme.onPrimary)
    }

val LocalTopBarColors = staticCompositionLocalOf { topBarColors(appColorScheme(AccentColor.CYAN, false), false) }

/** The bar shown while items are selected, dark in every theme so that selecting is unmistakable. */
val SelectionBarContainer = Color(0xFF263238)
val SelectionBarContent = Color.White

/** Whether [mode] resolves to a dark appearance given the current system setting. */
fun resolveDarkTheme(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.AMOLED -> true
}

/** Pure black background and near-black containers so OLED pixels can switch off. */
fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF1F1F1F),
)

@Composable
fun UltimateFilesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    accent: AccentColor = AccentColor.CYAN,
    content: @Composable () -> Unit,
) {
    val darkTheme = resolveDarkTheme(themeMode, isSystemInDarkTheme())
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> appColorScheme(accent, darkTheme)
    }
    val colors = if (themeMode == ThemeMode.AMOLED) base.toAmoled() else base
    var header = topBarColors(colors, darkTheme)
    if (themeMode == ThemeMode.AMOLED) header = header.copy(container = Color.Black)
    CompositionLocalProvider(LocalTopBarColors provides header) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
