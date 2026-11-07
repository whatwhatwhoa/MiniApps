package io.github.whatwhatwhoa.scan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.whatwhatwhoa.scan.Mode
import io.github.whatwhatwhoa.scan.Page
import io.github.whatwhatwhoa.scan.Pt
import io.github.whatwhatwhoa.scan.ScanModel
import io.github.whatwhatwhoa.stack.Mat
import io.github.whatwhatwhoa.stack.MatInk
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The picture's size in its own pixels, with the stage's padding and how much must stay in view. */
private class Geom(val iw: Float, val ih: Float, val pad: Float, val keep: Float)

/** Where the picture lies on the stage: [sc] screen px per picture px, its top-left at ([ox], [oy]). */
private class View(val sc: Float, val ox: Float, val oy: Float, val base: Float, val rawOx: Float, val rawOy: Float) {
    fun screen(p: Pt) = Offset(ox + p.x * sc, oy + p.y * sc)
    fun image(o: Offset) = Pt((o.x - ox) / sc, (o.y - oy) / sc)
}

/**
 * Zoom and pan of the stage (fit × zoom, plus pan, as the web tool), and the corner being dragged.
 * Made anew per page and view; the mat's size comes from [box], which outlives it.
 */
private class StageState(private val box: State<IntSize>) {
    var zoom by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)
    val size: IntSize get() = box.value
    var corner by mutableIntStateOf(-1)
    var finger by mutableStateOf<Offset?>(null)

    fun view(g: Geom): View {
        val w = size.width.toFloat(); val h = size.height.toFloat()
        if (w <= 0f || h <= 0f) return View(1f, 0f, 0f, 1f, 0f, 0f)
        val base = max(1e-4f, min((w - 2 * g.pad) / g.iw, (h - 2 * g.pad) / g.ih))
        val sc = base * zoom
        val rx = (w - g.iw * sc) / 2 + pan.x; val ry = (h - g.ih * sc) / 2 + pan.y
        // Never let the picture slide fully off the stage.
        val ox = min(max(rx, g.keep - g.iw * sc), w - g.keep)
        val oy = min(max(ry, g.keep - g.ih * sc), h - g.keep)
        return View(sc, ox, oy, base, rx, ry)
    }

    /** Takes the pan back to where [view] keeps the picture. */
    fun settle(g: Geom) {
        val v = view(g)
        pan += Offset(v.ox - v.rawOx, v.oy - v.rawOy)
    }

    /** Zooms by [factor] about the screen point [m], so the picture under it stays put. */
    fun zoomAt(m: Offset, factor: Float, g: Geom) {
        val v = view(g)
        val z = (zoom * factor).coerceIn(0.5f, 16f)
        if (z == zoom) return
        val ix = (m.x - v.ox) / v.sc; val iy = (m.y - v.oy) / v.sc
        val sc = v.base * z
        pan = Offset(m.x - ix * sc - (size.width - g.iw * sc) / 2, m.y - iy * sc - (size.height - g.ih * sc) / 2)
        zoom = z
        settle(g)
    }

    fun reset() { zoom = 1f; pan = Offset.Zero }
}

/**
 * The mat: in Crop the photo with the crop drawn over it (the outside dimmed, amber corners, a
 * magnifier while one is dragged); in Filter & review the processed page on white. Empty, it
 * says how to start.
 */
@Composable
fun Stage(model: ScanModel, modifier: Modifier, onTake: () -> Unit, onPick: () -> Unit) {
    val page = model.current
    val box = remember { mutableStateOf(IntSize.Zero) }
    Mat(modifier.onSizeChanged { box.value = it }) {
        if (page == null) {
            Empty(model, onTake, onPick)
            return@Mat
        }
        val crop = model.mode == Mode.Crop
        val st = remember(page.id, model.mode) { StageState(box) }
        val image = if (crop) model.source?.takeIf { it.id == page.id }?.bitmap
        // While a new filter or turn is on its way, the last result stays up under "Processing…".
        else model.result?.takeIf { it.id == page.id }?.bitmap
        if (image != null) {
            val density = LocalDensity.current
            val iw = if (crop) page.w.toFloat() else image.width.toFloat()
            val ih = if (crop) page.h.toFloat() else image.height.toFloat()
            val g = with(density) { Geom(iw, ih, (if (crop) 28.dp else 20.dp).toPx(), 60.dp.toPx()) }
            Picture(model, page, st, image, crop, g)
            Hint(model, page, crop, image)
            ZoomBar(st, g)
        }
        if (model.working || model.importing != null) {
            val text = model.importing ?: when {
                model.detecting -> "Finding the edges…"
                crop -> "Opening the photo…"
                else -> "Processing…"
            }
            MatTag(text, Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp))
        }
    }
}

@Composable
private fun MatTag(text: String, modifier: Modifier) {
    Text(
        text,
        modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        style = StackType.mono.copy(fontSize = 11.sp), color = MatInk.text,
    )
}

