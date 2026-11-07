package io.github.whatwhatwhoa.scan

import java.io.OutputStream
import java.util.Locale
import kotlin.math.min

/** The PDF's page size: each page the size of its scan, or the scan centered on a paper size. */
enum class PageSize(val key: String, val label: String, val desc: String) {
    Fit("fit", "Fit to scan", "Each page is the size of its scan, at 200 dpi."),
    Letter("letter", "US Letter", "8.5 × 11 in, the scan centered with a margin; wide scans turn the page."),
    A4("a4", "A4", "210 × 297 mm, the scan centered with a margin; wide scans turn the page.");

    companion object {
        fun of(key: String?): PageSize = entries.firstOrNull { it.key == key } ?: Fit
    }
}

/** JPEG quality of the pages in the PDF. */
enum class Quality(val key: String, val label: String, val jpeg: Int, val desc: String) {
    High("high", "High", 92, "Larger file."),
    Standard("standard", "Standard", 80, "Sharp text at a moderate size."),
    Small("small", "Small", 60, "Email friendly.");

    companion object {
        fun of(key: String?): Quality = entries.firstOrNull { it.key == key } ?: Standard
    }
}

/** Where an image goes on its page, in points from the page's top-left corner. */
data class Placement(val pageW: Double, val pageH: Double, val x: Double, val y: Double, val w: Double, val h: Double)

object Layout {
    private const val FIT_DPI = 200.0
    private const val MARGIN = 24.0

    fun place(size: PageSize, imgW: Int, imgH: Int): Placement {
        if (size == PageSize.Fit) {
            val pw = imgW * 72 / FIT_DPI; val ph = imgH * 72 / FIT_DPI
            return Placement(pw, ph, 0.0, 0.0, pw, ph)
        }
        val (a, b) = if (size == PageSize.A4) 595.28 to 841.89 else 612.0 to 792.0
        val land = imgW > imgH
        val pw = if (land) b else a; val ph = if (land) a else b
        val sc = min((pw - 2 * MARGIN) / imgW, (ph - 2 * MARGIN) / imgH)
        val w = imgW * sc; val h = imgH * sc
        return Placement(pw, ph, (pw - w) / 2, (ph - h) / 2, w, h)
    }
}

/**
 * A minimal PDF 1.4 writer for scanned pages: one JPEG per page, embedded as it is (DCTDecode),
 * so the file is the JPEGs plus a few hundred bytes each. It writes as it goes, so only one
 * page is ever in memory. Objects: 1 catalog, 2 page tree, 3 info, then per page its page,
 * image and content.
 */
class PdfWriter(sink: OutputStream, private val pageCount: Int, producer: String, creationDate: String) {
    private val out = Counting(sink)
    private val offsets = LongArray(3 + 3 * pageCount + 1)
    private var added = 0

    init {
        require(pageCount > 0) { "a PDF needs a page" }
        out.write(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A)) // %PDF-1.4
        out.write(byteArrayOf(0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A)) // binary marker
        obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        val kids = (0 until pageCount).joinToString(" ") { "${pageObj(it)} 0 R" }
        obj(2, "<< /Type /Pages /Kids [$kids] /Count $pageCount >>")
        obj(3, "<< /Producer ${text(producer)} /CreationDate ${text("D:$creationDate")} >>")
    }

    private fun pageObj(i: Int) = 4 + 3 * i

    /** Adds the next page: [jpeg] is [imgW] × [imgH] pixels, placed as [p] says. */
    fun addPage(jpeg: ByteArray, imgW: Int, imgH: Int, p: Placement) {
        check(added < pageCount) { "more pages than announced" }
        val n = pageObj(added)
        obj(n, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${num(p.pageW)} ${num(p.pageH)}] " +
            "/Resources << /XObject << /Im0 ${n + 1} 0 R >> >> /Contents ${n + 2} 0 R >>")
        stream(n + 1, "/Type /XObject /Subtype /Image /Width $imgW /Height $imgH /ColorSpace /DeviceRGB " +
            "/BitsPerComponent 8 /Filter /DCTDecode", jpeg)
        // PDF's origin is the bottom-left corner.
        val y = p.pageH - p.y - p.h
        val content = "q ${num(p.w)} 0 0 ${num(p.h)} ${num(p.x)} ${num(y)} cm /Im0 Do Q\n"
        stream(n + 2, "", content.toByteArray(Charsets.US_ASCII))
        added++
    }

    /** Writes the cross-reference table and trailer; the stream is left open for the caller. */
    fun finish() {
        check(added == pageCount) { "announced $pageCount pages, added $added" }
        val xref = out.count
        val size = offsets.size
        val sb = StringBuilder("xref\n0 $size\n0000000000 65535 f \n")
        for (i in 1 until size) sb.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]))
        sb.append("trailer\n<< /Size $size /Root 1 0 R /Info 3 0 R >>\nstartxref\n$xref\n%%EOF\n")
        ascii(sb.toString())
        out.flush()
    }

    private fun obj(n: Int, body: String) {
        offsets[n] = out.count
        ascii("$n 0 obj\n$body\nendobj\n")
    }

    private fun stream(n: Int, dict: String, data: ByteArray) {
        offsets[n] = out.count
        val d = if (dict.isEmpty()) "" else "$dict "
        ascii("$n 0 obj\n<< $d/Length ${data.size} >>\nstream\n")
        out.write(data)
        ascii("\nendstream\nendobj\n")
    }

    private fun ascii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))

    companion object {
        /** A number for PDF: a dot as the decimal mark in every locale, at most two decimals. */
        fun num(v: Double): String {
            val s = String.format(Locale.ROOT, "%.2f", v).trimEnd('0').trimEnd('.')
            return if (s == "-0") "0" else s
        }

        /** A PDF literal string, ASCII only (anything else becomes '?'). */
        fun text(s: String): String {
            val sb = StringBuilder("(")
            for (ch in s) when {
                ch == '(' || ch == ')' || ch == '\\' -> sb.append('\\').append(ch)
                ch.code in 32..126 -> sb.append(ch)
                else -> sb.append('?')
            }
            return sb.append(')').toString()
        }
    }

    private class Counting(private val inner: OutputStream) : OutputStream() {
        var count = 0L
            private set

        override fun write(b: Int) { inner.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { inner.write(b, off, len); count += len }
        override fun flush() = inner.flush()
        override fun close() = inner.close()
    }
}
