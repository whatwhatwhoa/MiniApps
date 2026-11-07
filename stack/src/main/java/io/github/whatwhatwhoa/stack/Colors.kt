package io.github.whatwhatwhoa.stack

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The Stack color tokens, copied value for value from the Stack Design Guide (`#stack-tokens`).
 * Each has one role: accent = selected, primary, focus; handle = things you grab or watch (warn
 * pill, callout bar); danger / good = status; mat = the dark cutting mat; s0…s4 = series told
 * apart by color, always next to a text label. Never recolor one: add a token to both themes.
 */
@Immutable
data class StackColors(
    val bg: Color,
    val panel: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val grid: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val mat: Color,
    val matLine: Color,
    val handle: Color,
    val danger: Color,
    val dangerSoft: Color,
    val good: Color,
    val s0: Color,
    val s1: Color,
    val s2: Color,
    val s3: Color,
    val s4: Color,
    val sOther: Color,
    val fail: Color,
    val dark: Boolean,
) {
    /** Series colors in order, fixed per item so a filter never repaints one. */
    val series: List<Color> get() = listOf(s0, s1, s2, s3, s4)
}

val LightStack = StackColors(
    bg = Color(0xFFE9ECEE), panel = Color(0xFFFAFBFB), ink = Color(0xFF1A2026),
    muted = Color(0xFF5B6670), line = Color(0xFFD2D8DD), grid = Color(0xFFE3E7EA),
    accent = Color(0xFF0E6767), accentInk = Color(0xFFFFFFFF), accentSoft = Color(0xFFD6EBEA),
    mat = Color(0xFF262B30), matLine = Color(0xFF353C43), handle = Color(0xFFF2A93B),
    danger = Color(0xFFB23A2E), dangerSoft = Color(0xFFF6E1DE), good = Color(0xFF2F8A55),
    s0 = Color(0xFF00917F), s1 = Color(0xFFC8691A), s2 = Color(0xFF2E6FD0),
    s3 = Color(0xFFB5317A), s4 = Color(0xFF7A45C0), sOther = Color(0xFF8A939B),
    fail = Color(0xFF7D8790), dark = false,
)

val DarkStack = StackColors(
    bg = Color(0xFF14181B), panel = Color(0xFF1C2125), ink = Color(0xFFE6EAED),
    muted = Color(0xFF98A3AC), line = Color(0xFF2E353B), grid = Color(0xFF262C31),
    accent = Color(0xFF4FB3B0), accentInk = Color(0xFF0B1414), accentSoft = Color(0xFF1E3534),
    mat = Color(0xFF0D1012), matLine = Color(0xFF22282D), handle = Color(0xFFF2A93B),
    danger = Color(0xFFE0685B), dangerSoft = Color(0xFF3A2220), good = Color(0xFF5CC48A),
    s0 = Color(0xFF1E9F8D), s1 = Color(0xFFD0782C), s2 = Color(0xFF4A82E0),
    s3 = Color(0xFFD2558F), s4 = Color(0xFF9670DC), sOther = Color(0xFF6F7880),
    fail = Color(0xFF7E8992), dark = true,
)