@Composable
private fun BoxScope.Empty(model: ScanModel, onTake: () -> Unit, onPick: () -> Unit) {
    Column(
        Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 420.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val importing = model.importing
        if (importing != null) {
            Text(importing, style = StackType.body, color = MatInk.text)
            return@Column
        }
        Text("Photograph a receipt or document", style = StackType.h2.copy(fontSize = 17.sp), color = MatInk.strong,
            textAlign = TextAlign.Center)
        Text("Add as many pages as you like. They stay on this device.", style = StackType.note, color = MatInk.text,
            textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StackButton("Take photo", onTake, primary = true)
            StackButton("Choose photos", onPick)
        }
        Text("You can also share photos to Scan from the gallery.", style = StackType.sub,
            color = MatInk.text.copy(alpha = 0.75f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun BoxScope.Hint(model: ScanModel, page: Page, crop: Boolean, image: ImageBitmap) {
    val text = when {
        !crop -> "${page.filter.label} · ${image.width} × ${image.height} px"
        model.snap -> "Tap to move the nearest corner there · pinch to zoom"
        else -> "Drag the corners onto the paper's edges · pinch to zoom"
    }
    Text(
        text,
        Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp, end = 150.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        style = StackType.mono.copy(fontSize = 11.sp), color = MatInk.text,
    )
}

@Composable
private fun BoxScope.ZoomBar(st: StageState, g: Geom) {
    val mid = Offset(st.size.width / 2f, st.size.height / 2f)
    Row(
        Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 10.dp).clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ZoomKey("−", "Zoom out") { st.zoomAt(mid, 1 / 1.4f, g) }
        ZoomKey("${(st.zoom * 100).roundToInt()}%", "Fit to the mat") { st.reset() }
        ZoomKey("+", "Zoom in") { st.zoomAt(mid, 1.4f, g) }
    }
}

@Composable
private fun ZoomKey(label: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .heightIn(min = 34.dp).widthIn(min = 40.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = StackType.mono.copy(fontSize = 13.sp), color = MatInk.text)
    }
}

@Composable
private fun Picture(model: ScanModel, page: Page, st: StageState, image: ImageBitmap, crop: Boolean, g: Geom) {
    val c = Stack.colors
    val snap = model.snap
    Canvas(
        Modifier.fillMaxSize()
            .pointerInput(page.id, crop, snap, g.iw, g.ih) { gestures(model, page, st, g, crop, snap) }
    ) {
        val v = st.view(g)
        val w = g.iw * v.sc; val h = g.ih * v.sc
        val dstOff = IntOffset(v.ox.roundToInt(), v.oy.roundToInt())
        val dstSize = IntSize(max(1, w.roundToInt()), max(1, h.roundToInt()))
        if (!crop) {
            // The page on white paper, with a soft drop below it.
            for (k in 1..3) {
                val s = k * 2.dp.toPx()
                drawRect(Color.Black.copy(alpha = 0.12f), Offset(v.ox - s / 2, v.oy + s), Size(w + s, h + s / 2))
            }
            drawRect(Color.White, Offset(v.ox, v.oy), Size(w, h))
            drawImage(image, IntOffset.Zero, IntSize(image.width, image.height), dstOff, dstSize, filterQuality = FilterQuality.High)
            return@Canvas
        }
        drawImage(image, IntOffset.Zero, IntSize(image.width, image.height), dstOff, dstSize, filterQuality = FilterQuality.High)
        val pts = page.quad.map(v::screen)
        val outline = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until 4) lineTo(pts[i].x, pts[i].y)
            close()
        }
        val dim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(v.ox, v.oy, v.ox + w, v.oy + h))
            addPath(outline)
        }
        drawPath(dim, Color.Black.copy(alpha = 0.55f))
        drawPath(outline, c.handle, style = Stroke(2.dp.toPx()))
        val k = st.corner
        if (snap && k >= 0) {
            // Crosshair snap: guides across the mat through the corner being placed.
            val p = pts[k]
            val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
            val line = Color.White.copy(alpha = 0.75f)
            drawLine(line, Offset(0f, p.y), Offset(size.width, p.y), 1.dp.toPx(), pathEffect = dash)
            drawLine(line, Offset(p.x, 0f), Offset(p.x, size.height), 1.dp.toPx(), pathEffect = dash)
        }
        pts.forEachIndexed { i, p ->
            val r = (if (i == k) 14.dp else 11.dp).toPx()
            drawCircle(MatInk.strong.copy(alpha = 0.95f), r, p)
            drawCircle(c.handle, r, p, style = Stroke(3.dp.toPx()))
        }
        if (k >= 0) loupe(image, pts[k], v, st.finger, c.handle, g)
    }
}

/**
 * The magnifier, pinned top-right: the photo under the corner being dragged, which the finger
 * hides. It fades when the finger or the corner comes under it.
 */
