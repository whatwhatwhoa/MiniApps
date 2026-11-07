package io.github.whatwhatwhoa.scan

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A point in a photo's pixels. */
data class Pt(val x: Float, val y: Float)

/** A crop: the paper's four corners in the photo's pixels, top-left, top-right, bottom-right, bottom-left. */
typealias Quad = List<Pt>

/**
 * Finding the paper in a photo, ported from the web Scan Stack: region candidates from four maps
 * (brightness, "paper-ness", local contrast, flatness) at many thresholds, each reduced to a
 * quadrilateral and scored by its shape and by how much its sides sit on a real color step; the
 * best one's sides are then nudged onto the strongest step. Pure Kotlin on ARGB pixels, so the
 * tests run on the JVM.
 */
object Detect {
    /** Detection runs on a copy at most this many pixels on its long side. */
    const val WORK = 512

    private const val FE = 2.0
    private const val EE = 3.0

    /** The whole photo less a margin: what a crop starts as when no paper is found. */
    fun inset(w: Int, h: Int, f: Float = 0.04f): Quad {
        val dx = w * f
        val dy = h * f
        return listOf(Pt(dx, dy), Pt(w - dx, dy), Pt(w - dx, h - dy), Pt(dx, h - dy))
    }

    /** The paper's corners in [argb] (w × h), or [inset] when nothing paper-like is found. */
    fun detect(argb: IntArray, w: Int, h: Int): Quad {
        val s = min(1.0, WORK.toDouble() / max(w, h))
        val sw = max(2, (w * s).roundToInt())
        val sh = max(2, (h * s).roundToInt())
        val small = if (sw == w && sh == h) argb else downscale(argb, w, h, sw, sh)
        val q = try { detectQuad(small, sw, sh) } catch (e: RuntimeException) { null } ?: return inset(w, h)
        val kx = w.toFloat() / sw
        val ky = h.toFloat() / sh
        return q.map { Pt((it.x * kx).coerceIn(0f, w.toFloat()), (it.y * ky).coerceIn(0f, h.toFloat())) }
    }

    /** Area-averaged shrink, so the small copy has no aliasing for the thresholds to trip on. */
    fun downscale(src: IntArray, w: Int, h: Int, nw: Int, nh: Int): IntArray {
        val out = IntArray(nw * nh)
        for (oy in 0 until nh) {
            val y0 = (oy.toLong() * h / nh).toInt()
            val y1 = max(y0 + 1, ((oy + 1).toLong() * h / nh).toInt())
            for (ox in 0 until nw) {
                val x0 = (ox.toLong() * w / nw).toInt()
                val x1 = max(x0 + 1, ((ox + 1).toLong() * w / nw).toInt())
                var r = 0L; var g = 0L; var b = 0L
                for (y in y0 until y1) {
                    var i = y * w + x0
                    for (x in x0 until x1) {
                        val c = src[i++]
                        r += (c shr 16) and 255; g += (c shr 8) and 255; b += c and 255
                    }
                }
                val n = (y1 - y0).toLong() * (x1 - x0)
                out[oy * nw + ox] = (0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
            }
        }
        return out
    }

    private class Img(val w: Int, val h: Int, val r: FloatArray, val g: FloatArray, val b: FloatArray)

    private class Cand(var quad: Quad, var score: Double, val frac: Double)

    /** The detection itself, in [px]'s own coordinates; null when no candidate region is found. */
    internal fun detectQuad(px: IntArray, w: Int, h: Int): Quad? {
        val n = w * h
        val lum = FloatArray(n)
        val paper = FloatArray(n)
        val rr = FloatArray(n); val gg = FloatArray(n); val bb = FloatArray(n)
        for (i in 0 until n) {
            val c = px[i]
            val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
            val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
            lum[i] = (0.299 * r + 0.587 * g + 0.114 * b).toFloat()
            paper[i] = lum[i] - (mx - mn)  // bright and colorless
            rr[i] = r.toFloat(); gg[i] = g.toFloat(); bb[i] = b.toFloat()
        }
        val img = Img(w, h, boxBlur(rr, w, h, 1), boxBlur(gg, w, h, 1), boxBlur(bb, w, h, 1))
        // Flatness: minus the color gradient. Paper interiors are flat regions enclosed by edges.
        val grad = FloatArray(n)
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            var m = 0.0
            for (ch in arrayOf(img.r, img.g, img.b)) {
                val gx = ch[i + 1] - ch[i - 1]; val gy = ch[i + w] - ch[i - w]
                m += gx * gx + gy * gy
            }
            grad[i] = -sqrt(m).toFloat()
        }
        for (x in 0 until w) { grad[x] = -999f; grad[(h - 1) * w + x] = -999f }
        for (y in 0 until h) { grad[y * w] = -999f; grad[y * w + w - 1] = -999f }

        val maps = listOf(lum, paper, localContrast(lum, w, h), grad)
        val cands = ArrayList<Cand>()
        val buf = Buffers(n)
        for ((mi, m) in maps.withIndex()) {
            val ths = LinkedHashSet<Float>()
            if (mi < 3) {
                val sorted = m.copyOf().also { it.sort() }
                ths.add(otsu(m))
                for (q in doubleArrayOf(0.5, 0.6, 0.7, 0.78, 0.85, 0.9, 0.94, 0.97)) ths.add(sorted[floor(q * (n - 1)).toInt()])
            } else {
                for (g in intArrayOf(6, 9, 13, 18, 25)) ths.add(-g.toFloat())
            }
            for (t in ths) componentsAbove(m, w, h, t, cands, buf)
        }
        if (cands.isEmpty()) return null
        for (c in cands) {
            val edge = edgeSupport(img, c.quad)
            c.score = c.score * edge.pow(EE) * (if (c.frac > 0.7) 0.3 else 1.0)
        }
        val best = cands.maxBy { it.score }
        return refineQuad(img, best.quad)
    }

