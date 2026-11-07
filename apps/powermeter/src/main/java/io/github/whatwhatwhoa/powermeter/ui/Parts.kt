package io.github.whatwhatwhoa.powermeter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackCard
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.powermeter.Format
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A Stack card with an optional title and (i) tooltip. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    info: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    StackCard(modifier, title = title, info = info, content = content)
}

/** Label and value on one line, value right-aligned in tabular figures. */
@Composable
fun Line(label: String, value: String, valueColor: Color = Color.Unspecified) {
    val c = Stack.colors
    Row(Modifier.fillMaxWidth()) {
        Text(
            label, Modifier.weight(1f), style = StackType.body.copy(fontSize = 14.sp),
            color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            value, style = StackType.body.copy(fontSize = 14.sp).merge(Numbers),
            color = if (valueColor == Color.Unspecified) c.ink else valueColor, maxLines = 1
        )
    }
}

/**
 * A time chart. [signed] draws power: a zero line, above it in the charge color, below it in the
 * drain color, labels in watts. Otherwise a 0–100 % line. A gap longer than [gapMs] breaks the line.
 *
 * With [onCursor] the chart can be read: a tap or a drag sideways puts a cursor at that time (a
 * drag up or down still scrolls the page), drawn as a line, a dot on the sample nearest to it and
 * a bubble with [bubble]'s text. [cursor] is owned by the caller, so two charts can share one.
 */
@Composable
fun Chart(
    times: LongArray,
    values: FloatArray,
    from: Long,
    to: Long,
    signed: Boolean,
    gapMs: Long,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    cursor: Long? = null,
    onCursor: ((Long) -> Unit)? = null,
    bubble: (Int) -> String = { i -> if (signed) Format.signedWatts(values[i].toDouble()) else "${values[i].roundToInt()}%" },
) {
    val flow = LocalFlow.current
    // Colors from the Stack tokens: faint gridlines, the accent for a level line, 11 sp mono axes.
    val c = Stack.colors
    val grid = c.line
    val lineColor = c.accent
    val labelStyle = StackType.mono.copy(fontSize = 11.sp, color = c.muted)
    val bubbleStyle = StackType.mono.copy(fontSize = 11.5.sp, color = c.panel)
    val measurer = rememberTextMeasurer()
    // The gesture detectors outlive a recomposition; they read the latest range from here.
    val span by rememberUpdatedState(from to to)
    val move by rememberUpdatedState(onCursor)
    fun timeAt(x: Float, w: Int): Long {
        val (a, b) = span
        return a + ((x / w.coerceAtLeast(1)).coerceIn(0f, 1f) * (b - a)).toLong()
    }
    val touch = if (onCursor == null) Modifier else Modifier
        .pointerInput(Unit) { detectTapGestures { p -> move?.invoke(timeAt(p.x, size.width)) } }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { p -> move?.invoke(timeAt(p.x, size.width)) },
            ) { change, _ ->
                change.consume()
                move?.invoke(timeAt(change.position.x, size.width))
            }
        }

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height).then(touch)) {
            val w = size.width
            val h = size.height
            var lo = 0f
            var hi = if (signed) 0f else 100f
            if (signed) {
                for (v in values) { lo = min(lo, v); hi = max(hi, v) }
                // Leave room so a flat line isn't drawn on the frame; at least ±1 W of range.
                val pad = max((hi - lo) * 0.1f, 500f)
                if (hi > 0f || lo == 0f) hi += pad
                if (lo < 0f) lo -= pad
            }
            val range = (to - from).coerceAtLeast(1L).toFloat()
            fun x(t: Long) = (t - from) / range * w
            fun y(v: Float) = h - (v - lo) / (hi - lo) * h
            val zero = y(0f)

            // Frame lines: top, bottom, and zero for power.
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            drawLine(grid, Offset(0f, 0f), Offset(w, 0f), 1f, pathEffect = dash)
            drawLine(grid, Offset(0f, h), Offset(w, h), 1f, pathEffect = dash)
            if (signed) drawLine(grid, Offset(0f, zero), Offset(w, zero), 2f)
            else drawLine(grid, Offset(0f, y(50f)), Offset(w, y(50f)), 1f, pathEffect = dash)

            val line = Path()
            val fill = Path()
            val base = if (signed) zero else h
            var segStart = -1f
            var lastX = 0f
            for (i in times.indices) {
                val px = x(times[i])
                val py = y(values[i])
                val newSeg = i == 0 || times[i] - times[i - 1] > gapMs
                if (newSeg) {
                    if (segStart >= 0) { fill.lineTo(lastX, base); fill.close() }
                    line.moveTo(px, py)
                    fill.moveTo(px, base); fill.lineTo(px, py)
                    segStart = px
                } else {
                    line.lineTo(px, py)
                    fill.lineTo(px, py)
                }
                lastX = px
            }
            if (segStart >= 0) { fill.lineTo(lastX, base); fill.close() }

            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (signed) {
                clipRect(bottom = zero) {
                    drawPath(fill, flow.charge.copy(alpha = 0.18f))
                    drawPath(line, flow.charge, style = stroke)
                }
                clipRect(top = zero) {
                    drawPath(fill, flow.drain.copy(alpha = 0.18f))
                    drawPath(line, flow.drain, style = stroke)
                }
            } else {
                drawPath(fill, lineColor.copy(alpha = 0.15f))
                drawPath(line, lineColor, style = stroke)
            }

            val top = if (signed) Format.signedWatts(hi.toDouble()) else "100%"
            val bottom = if (signed) Format.signedWatts(lo.toDouble()) else "0%"
            val m = 4.dp.toPx()
            drawText(measurer, top, Offset(m, m), labelStyle)
            if (!signed || lo < 0f) {
                val tl = measurer.measure(bottom, labelStyle)
                drawText(tl, topLeft = Offset(m, h - tl.size.height - m))
            }

            // The cursor: a line at the nearest sample (or at the touch, where nothing was
            // logged), a dot on the curve, and the bubble along the top, kept inside the chart.
            if (cursor != null && cursor in from..to) {
                val i = nearest(times, cursor, gapMs)
                val cx = if (i >= 0) x(times[i]) else x(cursor)
                drawLine(c.ink.copy(alpha = 0.6f), Offset(cx, 0f), Offset(cx, h), 1.dp.toPx())
                if (i >= 0) {
                    val v = values[i]
                    val dot = when {
                        !signed -> lineColor
                        v >= 0f -> flow.charge
                        else -> flow.drain
                    }
                    drawCircle(c.panel, 5.dp.toPx(), Offset(cx, y(v)))
                    drawCircle(dot, 3.5.dp.toPx(), Offset(cx, y(v)))
                }
                val tl = measurer.measure(if (i >= 0) bubble(i) else "not logged", bubbleStyle)
                val padX = 7.dp.toPx()
                val padY = 3.dp.toPx()
                val bw = tl.size.width + 2 * padX
                val bh = tl.size.height + 2 * padY
                val bx = (cx - bw / 2).coerceIn(0f, max(0f, w - bw))
                drawRoundRect(c.ink, Offset(bx, m), Size(bw, bh), CornerRadius(6.dp.toPx()))
                drawText(tl, topLeft = Offset(bx + padX, m + padY))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(Format.dayClock(from), Modifier.weight(1f), style = labelStyle)
            Text("now", style = labelStyle)
        }
    }
}

