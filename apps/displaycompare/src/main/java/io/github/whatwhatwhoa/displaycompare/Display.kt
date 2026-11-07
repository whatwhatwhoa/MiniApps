package io.github.whatwhatwhoa.displaycompare

import android.content.Context
import android.view.WindowManager
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A named shape, long side first. [Pixels] takes the shape from the resolution (square pixels).
 * The order is the segmented control's; the names are saved, so they never change.
 */
enum class Aspect(val label: String, val long: Double, val short: Double) {
    W16x9("16:9", 16.0, 9.0),
    W16x10("16:10", 16.0, 10.0),
    W3x2("3:2", 3.0, 2.0),
    W4x3("4:3", 4.0, 3.0),
    W21x9("21:9", 21.0, 9.0),
    W32x9("32:9", 32.0, 9.0),
    /** iPhones since the X: 2556 × 1179 and its kin. */
    W19_5x9("19.5:9", 19.5, 9.0),
    /** Most Android phones: 2400 × 1080, 2992 × 1344. */
    W20x9("20:9", 20.0, 9.0),
    Pixels("Pixels", 0.0, 0.0);

    val ratio: Double get() = long / short

    companion object {
        val named = entries.filter { it != Pixels }
        fun nearest(ratio: Double): Aspect = named.minBy { abs(it.ratio - ratio) }
    }
}

/**
 * One display. [diagonal] is the picture's, in inches, as displays are sold everywhere. The
 * shape is [aspect], or the pixels' when [aspect] is [Aspect.Pixels]. [portrait] stands the
 * display on its short side. [slot] fixes its letter and color for as long as it exists.
 */
data class Display(
    val slot: Int,
    val diagonal: Double,
    val aspect: Aspect,
    val resW: Int? = null,
    val resH: Int? = null,
    val portrait: Boolean = false,
    val name: String? = null,
) {
    val letter: String get() = ('A' + slot).toString()
    val hasRes: Boolean get() = (resW ?: 0) > 0 && (resH ?: 0) > 0

    /** Long side ÷ short side of the picture. */
    val ratio: Double
        get() = if (aspect == Aspect.Pixels && hasRes) max(resW!!, resH!!).toDouble() / min(resW, resH)
        else if (aspect == Aspect.Pixels) Aspect.W16x9.ratio else aspect.ratio

    private val longIn: Double get() = diagonal * ratio / hypot(ratio, 1.0)
    private val shortIn: Double get() = diagonal / hypot(ratio, 1.0)
    val widthIn: Double get() = if (portrait) shortIn else longIn
    val heightIn: Double get() = if (portrait) longIn else shortIn
    val areaIn2: Double get() = longIn * shortIn

    /** Pixels per inch along the diagonal; null without a resolution. */
    val ppi: Double? get() = if (hasRes) hypot(resW!!.toDouble(), resH!!.toDouble()) / diagonal else null

    /** The pixels' own ratio, when a named shape was picked that it doesn't match (> 1 % apart). */
    val pixelRatioMismatch: Double?
        get() {
            if (!hasRes || aspect == Aspect.Pixels) return null
            val px = max(resW!!, resH!!).toDouble() / min(resW, resH)
            return if (abs(px - aspect.ratio) / aspect.ratio > 0.01) px else null
        }

    /** "16:9", or for pixels the matching name, else the ratio ("2.39:1"). */
    val shapeLabel: String
        get() = if (aspect != Aspect.Pixels) aspect.label
        else if (hasRes) Fmt.pixelRatio(max(resW!!, resH!!), min(resW, resH)) else "16:9"

    /** The name a preset gave it, else a plain description. */
    val title: String get() = name ?: "${Fmt.diag(diagonal)} $shapeLabel"
}

enum class Units(val label: String, val len: String) { Inches("in", "in"), Centimeters("cm", "cm") }

/** How the displays sit on top of each other on the mat. */
enum class Align(val label: String) { Bottom("Bottom"), Center("Center"), Corner("Corner") }

/** The preset families, in the order of the sheet's tabs. */
enum class Family(val label: String) { Phones("Phones"), Tablets("Tablets"), Laptops("Laptops"), Monitors("Monitors"), TVs("TVs") }

/**
 * A named display to start from. [diagonal] is the viewable size for monitors and TVs (a 65″
 * class TV is 64.5″) and the maker's figure for phones, tablets and laptops. Phones stand
 * upright; everything else lies landscape.
 */
data class Preset(val family: Family, val name: String, val diagonal: Double, val resW: Int, val resH: Int) {
    val portrait: Boolean get() = family == Family.Phones

    fun toDisplay(slot: Int) = Display(slot, diagonal, Aspect.Pixels, resW, resH, portrait = portrait, name = name)
}

