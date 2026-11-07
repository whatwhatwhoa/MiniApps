package io.github.whatwhatwhoa.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import java.io.File
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** What a processed page depends on: its crop, its filter and its quarter turns (0–3). */
data class Look(val quad: Quad, val filter: Filter, val rot: Int)

/** Bitmaps in and out: opening photos, straightening and filtering a page, thumbnails. */
object Imaging {
    /** A photo is kept at most this many pixels on its long side (as the web Scan Stack). */
    const val MAX_SRC = 2400
    /** A straightened page is at most this many pixels on its long side. */
    const val MAX_OUT = 2200
    /** Thumbnails in the page list, long side. */
    const val THUMB = 240

    /** Opens a photo upright (its EXIF turn applied), at most [MAX_SRC] on its long side, in sRGB. */
    fun decode(source: ImageDecoder.Source): Bitmap {
        val b = ImageDecoder.decodeBitmap(source) { dec, info, _ ->
            val w = info.size.width; val h = info.size.height
            val s = min(1.0, MAX_SRC.toDouble() / max(w, h))
            if (s < 1) dec.setTargetSize(max(1, (w * s).roundToInt()), max(1, (h * s).roundToInt()))
            dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            dec.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
        }
        if (b.config == Bitmap.Config.ARGB_8888) return b
        return b.copy(Bitmap.Config.ARGB_8888, false).also { b.recycle() }
    }

    fun pixels(b: Bitmap): IntArray = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }

    fun load(file: File): Bitmap? =
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })

    /** Opens [file] reduced by the largest power of two that keeps its long side at least [minSide]. */
    fun loadSmall(file: File, longSide: Int, minSide: Int): Bitmap? {
        var s = 1
        while (longSide / (s * 2) >= minSide) s *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = s
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }

    /** Shrinks by halves (each a clean 2 × 2 average), then the last step, so a big shrink doesn't sparkle. */
    fun shrink(src: Bitmap, maxSide: Int): Bitmap {
        var b = src
        while (max(b.width, b.height) / 2 >= maxSide) {
            val n = b.scale(max(1, b.width / 2), max(1, b.height / 2))
            if (b !== src) b.recycle()
            b = n
        }
        val s = min(1f, maxSide.toFloat() / max(b.width, b.height))
        if (s >= 1f) return if (b === src) b.copy(Bitmap.Config.ARGB_8888, false) else b
        val n = b.scale(max(1, (b.width * s).roundToInt()), max(1, (b.height * s).roundToInt()))
        if (b !== src) b.recycle()
        return n
    }

    /** The page cut out of [src] along [look]'s quad, straightened, filtered and turned; at most [maxOut] long. */
    fun process(src: Bitmap, look: Look, maxOut: Int = MAX_OUT): Bitmap {
        val q = look.quad
        fun d(a: Pt, b: Pt) = hypot(a.x - b.x, a.y - b.y)
        val w0 = (d(q[0], q[1]) + d(q[3], q[2])) / 2
        val h0 = (d(q[0], q[3]) + d(q[1], q[2])) / 2
        val sc = min(1.25f, maxOut / max(1f, max(w0, h0)))
        val w = max(8, (w0 * sc).roundToInt()); val h = max(8, (h0 * sc).roundToInt())
        val out = createBitmap(w, h)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        // The perspective map from the four corners to the upright page; Skia samples it bilinearly.
        val m = Matrix()
        val from = floatArrayOf(q[0].x, q[0].y, q[1].x, q[1].y, q[2].x, q[2].y, q[3].x, q[3].y)
        val to = floatArrayOf(0f, 0f, w.toFloat(), 0f, w.toFloat(), h.toFloat(), 0f, h.toFloat())
        if (!m.setPolyToPoly(from, 0, to, 0, 4)) {
            // A twisted quad has no such map: fall back to its bounding box.
            val box = RectF(q.minOf { it.x }, q.minOf { it.y }, q.maxOf { it.x }, q.maxOf { it.y })
            m.setRectToRect(box, RectF(0f, 0f, w.toFloat(), h.toFloat()), Matrix.ScaleToFit.FILL)
        }
        canvas.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
        if (look.filter != Filter.Original) {
            val px = pixels(out)
            Filters.apply(look.filter, px, w, h)
            out.setPixels(px, 0, w, 0, 0, w, h)
        }
        if (look.rot == 0) return out
        val turned = Bitmap.createBitmap(out, 0, 0, w, h, Matrix().apply { postRotate(90f * look.rot) }, true)
        out.recycle()
        return turned
    }

    /** A thumbnail of the processed page, made from a small copy of the photo ([srcW] is the photo's full width). */
    fun thumb(small: Bitmap, srcW: Int, look: Look): Bitmap {
        val k = small.width.toFloat() / srcW
        val q = look.quad.map { Pt(it.x * k, it.y * k) }
        return process(small, look.copy(quad = q), THUMB)
    }
}