    private fun sample(ch: FloatArray, w: Int, i: Int, fx: Float, fy: Float): Float {
        val t = ch[i] + (ch[i + 1] - ch[i]) * fx
        val b = ch[i + w] + (ch[i + w + 1] - ch[i + w]) * fx
        return t + (b - t) * fy
    }

    /** The color distance between two points, each sampled bilinearly. */
    private fun colorStep(img: Img, ax: Float, ay: Float, bx: Float, by: Float): Double {
        val w = img.w; val h = img.h
        val x1 = ax.coerceIn(0f, w - 1.001f); val y1 = ay.coerceIn(0f, h - 1.001f)
        val x2 = bx.coerceIn(0f, w - 1.001f); val y2 = by.coerceIn(0f, h - 1.001f)
        val i1 = y1.toInt() * w + x1.toInt(); val fx1 = x1 - x1.toInt(); val fy1 = y1 - y1.toInt()
        val i2 = y2.toInt() * w + x2.toInt(); val fx2 = x2 - x2.toInt(); val fy2 = y2 - y2.toInt()
        val dr = sample(img.r, w, i1, fx1, fy1) - sample(img.r, w, i2, fx2, fy2)
        val dg = sample(img.g, w, i1, fx1, fy1) - sample(img.g, w, i2, fx2, fy2)
        val db = sample(img.b, w, i1, fx1, fy1) - sample(img.b, w, i2, fx2, fy2)
        return sqrt((dr * dr + dg * dg + db * db).toDouble())
    }

    /** Mean color step across the side a→b, sampled [off] px outside and inside; and the share of strong steps. */
    private fun sideStep(img: Img, ax: Float, ay: Float, bx: Float, by: Float, off: Float, out: DoubleArray) {
        val dx = bx - ax; val dy = by - ay
        val len = hypot(dx, dy).takeIf { it > 0f } ?: 1f
        val nx = dy / len; val ny = -dx / len  // outward for a clockwise quad (y down)
        val steps = max(8, (len / 3).roundToInt())
        var sum = 0.0; var good = 0
        for (k in 1 until steps) {
            val t = k.toFloat() / steps
            if (t < 0.08f || t > 0.92f) continue
            val x = ax + dx * t; val y = ay + dy * t
            val d = colorStep(img, x + nx * off, y + ny * off, x - nx * off, y - ny * off)
            sum += d
            if (d > 14) good++
        }
        val cnt = (steps - 1).toDouble()
        out[0] = sum / cnt
        out[1] = good / cnt
    }

    private fun edgeSupport(img: Img, q: Quad): Double {
        val off = max(2f, max(img.w, img.h) / 170f)
        val r = DoubleArray(2)
        var prod = 1.0
        for (s in 0 until 4) {
            val a = q[s]; val b = q[(s + 1) % 4]
            sideStep(img, a.x, a.y, b.x, b.y, off, r)
            prod *= max(0.02, min(1.0, r[0] / 45) * 0.5 + r[1] * 0.5)
        }
        return prod.pow(0.25)
    }