object Presets {
    val all = listOf(
        Preset(Family.Phones, "6.1″ iPhone", 6.1, 2556, 1179),
        Preset(Family.Phones, "6.3″ iPhone Pro", 6.3, 2622, 1206),
        Preset(Family.Phones, "6.9″ iPhone Pro Max", 6.9, 2868, 1320),
        Preset(Family.Phones, "6.3″ Pixel", 6.3, 2424, 1080),
        Preset(Family.Phones, "6.8″ Pixel Pro XL", 6.8, 2992, 1344),
        Preset(Family.Phones, "6.2″ Galaxy S", 6.2, 2340, 1080),
        Preset(Family.Phones, "6.9″ Galaxy S Ultra", 6.9, 3120, 1440),
        Preset(Family.Tablets, "8.3″ iPad mini", 8.3, 2266, 1488),
        Preset(Family.Tablets, "10.9″ iPad", 10.9, 2360, 1640),
        Preset(Family.Tablets, "11″ iPad Pro", 11.0, 2388, 1668),
        Preset(Family.Tablets, "12.9″ iPad Pro", 12.9, 2732, 2048),
        Preset(Family.Tablets, "8.8″ Android tablet", 8.8, 2560, 1600),
        Preset(Family.Tablets, "11″ Galaxy Tab S", 11.0, 2560, 1600),
        Preset(Family.Laptops, "13.6″ MacBook Air", 13.6, 2560, 1664),
        Preset(Family.Laptops, "15.3″ MacBook Air", 15.3, 2880, 1864),
        Preset(Family.Laptops, "14.2″ MacBook Pro", 14.2, 3024, 1964),
        Preset(Family.Laptops, "16.2″ MacBook Pro", 16.2, 3456, 2234),
        Preset(Family.Laptops, "13.8″ 3:2 laptop", 13.8, 2304, 1536),
        Preset(Family.Laptops, "14″ 16:10 laptop", 14.0, 1920, 1200),
        Preset(Family.Laptops, "15.6″ 1080p laptop", 15.6, 1920, 1080),
        Preset(Family.Laptops, "16″ 16:10 laptop", 16.0, 2560, 1600),
        Preset(Family.Monitors, "24″ class 1080p", 23.8, 1920, 1080),
        Preset(Family.Monitors, "24″ 16:10", 24.1, 1920, 1200),
        Preset(Family.Monitors, "27″ 1440p", 27.0, 2560, 1440),
        Preset(Family.Monitors, "27″ 4K", 27.0, 3840, 2160),
        Preset(Family.Monitors, "27″ 5K", 27.0, 5120, 2880),
        Preset(Family.Monitors, "32″ class 4K", 31.5, 3840, 2160),
        Preset(Family.Monitors, "34″ ultrawide", 34.0, 3440, 1440),
        Preset(Family.Monitors, "38″ class ultrawide", 37.5, 3840, 1600),
        Preset(Family.Monitors, "49″ super ultrawide", 49.0, 5120, 1440),
        Preset(Family.TVs, "43″ class 4K TV", 42.5, 3840, 2160),
        Preset(Family.TVs, "55″ class 4K TV", 54.6, 3840, 2160),
        Preset(Family.TVs, "65″ class 4K TV", 64.5, 3840, 2160),
        Preset(Family.TVs, "77″ class 4K TV", 76.7, 3840, 2160),
        Preset(Family.TVs, "85″ class 4K TV", 84.5, 3840, 2160),
    )

    fun named(name: String): Preset = all.first { it.name == name }

    /** The family a display came from, to open the sheet there; null for a custom one. */
    fun familyOf(d: Display): Family? =
        if (d.name == "This phone") Family.Phones else all.firstOrNull { it.name == d.name }?.family

    /**
     * The phone in hand, as Android reports it: its full panel in pixels ÷ the panel's dpi, held
     * upright. Makers quote the diagonal of the uncut rectangle, so this can read ~0.1″ under.
     */
    fun thisPhone(context: Context, slot: Int): Display {
        val dm = context.resources.displayMetrics
        val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val wIn = bounds.width() / dm.xdpi
        val hIn = bounds.height() / dm.ydpi
        val mode = context.display.mode
        val a = mode.physicalWidth
        val b = mode.physicalHeight
        return Display(
            slot = slot,
            diagonal = (hypot(wIn, hIn) * 10).roundToInt() / 10.0,
            aspect = Aspect.Pixels,
            resW = min(a, b), resH = max(a, b),
            portrait = true,
            name = "This phone",
        )
    }
}

/** Numbers as the UI shows them: American English, one decimal for lengths. */
object Fmt {
    private val us = Locale.US

    fun one(x: Double): String = String.format(us, "%.1f", x)

    /** 27″, 23.8″, 6.7″: no trailing .0. */
    fun diag(d: Double): String = trim(d) + "″"

    fun trim(d: Double): String =
        if (abs(d - d.roundToInt()) < 0.05) d.roundToInt().toString() else one(d)

    fun len(inches: Double, u: Units): String = one(if (u == Units.Inches) inches else inches * 2.54)

    fun area(in2: Double, u: Units): String =
        String.format(us, "%,.0f", if (u == Units.Inches) in2 else in2 * 6.4516)

    fun areaUnit(u: Units): String = if (u == Units.Inches) "in²" else "cm²"

    /** +7.8, −1.2, ±0.0 (a true minus sign, so the column lines up). */
    fun signed(x: Double): String = when {
        x >= 0.05 -> "+" + one(x)
        x <= -0.05 -> "−" + one(-x)
        else -> "±0.0"
    }

    fun times(r: Double): String = String.format(us, "%.2f", r)

    /** A named shape's label when the pixels match it within 0.5 %, else "2.39:1". */
    fun pixelRatio(long: Int, short: Int): String {
        val r = long.toDouble() / short
        Aspect.named.firstOrNull { abs(it.ratio - r) / it.ratio < 0.005 }?.let { return it.label }
        return String.format(us, "%.2f:1", r)
    }

    fun res(w: Int, h: Int): String = String.format(us, "%d × %d", w, h)
}
