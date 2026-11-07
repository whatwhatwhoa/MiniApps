package io.github.whatwhatwhoa.displaycompare

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * The displays being compared, the pair the tiles compare, units and alignment. Saved to
 * preferences on every change, so the app opens where it was left.
 */
class CompareState(private val context: Context) {
    private val prefs = context.getSharedPreferences("compare", Context.MODE_PRIVATE)

    val displays = mutableStateListOf<Display>()
    var units by mutableStateOf(Units.Inches)
        private set
    var align by mutableStateOf(Align.Bottom)
        private set
    /** The tiles show [other] against [ref] ("B with A"); both are slots. */
    var ref by mutableIntStateOf(0)
        private set
    var other by mutableIntStateOf(1)
        private set

    init {
        load()
    }

    fun bySlot(slot: Int): Display? = displays.firstOrNull { it.slot == slot }

    val canAdd: Boolean get() = displays.size < MAX

    fun update(slot: Int, change: (Display) -> Display) {
        val i = displays.indexOfFirst { it.slot == slot }
        if (i < 0) return
        displays[i] = change(displays[i])
        save()
    }

    fun usePreset(slot: Int, preset: Preset) = replace(preset.toDisplay(slot))

    fun useThisPhone(slot: Int) = replace(Presets.thisPhone(context, slot))

    private fun replace(d: Display) {
        val i = displays.indexOfFirst { it.slot == d.slot }
        if (i >= 0) displays[i] = d
        save()
    }

    /** Adds the first suggestion not on the mat yet, in the lowest free slot (its color). */
    fun add() {
        if (!canAdd) return
        val slot = (0 until MAX).first { s -> displays.none { it.slot == s } }
        val names = displays.mapNotNull { it.name }.toSet()
        val pick = SUGGESTIONS.firstOrNull { it !in names }
        val d = when (pick) {
            null -> displays.last().copy(slot = slot, diagonal = displays.last().diagonal + 2, name = null)
            THIS_PHONE -> Presets.thisPhone(context, slot)
            else -> Presets.named(pick).toDisplay(slot)
        }
        val at = displays.indexOfFirst { it.slot > slot }.let { if (it < 0) displays.size else it }
        displays.add(at, d)
        if (displays.size == 2) other = slot
        fixPair()
        save()
    }

    fun remove(slot: Int) {
        if (displays.size <= 1) return
        displays.removeAll { it.slot == slot }
        fixPair()
        save()
    }

    fun chooseUnits(u: Units) { units = u; save() }
    fun chooseAlign(a: Align) { align = a; save() }
    fun setPair(newOther: Int, newRef: Int) {
        other = newOther
        ref = newRef
        fixPair()
        save()
    }

    private fun fixPair() {
        val slots = displays.map { it.slot }
        if (ref !in slots) ref = slots.first()
        if (other !in slots || other == ref) other = slots.firstOrNull { it != ref } ?: ref
    }

    private fun save() {
        val list = JSONArray()
        displays.forEach { d ->
            list.put(JSONObject().apply {
                put("slot", d.slot)
                put("d", d.diagonal)
                put("a", d.aspect.name)
                d.resW?.let { put("rw", it) }
                d.resH?.let { put("rh", it) }
                put("p", d.portrait)
                d.name?.let { put("n", it) }
            })
        }
        val json = JSONObject().apply {
            put("v", 1)
            put("units", units.name)
            put("align", align.name)
            put("ref", ref)
            put("other", other)
            put("displays", list)
        }
        prefs.edit().putString("state", json.toString()).apply()
    }

    private fun load() {
        val saved = runCatching { JSONObject(prefs.getString("state", null) ?: "") }.getOrNull()
        val list = saved?.optJSONArray("displays")
        if (saved == null || list == null || list.length() == 0) {
            displays += Presets.named("27″ 1440p").toDisplay(0)
            displays += Presets.named("34″ ultrawide").toDisplay(1)
            return
        }
        val read = mutableListOf<Display>()
        for (i in 0 until list.length()) {
            val o = list.getJSONObject(i)
            read += Display(
                slot = o.getInt("slot").coerceIn(0, MAX - 1),
                diagonal = o.getDouble("d"),
                aspect = runCatching { Aspect.valueOf(o.getString("a")) }.getOrDefault(Aspect.W16x9),
                resW = if (o.has("rw")) o.getInt("rw") else null,
                resH = if (o.has("rh")) o.getInt("rh") else null,
                portrait = o.optBoolean("p"),
                name = if (o.has("n")) o.getString("n") else null,
            )
        }
        displays += read.distinctBy { it.slot }.sortedBy { it.slot }
        units = runCatching { Units.valueOf(saved.getString("units")) }.getOrDefault(Units.Inches)
        align = runCatching { Align.valueOf(saved.getString("align")) }.getOrDefault(Align.Bottom)
        ref = saved.optInt("ref", 0)
        other = saved.optInt("other", 1)
        fixPair()
    }

    companion object {
        /** Four: the series colors s0–s3 stay told apart, and the table still fits a tablet. */
        const val MAX = 4
        const val THIS_PHONE = "This phone"
        private val SUGGESTIONS = listOf("27″ 1440p", "34″ ultrawide", "32″ class 4K", THIS_PHONE, "65″ class 4K TV")
    }
}
