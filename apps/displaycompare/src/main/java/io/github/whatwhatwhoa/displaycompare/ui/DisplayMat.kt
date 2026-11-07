package io.github.whatwhatwhoa.displaycompare.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.whatwhatwhoa.displaycompare.Align
import io.github.whatwhatwhoa.displaycompare.Display
import io.github.whatwhatwhoa.displaycompare.Fmt
import io.github.whatwhatwhoa.displaycompare.Units
import io.github.whatwhatwhoa.stack.Mat
import io.github.whatwhatwhoa.stack.MatInk
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/** Grid steps a reader can count in: the smallest that leaves at most ~10 squares across. */
private fun gridStepIn(maxWidthIn: Double, units: Units): Pair<Double, String> {
    return if (units == Units.Inches) {
        val step = listOf(1.0, 2.0, 5.0, 10.0, 20.0, 50.0).first { maxWidthIn / it <= 10 || it == 50.0 }
        step to "${Fmt.trim(step)} in"
    } else {
        val cm = listOf(2.0, 5.0, 10.0, 20.0, 50.0, 100.0).first { maxWidthIn * 2.54 / it <= 10 || it == 100.0 }
        cm / 2.54 to "${Fmt.trim(cm)} cm"
    }
}

/**
 * The displays to scale on the cutting mat, the biggest drawn first so smaller ones sit on top.
 * The grid is in real units from the displays' common bottom-left corner, so its squares can be
 * counted. The mat is as tall as the displays need, up to [maxHeight].
 */
@Composable
fun DisplayMat(
    displays: List<Display>,
    units: Units,
    align: Align,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val c = Stack.colors
    val measurer = rememberTextMeasurer()
    val label = StackType.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val gridLabel = StackType.mono.copy(fontSize = 11.sp, color = MatInk.text.copy(alpha = 0.8f))
    val description = displays.joinToString("; ", prefix = "Displays to scale: ") {
        "${it.letter} ${it.title}, ${Fmt.len(it.widthIn, units)} by ${Fmt.len(it.heightIn, units)} ${units.len}"
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val pad = 16.dp
        val bottom = 28.dp // room for the grid's label under the displays
        val maxW = displays.maxOf { it.widthIn }
        val maxH = displays.maxOf { it.heightIn }
        val dpPerIn = min((maxWidth - pad * 2).value / maxW, (maxHeight - pad - bottom).value / maxH)
        val height = (maxH * dpPerIn).dp + pad + bottom

        Mat(Modifier.fillMaxWidth().height(height), grid = null) {
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
                val s = (dpPerIn * density).toFloat() // px per inch
                val boxW = (maxW * s).toFloat()
                val boxH = (maxH * s).toFloat()
                val ox = (size.width - boxW) / 2
                val oy = pad.toPx()
                val base = oy + boxH

                val (stepIn, stepLabel) = gridStepIn(maxW, units)
                val step = (stepIn * s).toFloat()
                var k = floor(-ox / step)
                while (ox + k * step <= size.width) {
                    val x = ox + k * step
                    if (x >= 0) drawRect(c.matLine, Offset(x, 0f), Size(1f, size.height))
                    k++
                }
                k = -ceil((size.height - base) / step)
                while (base - k * step >= 0) {
                    val y = base - k * step
                    if (y <= size.height) drawRect(c.matLine, Offset(0f, y), Size(size.width, 1f))
                    k++
                }

                fun rectOf(d: Display): Rect {
                    val w = (d.widthIn * s).toFloat()
                    val h = (d.heightIn * s).toFloat()
                    val left = when (align) {
                        Align.Corner -> ox
                        else -> ox + (boxW - w) / 2
                    }
                    val top = when (align) {
                        Align.Center -> oy + (boxH - h) / 2
                        else -> base - h
                    }
                    return Rect(left, top, left + w, top + h)
                }

                val sw = 2.dp.toPx()
                displays.sortedByDescending { it.areaIn2 }.forEach { d ->
                    val r = rectOf(d)
                    val color = c.series[d.slot % c.series.size]
                    drawRect(color.copy(alpha = 0.12f), r.topLeft, r.size)
                    drawRect(color, Offset(r.left + sw / 2, r.top + sw / 2), Size(r.width - sw, r.height - sw),
                        style = Stroke(sw))
                }

                // Labels in letter order, each moved down past any label already placed.
                val placed = mutableListOf<Rect>()
                val gap = 2.dp.toPx()
                displays.sortedBy { it.slot }.forEach { d ->
                    val r = rectOf(d)
                    val color = c.series[d.slot % c.series.size]
                    val text = measurer.measure("${d.letter} ${Fmt.diag(d.diagonal)}", label.copy(color = color))
                    val tw = text.size.width.toFloat()
                    val th = text.size.height.toFloat()
                    var at = Rect(Offset(r.left + 6.dp.toPx(), r.top + 4.dp.toPx()), Size(tw + 6.dp.toPx(), th))
                    if (r.height < th + 8.dp.toPx()) at = at.translate(0f, -(th + 6.dp.toPx()))
                    while (placed.any { it.overlaps(at) }) at = at.translate(0f, th + gap)
                    placed += at
                    drawRect(c.mat.copy(alpha = 0.7f), at.topLeft, at.size)
                    drawText(text, topLeft = Offset(at.left + 3.dp.toPx(), at.top))
                }

                val g = measurer.measure("Grid $stepLabel", gridLabel)
                drawText(g, topLeft = Offset(size.width - g.size.width - 10.dp.toPx(),
                    size.height - g.size.height - 7.dp.toPx()))
            }
        }
    }
}
