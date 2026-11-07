package io.github.whatwhatwhoa.dailynote

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.withScale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** The five papers. Each is drawn from the numbers in [PaperArt] as vector shapes; no images. */
enum class Paper(val label: String) {
    YELLOW("Yellow pad"),
    COLLEGE("College"),
    LEGAL("Legal pad"),
    GRID("Grid"),
    DOTS("Dot grid"),
}

/** The text size. The rule spacing follows it, so a larger size also means wider lines. */
enum class NoteSize(val label: String, val sp: Float) {
    SMALL("Small", 15f),
    MEDIUM("Medium", 18f),
    LARGE("Large", 22f),
}

/**
 * The ink's typeface. Handwriting is Patrick Hand (bundled, OFL), whose letters are a little
 * small for its size: it is drawn 1.15x larger, about the normal font's x-height, on the same
 * line spacing.
 */
enum class NoteFont(val label: String, val scale: Float) {
    NORMAL("Normal", 1f),
    HAND("Handwriting", 1.15f),
}

/**
 * Draws the papers onto an Android canvas: the page (rules, grid, dots, margins) and the band
 * along its top (a pad's glued binding, a spiral, or a plain edge). The page in the app and
 * every line of the widget come from here, so both always match.
 */
object PaperArt {
    const val INK = 0xFF1D2433.toInt()

    private class Look(
        val paper: Int,
        val rule: Int,
        val major: Int = rule,
        val margins: FloatArray = floatArrayOf(),
        val margin: Int = 0,
        val textStart: Float = 20f,
        val band: Int = paper,
        val bandEdge: Int,
        val darkBand: Boolean = false,
    )

    private val looks = mapOf(
        Paper.YELLOW to Look(
            paper = 0xFFFFF4B5.toInt(), rule = 0xFFB4C2CC.toInt(),
            band = 0xFF7A4E2D.toInt(), bandEdge = 0xFF5A3820.toInt(), darkBand = true,
        ),
        Paper.COLLEGE to Look(
            paper = 0xFFFDFDFB.toInt(), rule = 0xFFA9C8EC.toInt(),
            margins = floatArrayOf(42f), margin = 0xFFEE8E96.toInt(), textStart = 52f,
            band = 0xFFF0F0EC.toInt(), bandEdge = 0x26000000,
        ),
        Paper.LEGAL to Look(
            paper = 0xFFFCEC79.toInt(), rule = 0xFF8DB0D6.toInt(),
            margins = floatArrayOf(44f, 48f), margin = 0xFFE0473C.toInt(), textStart = 58f,
            band = 0xFFB23A30.toInt(), bandEdge = 0xFF8A2A22.toInt(), darkBand = true,
        ),
        Paper.GRID to Look(
            paper = 0xFFFAFBF8.toInt(), rule = 0xFFD3E2EE.toInt(), major = 0xFFAFC9DE.toInt(),
            bandEdge = 0x1F000000,
        ),
        Paper.DOTS to Look(
            paper = 0xFFFBFAF6.toInt(), rule = 0xFFA9ADB6.toInt(), bandEdge = 0x1F000000,
        ),
    )

    private fun look(p: Paper) = looks.getValue(p)

    fun paperColor(p: Paper) = look(p).paper
    fun darkBand(p: Paper) = look(p).darkBand

    /** Whole pixels, so every rule lands on the pixel grid and stays crisp. */
    fun lineHeight(textPx: Float) = (textPx * 1.8f).roundToInt()

    /** How far below a line's baseline its rule sits: the ink rests just on the line. */
    fun ruleGap(textPx: Float) = textPx * 0.15f

    /** The first grid column, centred so the grid's leftover is split between both edges. */
    private fun gridLeft(width: Float, lineH: Float) = (width % lineH) / 2f

    /** Where the ink starts: right of the margin, or one grid square in. */
    fun textStart(p: Paper, d: Float, width: Float, lineH: Float): Float = when (p) {
        Paper.GRID, Paper.DOTS -> gridLeft(width, lineH) + lineH + 4 * d
        else -> look(p).textStart * d
    }

    fun textEnd(d: Float) = 16 * d

