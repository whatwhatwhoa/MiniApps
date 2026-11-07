package io.github.whatwhatwhoa.dailynote.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import androidx.core.graphics.createBitmap
import io.github.whatwhatwhoa.dailynote.Note
import io.github.whatwhatwhoa.dailynote.NoteFont
import io.github.whatwhatwhoa.dailynote.Paper
import io.github.whatwhatwhoa.dailynote.PaperArt
import io.github.whatwhatwhoa.dailynote.Prefs
import io.github.whatwhatwhoa.dailynote.R
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * One widget's page at its current size: the note wrapped to the widget's width, line by line,
 * the strip of paper each line sits on, and each line's ink. Every line of a paper is the same
 * strip (the grid has two: its darker fifth lines), and the launcher gets the very same bitmap
 * for each, so it is stored once. The ink is drawn here, in the app's font (a widget can't use
 * an app's fonts), as an alpha-only picture cut to the line's width: about 100 KB a line.
 * Lines run on past the text to fill the widget, the last one cut to fit, so a note that fits
 * has nothing to scroll; a longer one scrolls to a blank line past its end.
 */
class WidgetPage(context: Context, manager: AppWidgetManager, id: Int) {
    val paper = Prefs.paper
    private val d = context.resources.displayMetrics.density
    private val text = Note.read().trimEnd()
    val width: Int
    private val height: Int
    private val bandH = (BAND_DP * d).roundToInt()
    private val lineH: Int
    private val rule: Float
    val textStart: Int

    /** A line's baseline in its row: the font's line centred in the row, as in the app. */
    private val baseline: Int
    private val layout: StaticLayout
    val rows: Int

    /** The last row's height when the note fits and the widget ends partway through a line, else 0. */
    private val tail: Int
    private val strips = HashMap<Int, Bitmap>()

    init {
        // A phone in portrait gets the narrowest width and tallest height the launcher reports.
        val o = manager.getAppWidgetOptions(id)
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val wDp = o.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val hDp = o.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        width = ((if (wDp > 0) wDp else 250) * d).roundToInt()
        height = ((if (hDp > 0) hDp else 250) * d).roundToInt()

        val font = Prefs.font
        val basePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, Prefs.size.sp, context.resources.displayMetrics)
        lineH = PaperArt.lineHeight(basePx)
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = basePx * font.scale
            typeface = if (font == NoteFont.HAND) context.resources.getFont(R.font.patrick_hand) else Typeface.DEFAULT
        }
        val fm = paint.fontMetricsInt
        baseline = (lineH - (fm.descent - fm.ascent)) / 2 - fm.ascent
        rule = baseline + PaperArt.ruleGap(basePx)
        textStart = PaperArt.textStart(paper, d, width.toFloat(), lineH.toFloat()).roundToInt()
        val textWidth = max(1, width - textStart - PaperArt.textEnd(d).roundToInt())
        layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, textWidth)
            .setIncludePad(false)
            .build()
        // The rows end a dp short of the widget's bottom: the launcher reports whole dp, and a
        // list even a pixel taller than the widget scrolls.
        val space = height - bandH - d.roundToInt()
        val full = space / lineH
        if (layout.lineCount <= full) {
            tail = space - full * lineH
            rows = full + if (tail > 0) 1 else 0
        } else {
            tail = 0
            rows = layout.lineCount + 1
        }
    }

    fun band(): Bitmap {
        val b = createBitmap(width, bandH, Bitmap.Config.RGB_565)
        PaperArt.drawBand(Canvas(b), paper, d, width.toFloat(), bandH.toFloat())
        return b
    }

    /** The paper under line [i]; the last row's is cut short when the widget ends within it. */
    fun strip(i: Int): Bitmap {
        val major = paper == Paper.GRID && i % 5 == 0
        val h = if (tail > 0 && i == rows - 1) tail else lineH
        return strips.getOrPut(h * 2 + if (major) 1 else 0) {
            val b = createBitmap(width, h, Bitmap.Config.RGB_565)
            val k = if (major) 0 else 1
            PaperArt.drawSheet(Canvas(b), paper, d, width.toFloat(), 0f, h.toFloat(), rule - k * lineH, lineH.toFloat())
            b
        }
    }

    /** Line [i]'s ink and how far from the text's start it begins; null for a blank line. */
    fun ink(i: Int): Pair<Bitmap, Int>? {
        if (i >= layout.lineCount || line(i).isBlank()) return null
        val left = floor(layout.getLineLeft(i)).toInt()
        val right = ceil(layout.getLineRight(i)).toInt() + 2
        val b = createBitmap(max(1, right - left), lineH, Bitmap.Config.ALPHA_8)
        val c = Canvas(b)
        c.translate(-left.toFloat(), (baseline - layout.getLineBaseline(i)).toFloat())
        c.clipRect(left.toFloat(), layout.getLineTop(i).toFloat(), right.toFloat(), layout.getLineBottom(i).toFloat())
        layout.draw(c)
        return b to left
    }

    /** Line [i]'s text, as wrapped to the widget; empty below the text. */
    fun line(i: Int): String {
        if (i >= layout.lineCount) return ""
        return text.substring(layout.getLineStart(i), layout.getLineEnd(i)).trimEnd('\n')
    }

    /** Where a tap on line [i] puts the cursor: the end of that line, or blank lines down to it. */
    fun jumpFor(i: Int): Pair<Int, Int> {
        // An empty note still has one (empty) line, so the first line needs no blank lines.
        if (i < layout.lineCount) {
            var end = layout.getLineEnd(i)
            if (end > 0 && text[end - 1] == '\n') end--
            return end to 0
        }
        return text.length to i - layout.lineCount + 1
    }

    companion object {
        const val BAND_DP = 26f
    }
}
