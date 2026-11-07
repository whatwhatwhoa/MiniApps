package io.github.whatwhatwhoa.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.math.hypot
import kotlin.random.Random

class CoreTest {
    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    private fun inside(q: Quad, x: Float, y: Float): Boolean {
        // Clockwise on screen: inside is to the right of every side (cross ≥ 0 with y down).
        for (i in 0 until 4) {
            val a = q[i]; val b = q[(i + 1) % 4]
            if ((b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x) < 0) return false
        }
        return true
    }

    /** A light receipt with a few dark lines of "text" on a darker, noisy table. */
    private fun photo(w: Int, h: Int, paper: Quad, seed: Int = 1): IntArray {
        val rnd = Random(seed)
        return IntArray(w * h) { i ->
            val x = i % w + 0.5f; val y = i / w + 0.5f
            val n = rnd.nextInt(-6, 7)
            if (inside(paper, x, y)) {
                val line = ((y - paper[0].y).toInt() / 14) % 3 == 0 && (x.toInt() / 9) % 4 != 0
                if (line) rgb(70 + n, 70 + n, 75 + n) else rgb(232 + n, 229 + n, 222 + n)
            } else rgb(96 + n, 78 + n, 62 + n)
        }
    }

    private fun assertNear(want: Quad, got: Quad, tol: Float) {
        for (k in 0 until 4) {
            val d = hypot(want[k].x - got[k].x, want[k].y - got[k].y)
            assertTrue("corner $k: want ${want[k]}, got ${got[k]} (off by $d px)", d <= tol)
        }
    }

    @Test fun findsATiltedReceipt() {
        val paper = listOf(Pt(150f, 90f), Pt(640f, 120f), Pt(610f, 520f), Pt(130f, 500f))
        val q = Detect.detect(photo(800, 600, paper), 800, 600)
        assertNear(paper, q, 8f)
    }

    @Test fun findsAPortraitPageInABigPhoto() {
        // Larger than the work size, so the shrink and the scale back are exercised too.
        val paper = listOf(Pt(300f, 260f), Pt(1450f, 330f), Pt(1380f, 2050f), Pt(260f, 1980f))
        val q = Detect.detect(photo(1800, 2400, paper, seed = 7), 1800, 2400)
        assertNear(paper, q, 14f)
    }

    @Test fun aPlainPhotoFallsBackToTheInset() {
        val px = IntArray(400 * 300) { rgb(120, 120, 120) }
        assertEquals(Detect.inset(400, 300), Detect.detect(px, 400, 300))
    }

    @Test fun cornersComeClockwiseFromTopLeft() {
        val shuffled = listOf(Pt(10f, 90f), Pt(90f, 10f), Pt(10f, 10f), Pt(90f, 90f))
        assertEquals(listOf(Pt(10f, 10f), Pt(90f, 10f), Pt(90f, 90f), Pt(10f, 90f)), Detect.orderQuad(shuffled))
    }

    @Test fun documentWhitensUnevenPaperAndKeepsInk() {
        val w = 300; val h = 200
        // Paper lit from the left (bright to dim), with an ink stroke across the middle.
        val px = IntArray(w * h) { i ->
            val x = i % w; val y = i / w
            val light = 230 - x * 100 / w
            if (y in 95..104 && x in 40..260) rgb(light / 4, light / 4, light / 3) else rgb(light, light, light - 6)
        }
        Filters.document(px, w, h)
        fun lum(c: Int) = ((c shr 16) and 255)
        assertTrue("bright side of the paper is white", lum(px[20 * w + 20]) >= 245)
        assertTrue("dim side of the paper is white too", lum(px[20 * w + 280]) >= 245)
        assertTrue("ink stays dark", lum(px[100 * w + 150]) <= 40)
    }

    @Test fun blackAndWhiteIsOnlyBlackAndWhite() {
        val rnd = Random(3)
        val px = IntArray(120 * 80) { rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        Filters.bw(px, 120, 80)
        assertTrue(px.all { it == rgb(0, 0, 0) || it == rgb(255, 255, 255) })
    }

    @Test fun photoStretchesContrast() {
        val px = IntArray(100 * 100) { i -> val v = 80 + (i % 100); rgb(v, v, v) }
        Filters.enhance(px)
        assertTrue((px.minOf { (it shr 16) and 255 }) <= 3)
        assertTrue((px.maxOf { (it shr 16) and 255 }) >= 250)
    }

    @Test fun pdfNumbersUseADotEverywhere() {
        assertEquals("612", PdfWriter.num(612.0))
        assertEquals("595.28", PdfWriter.num(595.28))
        assertEquals("0.5", PdfWriter.num(0.5))
        assertEquals("0", PdfWriter.num(-0.001))
        assertEquals("(a\\(b\\) ?)", PdfWriter.text("a(b) é"))
    }

    @Test fun pageLayout() {
        val fit = Layout.place(PageSize.Fit, 1700, 2200)
        assertEquals(612.0, fit.pageW, 1e-9)
        assertEquals(792.0, fit.pageH, 1e-9)
        val wide = Layout.place(PageSize.Letter, 3000, 1000)
        assertEquals(792.0, wide.pageW, 1e-9)  // a wide scan turns the page
        assertEquals((792.0 - wide.w) / 2, wide.x, 1e-9)
        assertEquals(792.0 - 48, wide.w, 1e-9)
        val a4 = Layout.place(PageSize.A4, 1000, 1414)
        assertTrue(a4.w <= 595.28 - 48 + 1e-9 && a4.h <= 841.89 - 48 + 1e-9)
    }

    @Test fun pdfCrossReferencePointsAtEveryObject() {
        val bos = ByteArrayOutputStream()
        val jpeg1 = ByteArray(1000) { it.toByte() }
        val jpeg2 = ByteArray(321) { 7 }
        val pdf = PdfWriter(bos, 2, "Scan test", "20261107050000")
        pdf.addPage(jpeg1, 40, 50, Layout.place(PageSize.Fit, 40, 50))
        pdf.addPage(jpeg2, 60, 30, Layout.place(PageSize.A4, 60, 30))
        pdf.finish()
        val bytes = bos.toByteArray()
        val s = String(bytes, Charsets.ISO_8859_1)
        assertTrue(s.startsWith("%PDF-1.4\n"))
        assertTrue(s.endsWith("%%EOF\n"))
        val startxref = Regex("startxref\n(\\d+)\n").find(s)!!.groupValues[1].toInt()
        assertTrue(s.startsWith("xref\n", startxref))
        val rows = Regex("(\\d{10}) 00000 n ").findAll(s).map { it.groupValues[1].toInt() }.toList()
        assertEquals(9, rows.size)  // catalog, pages, info + 3 per page
        rows.forEachIndexed { i, off -> assertTrue("object ${i + 1} at $off", s.startsWith("${i + 1} 0 obj\n", off)) }
        assertTrue(s.contains("/Kids [4 0 R 7 0 R] /Count 2"))
        assertTrue(s.contains("/Length 1000 >>\nstream\n"))
        assertTrue(s.contains("/MediaBox [0 0 841.89 595.28]"))  // A4, turned for the wide page
    }
}