/**
 * The sample nearest to [t], or -1 when the nearest is more than half of [gapMs] away: the meter
 * wasn't running there.
 */
fun nearest(times: LongArray, t: Long, gapMs: Long): Int {
    if (times.isEmpty()) return -1
    val found = times.binarySearch(t)
    val i = if (found >= 0) found else {
        val after = -found - 1
        when {
            after == 0 -> 0
            after >= times.size -> times.size - 1
            t - times[after - 1] <= times[after] - t -> after - 1
            else -> after
        }
    }
    return if (abs(times[i] - t) <= gapMs / 2) i else -1
}

/**
 * Under a readable chart: what the cursor reads, with Clear; or, with no cursor, how to read it.
 */
@Composable
fun Readout(text: String?, onClear: () -> Unit) {
    val c = Stack.colors
    if (text == null) {
        Text("Touch or drag along the chart to read any point.", style = StackType.note, color = c.muted)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, Modifier.weight(1f), style = StackType.body.copy(fontSize = 14.sp).merge(Numbers), color = c.ink)
        StackButton("Clear", onClick = onClear, small = true)
    }
}

/** min / average / max of a power series, for the line under a chart. */
fun spread(values: FloatArray): Triple<Double, Double, Double>? {
    if (values.isEmpty()) return null
    var lo = Float.MAX_VALUE; var hi = -Float.MAX_VALUE; var sum = 0.0
    for (v in values) { lo = min(lo, v); hi = max(hi, v); sum += v }
    return Triple(lo.toDouble(), sum / values.size, hi.toDouble())
}

fun flowColor(mw: Double, flow: Flow, neutral: Color): Color = when {
    abs(mw) < 1 -> neutral
    mw > 0 -> flow.charge
    else -> flow.drain
}
