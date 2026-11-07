package io.github.whatwhatwhoa.powermeter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.powermeter.Format
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                Text(
                    title, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/** Label and value on one line, value right-aligned. */
@Composable
fun Line(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            value, style = MaterialTheme.typography.bodyMedium.merge(Numbers),
            color = valueColor, maxLines = 1
        )
    }
}

/**
 * A time chart. [signed] draws power: a zero line, above it in the charge color, below it in the
 * drain color, labels in watts. Otherwise a 0–100 % line. A gap longer than [gapMs] breaks the line.
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
) {
    val flow = LocalFlow.current
    val grid = MaterialTheme.colorScheme.outlineVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val labelStyle = MaterialTheme.typography.labelSmall.merge(Numbers)
        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
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
            val span = (to - from).coerceAtLeast(1L).toFloat()
            fun x(t: Long) = (t - from) / span * w
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
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(Format.dayClock(from), Modifier.weight(1f), style = labelStyle)
            Text("now", style = labelStyle)
        }
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