    /**
     * The page between [top] and [bottom]. [firstRule] is the y of the rule under the first line of
     * text; rules repeat every [lineH] below it. Ruled papers have no rules above it, grids do.
     */
    fun drawSheet(
        c: Canvas, p: Paper, d: Float, width: Float, top: Float, bottom: Float,
        firstRule: Float, lineH: Float,
    ) {
        val look = look(p)
        val paint = Paint()
        paint.color = look.paper
        c.drawRect(0f, top, width, bottom, paint)

        val w = max(1f, d.roundToInt().toFloat())
        val k0 = floor((top - firstRule) / lineH).toInt() - 1
        val k1 = ceil((bottom - firstRule) / lineH).toInt() + 1
        when (p) {
            Paper.YELLOW, Paper.COLLEGE, Paper.LEGAL -> {
                paint.color = look.rule
                for (k in max(k0, 0)..k1) hLine(c, paint, width, firstRule + k * lineH, w)
            }
            Paper.GRID -> {
                for (k in k0..k1) {
                    paint.color = if (k.mod(5) == 0) look.major else look.rule
                    hLine(c, paint, width, firstRule + k * lineH, w)
                }
                var x = gridLeft(width, lineH)
                var j = 0
                while (x <= width) {
                    paint.color = if (j % 5 == 0) look.major else look.rule
                    vLine(c, paint, x, top, bottom, w)
                    x += lineH
                    j++
                }
            }
            Paper.DOTS -> {
                paint.isAntiAlias = true
                paint.color = look.rule
                val r = 1.35f * d
                for (k in k0..k1) {
                    val y = firstRule + k * lineH
                    var x = gridLeft(width, lineH)
                    while (x <= width) {
                        c.drawCircle(x, y, r, paint)
                        x += lineH
                    }
                }
            }
        }
        paint.isAntiAlias = false
        paint.color = look.margin
        for (m in look.margins) vLine(c, paint, m * d, top, bottom, w)
    }

    /**
     * The band across the top, [height] tall from y = 0; its marks sit at its bottom, so a band
     * that also covers the status bar looks the same as the widget's short one. The spiral stops
     * [endInset] short of the right edge, where the app puts its paper button.
     */
    fun drawBand(c: Canvas, p: Paper, d: Float, width: Float, height: Float, endInset: Float = 0f) {
        val look = look(p)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = look.band
        c.drawRect(0f, 0f, width, height, paint)
        when (p) {
            // A glued binding, with a darker lip where it meets the paper.
            Paper.YELLOW, Paper.LEGAL -> {
                paint.color = look.bandEdge
                c.drawRect(0f, height - 3 * d, width, height, paint)
            }
            Paper.COLLEGE -> {
                spiral(c, paint, d, width - endInset, height)
                paint.color = look.bandEdge
                c.drawRect(0f, height - max(1f, d.roundToInt().toFloat()), width, height, paint)
            }
            Paper.GRID, Paper.DOTS -> {
                paint.color = look.bandEdge
                c.drawRect(0f, height - max(1f, d.roundToInt().toFloat()), width, height, paint)
            }
        }
    }

    /** A miniature page for the paper picker: as if a phone page were seen from arm's length. */
    fun drawThumb(c: Canvas, p: Paper, d: Float, width: Float, height: Float) {
        val s = 0.42f
        c.withScale(s, s) {
            val w = width / s
            val h = height / s
            val lineH = 30 * d
            val band = 26 * d
            drawSheet(this, p, d, w, 0f, h, band + lineH * 0.9f, lineH)
            drawBand(this, p, d, w, band)
        }
    }

    // Wire coils through punched holes, every 14 dp along the top.
    private fun spiral(c: Canvas, paint: Paint, d: Float, right: Float, height: Float) {
        val holeY = height - 8 * d
        val topY = max(0f, height - 24 * d)
        var x = 16 * d
        paint.strokeCap = Paint.Cap.ROUND
        while (x < right - 8 * d) {
            paint.style = Paint.Style.FILL
            paint.color = 0xFF4A4F57.toInt()
            c.drawCircle(x, holeY, 3 * d, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.4f * d
            paint.color = 0xFF8E949C.toInt()
            c.drawLine(x - 1.5f * d, holeY, x + 3.5f * d, topY, paint)
            paint.strokeWidth = 0.9f * d
            paint.color = 0xFFDADDE2.toInt()
            c.drawLine(x - 1.8f * d, holeY - 1.5f * d, x + 2.6f * d, topY + 1.2f * d, paint)
            x += 14 * d
        }
        paint.style = Paint.Style.FILL
    }

    private fun hLine(c: Canvas, paint: Paint, width: Float, y: Float, w: Float) {
        val t = (y - w / 2).roundToInt().toFloat()
        c.drawRect(0f, t, width, t + w, paint)
    }

    private fun vLine(c: Canvas, paint: Paint, x: Float, top: Float, bottom: Float, w: Float) {
        val l = (x - w / 2).roundToInt().toFloat()
        c.drawRect(l, top, l + w, bottom, paint)
    }
}
