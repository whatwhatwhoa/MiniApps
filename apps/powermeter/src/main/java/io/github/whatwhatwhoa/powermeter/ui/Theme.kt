package io.github.whatwhatwhoa.powermeter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import io.github.whatwhatwhoa.stack.LightStack
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackTheme
import io.github.whatwhatwhoa.stack.TNUM

/** The two colors that carry meaning: power into the battery, and out of it. */
@Immutable
data class Flow(val charge: Color, val drain: Color)

val LocalFlow = staticCompositionLocalOf { Flow(LightStack.good, LightStack.s1) }

/** Tabular figures, so a changing number doesn't jiggle its neighbors. */
val Numbers = TextStyle(fontFeatureSettings = TNUM)

/** The series' Stack look; charging in the good green, draining in the s1 orange. */
@Composable
fun PowerMeterTheme(content: @Composable () -> Unit) {
    StackTheme {
        val c = Stack.colors
        CompositionLocalProvider(LocalFlow provides Flow(charge = c.good, drain = c.s1), content = content)
    }
}