    /** Nudges each side along its normal to the strongest step, then rebuilds the corners from the lines. */
    private fun refineQuad(img: Img, q: Quad): Quad {
        val off = max(2f, max(img.w, img.h) / 170f)
        val span = max(4, (max(img.w, img.h) / 50f).roundToInt())
        val r = DoubleArray(2)
        val lines = ArrayList<FloatArray>(4)
        for (s in 0 until 4) {
            val a = q[s]; val b = q[(s + 1) % 4]
            val dx = b.x - a.x; val dy = b.y - a.y
            val len = hypot(dx, dy).takeIf { it > 0f } ?: 1f
            val nx = dy / len; val ny = -dx / len
            var bestA = 0; var bestB = 0; var bestV = -1.0
            for (da in -span..span) for (db in -span..span) {
                sideStep(img, a.x + nx * da, a.y + ny * da, b.x + nx * db, b.y + ny * db, off, r)
                val v = r[0] - 0.15 * (abs(da) + abs(db))
                if (v > bestV) { bestV = v; bestA = da; bestB = db }
            }
            lines.add(floatArrayOf(a.x + nx * bestA, a.y + ny * bestA, b.x + nx * bestB, b.y + ny * bestB))
        }
        val out = ArrayList<Pt>(4)
        for (k in 0 until 4) {
            val c = intersect(lines[(k + 3) % 4], lines[k]) ?: return q
            if (hypot(c.x - q[k].x, c.y - q[k].y) > span * 3) return q
            out.add(c)
        }
        return out
    }

    private fun intersect(l1: FloatArray, l2: FloatArray): Pt? {
        val d1x = l1[2] - l1[0]; val d1y = l1[3] - l1[1]
        val d2x = l2[2] - l2[0]; val d2y = l2[3] - l2[1]
        val den = d1x * d2y - d1y * d2x
        if (abs(den) < 1e-6f) return null
        val t = ((l2[0] - l1[0]) * d2y - (l2[1] - l1[1]) * d2x) / den
        return Pt(l1[0] + d1x * t, l1[1] + d1y * t)
    }

    internal fun otsu(m: FloatArray): Float {
        val hist = DoubleArray(256)
        var mn = Float.POSITIVE_INFINITY; var mx = Float.NEGATIVE_INFINITY
        for (v in m) { if (v < mn) mn = v; if (v > mx) mx = v }
        val k = 255.0 / max(1e-6, (mx - mn).toDouble())
        for (v in m) hist[((v - mn) * k).toInt().coerceIn(0, 255)]++
        val n = m.size.toDouble()
        var sum = 0.0
        for (i in 0 until 256) sum += i * hist[i]
        var sB = 0.0; var wB = 0.0; var best = 0.0; var th = 128
        for (i in 0 until 256) {
            wB += hist[i]
            if (wB == 0.0) continue
            val wF = n - wB
            if (wF == 0.0) break
            sB += i * hist[i]
            val mB = sB / wB; val mF = (sum - sB) / wF
            val v = wB * wF * (mB - mF) * (mB - mF)
            if (v > best) { best = v; th = i }
        }
        return (mn + th / k).toFloat()
    }

    /** A box blur of radius [r], separable, with the window shrinking at the edges. */
    internal fun boxBlur(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        val tmp = FloatArray(w * h); val out = FloatArray(w * h)
        for (y in 0 until h) {
            var acc = 0.0; var cnt = 0; val o = y * w
            for (x in -r until w) {
                val add = x + r
                if (add < w) { acc += src[o + add]; cnt++ }
                val rem = x - r - 1
                if (rem >= 0) { acc -= src[o + rem]; cnt-- }
                if (x >= 0) tmp[o + x] = (acc / cnt).toFloat()
            }
        }
        for (x in 0 until w) {
            var acc = 0.0; var cnt = 0
            for (y in -r until h) {
                val add = y + r
                if (add < h) { acc += tmp[add * w + x]; cnt++ }
                val rem = y - r - 1
                if (rem >= 0) { acc -= tmp[rem * w + x]; cnt-- }
                if (y >= 0) out[y * w + x] = (acc / cnt).toFloat()
            }
        }
        return out
    }

    private fun localContrast(l: FloatArray, w: Int, h: Int): FloatArray {
        val r = (max(w, h) / 6f).roundToInt()
        val b = boxBlur(boxBlur(l, w, h, r), w, h, r)
        return FloatArray(w * h) { l[it] - b[it] }
    }

    /** Scratch arrays for [componentsAbove], reused across its many calls. */
    private class Buffers(n: Int) {
        val lab = IntArray(n)
        val stack = IntArray(n)
        val edge = IntArray(n)
    }

