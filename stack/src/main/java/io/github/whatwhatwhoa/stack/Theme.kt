package io.github.whatwhatwhoa.stack

import android.app.Activity
import android.content.Context
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/** The header's "Theme: Auto / Light / Dark" cycle (guide rule 5). */
enum class ThemeMode(val label: String) {
    Auto("Auto"), Light("Light"), Dark("Dark");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

/** The theme choice, kept per app in its own "stack" preferences file. */
object StackPrefs {
    private fun prefs(context: Context) = context.getSharedPreferences("stack", Context.MODE_PRIVATE)

    fun themeMode(context: Context): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs(context).getString("theme", null) ?: "Auto") }
            .getOrDefault(ThemeMode.Auto)

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString("theme", mode.name).apply()
    }
}

@Immutable
class ThemeControl(val mode: ThemeMode, val set: (ThemeMode) -> Unit)

val LocalStackColors = staticCompositionLocalOf { LightStack }
val LocalThemeControl = staticCompositionLocalOf { ThemeControl(ThemeMode.Auto) {} }

/** Shorthand: `Stack.colors.accent`. */
object Stack {
    val colors: StackColors
        @Composable get() = LocalStackColors.current
}

private fun scheme(c: StackColors): ColorScheme {
    val base = if (c.dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.accent, onPrimary = c.accentInk,
        primaryContainer = c.accentSoft, onPrimaryContainer = c.ink,
        secondary = c.accent, onSecondary = c.accentInk,
        secondaryContainer = c.accentSoft, onSecondaryContainer = c.ink,
        tertiary = c.handle, onTertiary = Color(0xFF1A2026),
        tertiaryContainer = c.accentSoft, onTertiaryContainer = c.ink,
        background = c.bg, onBackground = c.ink,
        surface = c.panel, onSurface = c.ink,
        surfaceVariant = c.grid, onSurfaceVariant = c.muted,
        surfaceTint = c.panel,
        surfaceBright = c.panel, surfaceDim = c.bg,
        surfaceContainerLowest = c.panel, surfaceContainerLow = c.panel,
        surfaceContainer = c.panel, surfaceContainerHigh = c.panel,
        surfaceContainerHighest = c.grid,
        inverseSurface = c.ink, inverseOnSurface = c.panel, inversePrimary = c.accentSoft,
        outline = c.line, outlineVariant = c.grid,
        error = c.danger, onError = c.panel, errorContainer = c.dangerSoft, onErrorContainer = c.ink,
    )
}

/** Radii from the guide: 6 inside a segmented control, 8 buttons, 10 inner panels, 12 cards. */
private val StackShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

/**
 * The Stack theme: tokens, Material 3 mapped onto them, the Stack type, and the theme mode with
 * its saved choice. Status and navigation bar icons follow the theme, not only the system,
 * unless [systemBars] is false (an app whose page sets its own bars, such as one on paper).
 */
@Composable
fun StackTheme(systemBars: Boolean = true, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(StackPrefs.themeMode(context)) }
    val dark = when (mode) {
        ThemeMode.Auto -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colors = if (dark) DarkStack else LightStack

    val view = LocalView.current
    if (systemBars && !view.isInEditMode) {
        SideEffect {
            val bars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            (view.context as? Activity)?.window?.insetsController
                ?.setSystemBarsAppearance(if (dark) 0 else bars, bars)
        }
    }

    val control = remember(mode) {
        ThemeControl(mode) { next -> mode = next; StackPrefs.setThemeMode(context, next) }
    }
    CompositionLocalProvider(LocalStackColors provides colors, LocalThemeControl provides control) {
        MaterialTheme(colorScheme = scheme(colors), typography = StackTypography, shapes = StackShapes) {
            // Text without a color of its own takes the content color, which is black unless a
            // Surface or Scaffold sets it; Stack pages are plain boxes on bg, so set ink here.
            CompositionLocalProvider(LocalContentColor provides colors.ink, content = content)
        }
    }
}
