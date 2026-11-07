package io.github.whatwhatwhoa.stack

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** UI face. Bundled, cut to Latin (tools/cut_fonts.py), so it works offline and starts at once. */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
    Font(R.font.instrument_sans_bold, FontWeight.Bold),
)

/** Data face: figures, codes, counts in the header. */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

/** Tabular figures, so a changing number doesn't jiggle its neighbors. */
const val TNUM = "tnum"

/**
 * The Stack type scale (guide §4), in sp. Labels are uppercase: pass the text through
 * [String.uppercase], since a style can't change case.
 */
object StackType {
    val h1 = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Bold, fontSize = 19.sp,
        lineHeight = 24.sp, letterSpacing = (-0.01).em)
    val h2 = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        lineHeight = 20.sp)
    val body = TextStyle(fontFamily = InstrumentSans, fontSize = 15.sp, lineHeight = 1.45.em)
    val note = TextStyle(fontFamily = InstrumentSans, fontSize = 13.sp, lineHeight = 1.45.em)
    val sub = TextStyle(fontFamily = InstrumentSans, fontSize = 12.5.sp, lineHeight = 1.35.em,
        fontFeatureSettings = TNUM)
    val label = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
        letterSpacing = 0.07.em)
    val tileValue = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Bold, fontSize = 30.sp,
        lineHeight = 1.1.em, letterSpacing = (-0.02).em, fontFeatureSettings = TNUM)
    val tileValuePhone = tileValue.copy(fontSize = 24.sp)
    val tileUnit = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val table = TextStyle(fontFamily = InstrumentSans, fontSize = 13.sp, lineHeight = 1.35.em,
        fontFeatureSettings = TNUM)
    val th = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp,
        letterSpacing = 0.06.em)
    val button = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val seg = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    val pill = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
    val tab = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val mono = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, fontFeatureSettings = TNUM)
    val monoBody = TextStyle(fontFamily = JetBrainsMono, fontSize = 15.sp, fontFeatureSettings = TNUM)
}

/** Material's roles in the Stack faces, so stock controls (switches, menus, fields) match. */
internal val StackTypography: Typography = Typography().run {
    fun TextStyle.ui() = copy(fontFamily = InstrumentSans)
    copy(
        displayLarge = displayLarge.ui().copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
        displayMedium = displayMedium.ui().copy(fontWeight = FontWeight.Medium),
        displaySmall = displaySmall.ui().copy(fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.ui().copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.ui().copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.ui().copy(fontWeight = FontWeight.Bold),
        titleLarge = StackType.h1,
        titleMedium = StackType.h2,
        titleSmall = titleSmall.ui().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.ui().copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = bodyMedium.ui().copy(fontSize = 13.sp, lineHeight = 19.sp),
        bodySmall = bodySmall.ui().copy(fontSize = 12.5.sp, lineHeight = 17.sp),
        labelLarge = StackType.button,
        labelMedium = labelMedium.ui().copy(fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.ui().copy(fontWeight = FontWeight.SemiBold),
    )
}