private fun DrawScope.loupe(img: ImageBitmap, at: Offset, v: View, finger: Offset?, handle: Color, g: Geom) {
    val r = 66.dp.toPx()
    val margin = 14.dp.toPx()
    val center = Offset(size.width - r - margin, r + margin)
    val near = r + 18.dp.toPx()
    val under = (at - center).getDistance() < near || (finger != null && (finger - center).getDistance() < near)
    val alpha = if (under) 0.18f else 1f
    // Screen px per photo px: three times the view, capped at 2.5 dp per photo pixel.
    val ls = max(v.sc, min(v.sc * 3f, 2.5f * density))
    val sx = (at.x - v.ox) / v.sc; val sy = (at.y - v.oy) / v.sc
    val span = r / ls
    val kx = img.width / g.iw; val ky = img.height / g.ih
    // The source in whole bitmap pixels, inside the bitmap; the target follows from them exactly.
    val l = floor(max(0f, (sx - span) * kx)).toInt()
    val t = floor(max(0f, (sy - span) * ky)).toInt()
    val rr = ceil(min(img.width.toFloat(), (sx + span) * kx)).toInt()
    val b = ceil(min(img.height.toFloat(), (sy + span) * ky)).toInt()
    drawCircle(Color.Black, r, center, alpha)
    if (rr > l && b > t) {
        val dl = center.x + (l / kx - sx) * ls; val dt = center.y + (t / ky - sy) * ls
        val dw = (rr - l) / kx * ls; val dh = (b - t) / ky * ls
        clipPath(Path().apply { addOval(Rect(center, r)) }) {
            drawImage(img, IntOffset(l, t), IntSize(rr - l, b - t), IntOffset(dl.roundToInt(), dt.roundToInt()),
                IntSize(max(1, dw.roundToInt()), max(1, dh.roundToInt())), alpha = alpha, filterQuality = FilterQuality.High)
        }
    }
    drawCircle(handle, r, center, alpha, style = Stroke(3.dp.toPx()))
    val a = 12.dp.toPx(); val sw = 1.5.dp.toPx()
    drawLine(handle, center - Offset(a, 0f), center + Offset(a, 0f), sw, alpha = alpha)
    drawLine(handle, center - Offset(0f, a), center + Offset(0f, a), sw, alpha = alpha)
}

/**
 * One finger: in Crop, grab a corner near it (keeping the offset, so it doesn't jump), or with
 * crosshair snap move the nearest corner to it; otherwise pan. Two fingers: pinch to zoom and
 * move, and a corner being dragged goes back to where it was.
 */
private suspend fun PointerInputScope.gestures(model: ScanModel, page: Page, st: StageState, g: Geom, crop: Boolean, snap: Boolean) {
    val grabRadius = 40.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown()
        val v0 = st.view(g)
        var corner = -1
        var grab = Offset.Zero
        var orig: Pt? = null
        var panning = false
        if (crop) {
            val pts = page.quad.map(v0::screen)
            var best = grabRadius
            pts.forEachIndexed { i, p ->
                val d = (p - down.position).getDistance()
                if (d < best) { best = d; corner = i }
            }
            if (corner >= 0) {
                grab = pts[corner] - down.position
                orig = page.quad[corner]
            } else if (snap) {
                corner = pts.indices.minBy { (pts[it] - down.position).getDistance() }
                orig = page.quad[corner]
                model.moveCorner(page, corner, v0.image(down.position))
            } else panning = true
        } else panning = true
        if (corner >= 0) { st.corner = corner; st.finger = down.position }
        down.consume()

        var last = down.position
        var pinched = false
        var lastMid: Offset? = null
        var lastDist = 0f
        var lastIds: List<PointerId> = emptyList()
        while (true) {
            val ev = awaitPointerEvent()
            val pressed = ev.changes.filter { it.pressed }
            if (pressed.isEmpty()) break
            if (pressed.size >= 2) {
                if (corner >= 0) {
                    orig?.let { model.moveCorner(page, corner, it) }
                    corner = -1; st.corner = -1; st.finger = null
                }
                pinched = true
                val a = pressed[0].position; val b = pressed[1].position
                val mid = (a + b) / 2f
                val dist = (a - b).getDistance()
                val ids = listOf(pressed[0].id, pressed[1].id)
                val lm = lastMid
                if (lm != null && ids == lastIds && lastDist > 0f) {
                    st.pan += mid - lm
                    st.zoomAt(mid, dist / lastDist, g)
                    st.settle(g)
                }
                lastMid = mid; lastDist = dist; lastIds = ids
                ev.changes.forEach { it.consume() }
                continue
            }
            lastMid = null
            val ch = pressed[0]
            if (!pinched) {
                if (corner >= 0) {
                    model.moveCorner(page, corner, st.view(g).image(ch.position + grab))
                    st.finger = ch.position
                } else if (panning) {
                    st.pan += ch.position - last
                    st.settle(g)
                }
            }
            last = ch.position
            ch.consume()
        }
        if (corner >= 0) model.commitQuad()
        st.corner = -1
        st.finger = null
    }
}
