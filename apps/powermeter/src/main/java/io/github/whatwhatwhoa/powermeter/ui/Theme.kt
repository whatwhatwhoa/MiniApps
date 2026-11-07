package io.github.whatwhatwhoa.powermeter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The two colors that carry meaning: power into the battery, and out of it. */
@Immutable
data class Flow(val charge: Color, val drain: Color)

val LocalFlow = staticCompositionLocalOf { Flow(Color(0xFF16A34A), Color(0xFFD97706)) }

private val lightFlow = Flow(charge = Color(0xFF15803D), drain = Color(0xFFC2410C))
private val darkFlow = Flow(charge = Color(0xFF4ADE80), drain = Color(0xFFFBBF24))

/** Tabular figures, so a changing number doesn't jiggle its neighbors. */
val Numbers = TextStyle(fontFeatureSettings = "tnum")

private val type = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Medium, fontSize = 64.sp, lineHeight = 68.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun PowerMeterTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    // The phone's own wallpaper colors for the chrome (Android 12+, which is every device we run on).
    val colors = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    androidx.compose.runtime.CompositionLocalProvider(LocalFlow provides if (dark) darkFlow else lightFlow) {
        MaterialTheme(colorScheme = colors, typography = type, content = content)
    }
}
