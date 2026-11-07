package io.github.whatwhatwhoa.scan

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** A page's look in the PDF. [key] is what the saved pages file stores. */
enum class Filter(val key: String, val label: String, val desc: String) {
    Original("original", "Original", "Exactly as photographed, just cropped and straightened."),
    Photo("enhance", "Photo", "Auto contrast and slightly richer color. Best for photos, logos and colored paper."),
    Document("document", "Document", "Removes shadows and whitens the paper while keeping ink color. Best for most receipts."),
    Gray("gray", "Grayscale", "Same cleanup as Document, in grayscale. Smaller files."),
    BW("bw", "B&W", "Pure black and white. Crispest text and smallest files, but faint print can drop out.");

    companion object {
        fun of(key: String?): Filter = entries.firstOrNull { it.key == key } ?: Document
    }
}

/**
 * The filters on ARGB pixels, in place, ported from the web Scan Stack. Document divides each
 * pixel by the paper's own brightness around it (the background), which takes out shadows and
 * uneven light; the others build on it or stand alone.
 */
object Filters {
    fun apply(f: Filter, px: IntArray, w: Int, h: Int) {
        when (f) {
            Filter.Original -> Unit
            Filter.Photo -> enhance(px)
            Filter.Document -> document(px, w, h)
            Filter.Gray -> { document(px, w, h); gray(px) }
            Filter.BW -> bw(px, w, h)
        }
    }

    private fun clamp8(v: Double): Int = v.roundToInt().coerceIn(0, 255)

    private class Background(val g: Array<FloatArray>, val gw: Int, val gh: Int, val b: Int)

    /** The paper's brightness per channel on a coarse grid: block maximum, then spread and smoothed. */
    private fun background(px: IntArray, w: Int, h: Int): Background {
        val b = max(8, (min(w, h) / 36f).roundToInt())
        val gw = (w + b - 1) / b; val gh = (h + b - 1) / b; val n = gw * gh
        var g = Array(3) { FloatArray(n) }
        for (y in 0 until h) {
            val gy = (y / b) * gw
            for (x in 0 until w) {
                val gi = gy + x / b
                val c = px[y * w + x]
                val r = ((c shr 16) and 255).toFloat(); val gr = ((c shr 8) and 255).toFloat(); val bl = (c and 255).toFloat()
                if (r > g[0][gi]) g[0][gi] = r
                if (gr > g[1][gi]) g[1][gi] = gr
                if (bl > g[2][gi]) g[2][gi] = bl
            }
        }
        fun pass(src: FloatArray, maxMode: Boolean): FloatArray {
            val dst = FloatArray(n)
            for (y in 0 until gh) for (x in 0 until gw) {
                var acc = 0f; var cnt = 0
                for (yy in max(0, y - 1)..min(gh - 1, y + 1)) for (xx in max(0, x - 1)..min(gw - 1, x + 1)) {
                    val v = src[yy * gw + xx]
                    if (maxMode) { if (v > acc) acc = v } else { acc += v; cnt++ }
                }
                dst[y * gw + x] = if (maxMode) acc else acc / cnt
            }
            return dst
        }
        g = Array(3) { pass(pass(pass(g[it], true), false), false) }
        return Background(g, gw, gh, b)
    }

    /** Divide by the background, then a gentle curve that keeps ink dark and turns paper white. */
    fun document(px: IntArray, w: Int, h: Int) {
        val bg = background(px, w, h)
        val g = bg.g; val gw = bg.gw; val gh = bg.gh; val b = bg.b
        val lut = IntArray(1025) { clamp8(255 * min(1.0, it / 1024.0 * 1.06).pow(2.1)) }
        val gxA = IntArray(w); val fxA = FloatArray(w)
        for (x in 0 until w) {
            val gx = ((x + 0.5f) / b - 0.5f).coerceIn(0f, gw - 1.001f)
            gxA[x] = gx.toInt(); fxA[x] = gx - gx.toInt()
        }
        val ch = IntArray(3)
        for (y in 0 until h) {
            val gy = ((y + 0.5f) / b - 0.5f).coerceIn(0f, gh - 1.001f)
            val y0 = gy.toInt(); val fy = gy - y0
            val r0 = y0 * gw; val r1 = min(gh - 1, y0 + 1) * gw
            for (x in 0 until w) {
                val x0 = gxA[x]; val x1 = min(gw - 1, x0 + 1); val fx = fxA[x]
                val i = y * w + x
                val c = px[i]
                ch[0] = (c shr 16) and 255; ch[1] = (c shr 8) and 255; ch[2] = c and 255
                for (k in 0 until 3) {
                    val gk = g[k]
                    val t = gk[r0 + x0] + (gk[r0 + x1] - gk[r0 + x0]) * fx
                    val bt = gk[r1 + x0] + (gk[r1 + x1] - gk[r1 + x0]) * fx
                    val bgv = max(24f, t + (bt - t) * fy)
                    ch[k] = lut[min(1024, (ch[k] / bgv * 1024).toInt())]
                }
                px[i] = (c and (0xFF shl 24)) or (ch[0] shl 16) or (ch[1] shl 8) or ch[2]
            }
        }
    }

    fun gray(px: IntArray) {
        for (i in px.indices) {
            val c = px[i]
            val v = (((c shr 16) and 255) * 77 + ((c shr 8) and 255) * 150 + (c and 255) * 29) shr 8
            px[i] = (c and (0xFF shl 24)) or (v shl 16) or (v shl 8) or v
        }
    }

    fun bw(px: IntArray, w: Int, h: Int) {
        document(px, w, h)
        gray(px)
        for (i in px.indices) {
            val c = px[i]
            val v = if (((c shr 16) and 255) > 150) 255 else 0
            px[i] = (c and (0xFF shl 24)) or (v shl 16) or (v shl 8) or v
        }
    }

    /** Stretch the darkest 1 % and brightest 0.5 % to the ends, and 15 % more color. */
    fun enhance(px: IntArray) {
        val n = px.size
        val hist = IntArray(256)
        for (c in px) hist[(((c shr 16) and 255) * 77 + ((c shr 8) and 255) * 150 + (c and 255) * 29) shr 8]++
        var lo = 0; var acc = 0L
        while (lo < 255) { acc += hist[lo]; if (acc < n * 0.01) lo++ else break }
        var hi = 255; acc = 0
        while (hi > 0) { acc += hist[hi]; if (acc < n * 0.005) hi-- else break }
        val k = 255.0 / max(20, hi - lo)
        for (i in px.indices) {
            val c = px[i]
            val r = (((c shr 16) and 255) - lo) * k
            val g = (((c shr 8) and 255) - lo) * k
            val b = ((c and 255) - lo) * k
            val l = 0.3 * r + 0.59 * g + 0.11 * b
            px[i] = (c and (0xFF shl 24)) or (clamp8(l + (r - l) * 1.15) shl 16) or
                (clamp8(l + (g - l) * 1.15) shl 8) or clamp8(l + (b - l) * 1.15)
        }
    }
}
