package io.github.whatwhatwhoa.dailynote.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextGeometricTransform
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * What one swipe erases. [words] are scratched out and fade; then [remove] leaves the text: the
 * words with the space beside them, or, when the line is left with nothing on it, the whole
 * line. [wholeLine] is that line's text, set only then: the lines below slide up into its place.
 */
data class Erase(val words: TextRange, val remove: TextRange, val wholeLine: TextRange?)

/**
 * The whole words a swipe from [x1] to [x2] (layout coordinates) crossed on visual [line]. A word
 * touched anywhere counts whole. Null when the swipe crossed only space.
 */
fun findErase(text: String, layout: TextLayoutResult, line: Int, x1: Float, x2: Float): Erase? {
    val lineStart = layout.getLineStart(line)
    val lineEnd = layout.getLineEnd(line, visibleEnd = true)
    if (lineStart >= lineEnd) return null
    val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2
    var s = layout.getOffsetForPosition(Offset(min(x1, x2), y)).coerceIn(lineStart, lineEnd)
    var e = layout.getOffsetForPosition(Offset(max(x1, x2), y)).coerceIn(lineStart, lineEnd)
    if (s == e) {
        // A short swipe inside one word: take the word on either side of the caret position.
        when {
            s < lineEnd && !text[s].isWhitespace() -> e = s + 1
            s > lineStart && !text[s - 1].isWhitespace() -> s--
            else -> return null
        }
    } else {
        while (s < e && text[s].isWhitespace()) s++
        while (e > s && text[e - 1].isWhitespace()) e--
        if (s >= e) return null
    }
    while (s > lineStart && !text[s - 1].isWhitespace()) s--
    while (e < lineEnd && !text[e].isWhitespace()) e++

    val paraStart = text.lastIndexOf('\n', s - 1) + 1
    val paraEnd = text.indexOf('\n', e).let { if (it < 0) text.length else it }
    if ((text.substring(paraStart, s) + text.substring(e, paraEnd)).isBlank()) {
        val para = TextRange(paraStart, paraEnd)
        return when {
            paraEnd < text.length -> Erase(TextRange(s, e), TextRange(paraStart, paraEnd + 1), para)
            paraStart > 0 -> Erase(TextRange(s, e), TextRange(paraStart - 1, paraEnd), para)
            else -> Erase(TextRange(s, e), para, null) // the only line: it just empties
        }
    }
    // The space after the words goes with them; at the end of a line, the space before them.
    var rs = s
    var re = e
    if (re < paraEnd && text[re].isWhitespace()) {
        while (re < paraEnd && text[re].isWhitespace()) re++
    } else {
        while (rs > paraStart && text[rs - 1].isWhitespace()) rs--
    }
    return Erase(TextRange(s, e), TextRange(rs, re), null)
}

/** The eraser's state while a swipe and its animation run; [line] is -1 when idle. */
@Stable
class Eraser {
    var line by mutableIntStateOf(-1)

    /** The scratch runs from [from] to [to], in the text layout's x. */
    var from by mutableFloatStateOf(0f)
    var to by mutableFloatStateOf(0f)
    var target by mutableStateOf<Erase?>(null)

    /** The scratched words' ink and the scratch itself fade together, 1 to 0. */
    var ink by mutableFloatStateOf(1f)

    /** How far the gap has closed, 0 to 1. */
    var collapse by mutableFloatStateOf(0f)

    val busy get() = line >= 0

    fun reset() {
        line = -1
        target = null
        ink = 1f
        collapse = 0f
    }
}

/**
 * Fades the scratched words, then narrows them (and the space that goes with them) to nothing,
 * so the rest of the line closes up and reflows. The text itself is untouched until the end.
 */
fun eraseTransformation(e: Erase, ink: Color, alpha: Float, collapse: Float) = VisualTransformation { text ->
    val b = AnnotatedString.Builder(text)
    b.addStyle(SpanStyle(color = ink.copy(alpha = alpha)), e.words.start, e.words.end)
    if (e.wholeLine == null && collapse > 0f) {
        b.addStyle(
            SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX = max(0.01f, 1f - collapse))),
            e.remove.start, e.remove.end,
        )
    }
    TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
}

/**
 * The scratch: a quick back-and-forth pen stroke over the middle of the letters, from [from]
 * toward [to], drawn at [left] (where the layout's x = 0 sits in this draw scope).
 */
fun DrawScope.drawScratch(
    from: Float, to: Float, left: Float, baseline: Float, textPx: Float, color: Color,
) {
    if (abs(to - from) < 1f) return
    val dir = if (to > from) 1f else -1f
    val mid = baseline - textPx * 0.3f
    val amp = textPx * 0.26f
    val step = textPx * 0.22f
    val path = Path()
    path.moveTo(left + from, mid + amp * 0.4f)
    var x = from
    var up = true
    while ((to - x) * dir > step) {
        x += step * dir
        path.lineTo(left + x, mid + if (up) -amp else amp)
        up = !up
    }
    path.lineTo(left + to, mid)
    drawPath(
        path, color,
        style = Stroke(width = textPx * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/**
 * A swipe across a line of text. Watches every touch before the text field and the scroll do
 * (the Initial pass) and takes it over only when it starts on a line with words and is clearly
 * sideways and quick: an up or down drag stays a scroll, a long press stays a text selection.
 */
suspend fun PointerInputScope.scratchGesture(
    lineAt: (Offset) -> Int,
    onStart: (line: Int, x0: Float, x: Float) -> Unit,
    onMove: (x: Float) -> Unit,
    onEnd: (cancelled: Boolean) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val line = lineAt(down.position)
    if (line < 0) return@awaitEachGesture
    val slop = viewConfiguration.touchSlop
    val longPress = viewConfiguration.longPressTimeoutMillis
    var travel = Offset.Zero
    var scratching = false
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change == null || event.changes.size > 1) {
            if (scratching) onEnd(true) // a second finger: leave the words alone
            break
        }
        if (!change.pressed) {
            if (scratching) {
                change.consume()
                onEnd(false)
            }
            break
        }
        travel += change.position - change.previousPosition
        if (!scratching) {
            val ax = abs(travel.x)
            val ay = abs(travel.y)
            if (ay > slop && ay >= ax) break
            if (change.uptimeMillis - down.uptimeMillis > longPress) break
            if (ax > slop * 2 && ax > ay * 2) {
                scratching = true
                onStart(line, down.position.x, change.position.x)
            }
        }
        if (scratching) {
            change.consume()
            onMove(change.position.x)
        }
    }
}