    /** Every 4-connected region above [t] becomes a candidate quad with a shape score. */
    private fun componentsAbove(m: FloatArray, w: Int, h: Int, t: Float, out: MutableList<Cand>, buf: Buffers) {
        val n = w * h
        val lab = buf.lab; val stack = buf.stack; val edgePts = buf.edge
        lab.fill(0)
        var id = 0
        for (i in 0 until n) {
            if (m[i] <= t || lab[i] != 0) continue
            id++
            var sp = 0; var size = 0; var border = 0; var ne = 0
            stack[sp++] = i; lab[i] = id
            while (sp > 0) {
                val p = stack[--sp]
                size++
                val x = p % w; val y = p / w
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) border++
                var edge = false
                if (x > 0) { if (m[p - 1] > t) { if (lab[p - 1] == 0) { lab[p - 1] = id; stack[sp++] = p - 1 } } else edge = true } else edge = true
                if (x < w - 1) { if (m[p + 1] > t) { if (lab[p + 1] == 0) { lab[p + 1] = id; stack[sp++] = p + 1 } } else edge = true } else edge = true
                if (y > 0) { if (m[p - w] > t) { if (lab[p - w] == 0) { lab[p - w] = id; stack[sp++] = p - w } } else edge = true } else edge = true
                if (y < h - 1) { if (m[p + w] > t) { if (lab[p + w] == 0) { lab[p + w] = id; stack[sp++] = p + w } } else edge = true } else edge = true
                if (edge) edgePts[ne++] = p
            }
            val frac = size.toDouble() / n
            if (frac < 0.02 || frac > 0.85 || ne < 8) continue
            val hull = convexHull(edgePts, ne, w)
            val quad = simplifyToQuad(hull) ?: continue
            val qa = polyArea(quad); val ha = polyArea(hull)
            if (qa < 1.0) continue
            val fill = size / qa
            val hullFit = ha / qa
            val bfrac = border.toDouble() / (2 * (w + h))
            val sc = min(fill, 1.0).pow(FE) * min(hullFit, 1 / hullFit).pow(2) * sqrt(qa / n) * max(0.0, 1 - bfrac * 8)
            out.add(Cand(orderQuad(quad), sc, frac))
        }
    }

    /** Monotone-chain hull of pixel centers given as indices y·w + x. */
    internal fun convexHull(idx: IntArray, count: Int, w: Int): List<Pt> {
        // Sort by x, then y: pack both into one Long (both are below 2^20 here).
        val keys = LongArray(count) { val p = idx[it]; ((p % w).toLong() shl 20) or (p / w).toLong() }
        keys.sort()
        val xs = FloatArray(count) { (keys[it] shr 20).toFloat() + 0.5f }
        val ys = FloatArray(count) { (keys[it] and 0xFFFFF).toFloat() + 0.5f }
        fun cross(o: Int, a: Int, b: Int) = (xs[a] - xs[o]) * (ys[b] - ys[o]) - (ys[a] - ys[o]) * (xs[b] - xs[o])
        val lo = IntArray(count + 1); var nl = 0
        for (i in 0 until count) {
            while (nl >= 2 && cross(lo[nl - 2], lo[nl - 1], i) <= 0) nl--
            lo[nl++] = i
        }
        val up = IntArray(count + 1); var nu = 0
        for (i in count - 1 downTo 0) {
            while (nu >= 2 && cross(up[nu - 2], up[nu - 1], i) <= 0) nu--
            up[nu++] = i
        }
        val res = ArrayList<Pt>(nl + nu)
        for (k in 0 until nl - 1) res.add(Pt(xs[lo[k]], ys[lo[k]]))
        for (k in 0 until nu - 1) res.add(Pt(xs[up[k]], ys[up[k]]))
        return res
    }

    internal fun polyArea(q: List<Pt>): Double {
        var a = 0.0
        for (i in q.indices) {
            val p = q[i]; val r = q[(i + 1) % q.size]
            a += p.x.toDouble() * r.y - r.x.toDouble() * p.y
        }
        return abs(a) / 2
    }

    /** Down to four vertices by dropping, again and again, the one whose loss costs the least area. */
    private fun simplifyToQuad(hull: List<Pt>): List<Pt>? {
        if (hull.size < 4) return null
        val poly = ArrayList(hull)
        fun tri(a: Pt, b: Pt, c: Pt) = abs((b.x - a.x).toDouble() * (c.y - a.y) - (b.y - a.y).toDouble() * (c.x - a.x)) / 2
        while (poly.size > 4) {
            var bi = 0; var ba = Double.POSITIVE_INFINITY
            val s = poly.size
            for (i in 0 until s) {
                val a = tri(poly[(i - 1 + s) % s], poly[i], poly[(i + 1) % s])
                if (a < ba) { ba = a; bi = i }
            }
            poly.removeAt(bi)
        }
        return poly
    }

    /** Clockwise on screen (y down), starting at the top-left corner (smallest x + y). */
    internal fun orderQuad(q: List<Pt>): Quad {
        val cx = q.sumOf { it.x.toDouble() } / 4; val cy = q.sumOf { it.y.toDouble() } / 4
        val s = q.sortedBy { atan2(it.y - cy, it.x - cx) }
        var ti = 0
        for (i in 1 until 4) if (s[i].x + s[i].y < s[ti].x + s[ti].y) ti = i
        return List(4) { s[(ti + it) % 4] }
    }
}
