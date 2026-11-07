package io.github.whatwhatwhoa.displaycompare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.displaycompare.Align
import io.github.whatwhatwhoa.displaycompare.CompareState
import io.github.whatwhatwhoa.displaycompare.Display
import io.github.whatwhatwhoa.displaycompare.Fmt
import io.github.whatwhatwhoa.displaycompare.Units
import io.github.whatwhatwhoa.stack.BlockGap
import io.github.whatwhatwhoa.stack.FLabel
import io.github.whatwhatwhoa.stack.Notes
import io.github.whatwhatwhoa.stack.Seg
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackCard
import io.github.whatwhatwhoa.stack.StackHeader
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.stack.Swatch
import io.github.whatwhatwhoa.stack.Tile
import io.github.whatwhatwhoa.stack.TileGrid
import io.github.whatwhatwhoa.stack.stackColumn
import kotlin.math.roundToInt

/**
 * Phones: the header and the mat stay put while the rest scrolls under them, so a change shows
 * on the mat as it is typed. From 600 dp: the mat and the comparison on the left, the editors
 * and the table on the right.
 */
@Composable
fun CompareScreen(state: CompareState) {
    val c = Stack.colors
    val n = state.displays.size
    BoxWithConstraints(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val wide = maxWidth >= 600.dp
        val roomy = maxWidth >= 900.dp
        val header = @Composable {
            StackHeader("Display Compare", count = if (n == 1) "1 display" else "$n displays")
        }
        if (!wide) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.stackColumn().padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    header()
                    DisplayMat(state.displays, state.units, state.align, maxHeight = 250.dp)
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.stackColumn().padding(top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(BlockGap)) {
                        Controls(state)
                        Comparison(state, columns = 2)
                        Editors(state)
                        Table(state)
                        About()
                    }
                }
            }
        } else {
            Column(Modifier.stackColumn().fillMaxSize()) {
                header()
                Spacer(Modifier.height(10.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier.weight(1.1f).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(BlockGap),
                    ) {
                        DisplayMat(state.displays, state.units, state.align, maxHeight = 440.dp)
                        Controls(state)
                        Comparison(state, columns = if (roomy) 4 else 2)
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(BlockGap),
                    ) {
                        Editors(state)
                        Table(state)
                        About()
                    }
                }
            }
        }
    }
}

@Composable
private fun Controls(state: CompareState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Labeled("Units") {
            Seg(Units.entries, state.units, state::chooseUnits, { it.label })
        }
        Labeled("Line up") {
            Seg(Align.entries, state.align, state::chooseAlign, { it.label })
        }
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FLabel(label)
        content()
    }
}

/** Four tiles: one display against another (B with A unless chosen). */
@Composable
private fun Comparison(state: CompareState, columns: Int) {
    val c = Stack.colors
    val o = state.bySlot(state.other)
    val r = state.bySlot(state.ref)
    if (o == null || r == null || o.slot == r.slot) {
        StackCard(title = "Compare") { Notes(listOf("Add a second display to compare two.")) }
        return
    }
    val u = state.units
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.displays.size > 2) {
            val slots = state.displays.map { it.slot }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically) {
                FLabel("Compare")
                Seg(slots, o.slot, { state.setPair(it, if (it == r.slot) o.slot else r.slot) }, { "${'A' + it}" })
                Text("with", style = StackType.note, color = c.muted)
                Seg(slots, r.slot, { state.setPair(if (it == o.slot) r.slot else o.slot, it) }, { "${'A' + it}" })
            }
        }
        val ol = o.letter
        val rl = r.letter
        val tiles = listOf<@Composable (Modifier) -> Unit>(
            { m ->
                Tile("Area", Fmt.times(o.areaIn2 / r.areaIn2), m, unit = "×",
                    info = "$ol's picture area ÷ $rl's. Area is the picture's width × height; bezels aren't counted.",
                    sub = "$ol ${Fmt.area(o.areaIn2, u)} · $rl ${Fmt.area(r.areaIn2, u)} ${Fmt.areaUnit(u)}",
                    phone = columns <= 2)
            },
            { m ->
                Tile("Width", Fmt.signed(len(o.widthIn - r.widthIn, u)), m, unit = u.len,
                    info = "How much wider $ol's picture is than $rl's, as each stands (portrait or landscape).",
                    sub = "$ol ${Fmt.len(o.widthIn, u)} · $rl ${Fmt.len(r.widthIn, u)} ${u.len}", phone = columns <= 2)
            },
            { m ->
                Tile("Height", Fmt.signed(len(o.heightIn - r.heightIn, u)), m, unit = u.len,
                    info = "How much taller $ol's picture is than $rl's, as each stands (portrait or landscape).",
                    sub = "$ol ${Fmt.len(o.heightIn, u)} · $rl ${Fmt.len(r.heightIn, u)} ${u.len}", phone = columns <= 2)
            },
            { m ->
                val op = o.ppi
                val rp = r.ppi
                val info = "Pixels per inch along the diagonal: √(width² + height²) in pixels ÷ the diagonal. " +
                    "Higher is sharper at the same distance."
                if (op != null && rp != null) {
                    val diff = op.roundToInt() - rp.roundToInt()
                    Tile("Pixel density", (if (diff > 0) "+" else if (diff < 0) "−" else "±") + kotlin.math.abs(diff),
                        m, unit = "ppi", info = info, sub = "$ol ${op.roundToInt()} · $rl ${rp.roundToInt()} ppi",
                        phone = columns <= 2)
                } else {
                    Tile("Pixel density", "—", m, info = info, sub = "Needs both resolutions", phone = columns <= 2)
                }
            },
        )
        TileGrid(columns, tiles)
    }
}

private fun len(inches: Double, u: Units) = if (u == Units.Inches) inches else inches * 2.54

@Composable
private fun ColumnScope.Editors(state: CompareState) {
    state.displays.forEach { d ->
        DisplayCard(
            d = d,
            units = state.units,
            canRemove = state.displays.size > 1,
            onChange = { change -> state.update(d.slot, change) },
            onPreset = { state.usePreset(d.slot, it) },
            onThisPhone = { state.useThisPhone(d.slot) },
            onRemove = { state.remove(d.slot) },
        )
    }
    if (state.canAdd) {
        StackButton("Add display", onClick = state::add, primary = true)
    }
}

/** Every display side by side: one column each, a row per measure. */
@Composable
private fun Table(state: CompareState) {
    val c = Stack.colors
    val u = state.units
    val ds = state.displays
    val ref = state.bySlot(state.ref) ?: ds.first()
    val rows: List<Pair<String, (Display) -> String>> = listOf(
        "Diagonal" to { d -> if (u == Units.Inches) Fmt.diag(d.diagonal) else "${Fmt.one(d.diagonal * 2.54)} cm" },
        "Width" to { d -> "${Fmt.len(d.widthIn, u)} ${u.len}" },
        "Height" to { d -> "${Fmt.len(d.heightIn, u)} ${u.len}" },
        "Area" to { d -> "${Fmt.area(d.areaIn2, u)} ${Fmt.areaUnit(u)}" },
        "Area vs ${ref.letter}" to { d -> "${Fmt.times(d.areaIn2 / ref.areaIn2)}×" },
        "Shape" to { d -> d.shapeLabel + if (d.portrait) " ↑" else "" },
        "Pixels" to { d -> if (d.hasRes) Fmt.res(d.resW!!, d.resH!!) else "—" },
        "Density" to { d -> d.ppi?.let { "${it.roundToInt()} ppi" } ?: "—" },
    )
    val labelW = 96.dp
    val colW = 104.dp
    StackCard(title = "All displays",
        info = "Each display's picture: bezels aren't counted. ↑ marks one turned portrait.") {
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Column {
                Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(labelW))
                    ds.forEach { d ->
                        Row(Modifier.width(colW), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically) {
                            Swatch(c.series[d.slot % c.series.size])
                            Text(d.letter, style = StackType.th, color = c.muted)
                        }
                    }
                }
                HorizontalDivider(color = c.line)
                rows.forEach { (label, value) ->
                    Row(Modifier.padding(vertical = 7.dp)) {
                        Text(label.uppercase(), Modifier.width(labelW), style = StackType.th, color = c.muted)
                        ds.forEach { d ->
                            Text(value(d), Modifier.width(colW), style = StackType.table, color = c.ink,
                                textAlign = TextAlign.End, maxLines = 1)
                        }
                    }
                    HorizontalDivider(color = c.line)
                }
            }
        }
    }
}

@Composable
private fun About() {
    StackCard(title = "How it measures") {
        Notes(
            listOf(
                "Sizes are of the picture; bezels and stands aren't counted.",
                "Shape: from the pixels when a resolution is entered (pixels are square), else the shape picked.",
                "21:9 is a name, not a ratio: 3440 × 1440 is 2.39:1 and 2560 × 1080 is 2.37:1.",
                "Monitor and TV presets use the viewable diagonal (a 65″ class TV is 64.5″); phones, tablets and laptops the maker's figure.",
                "Phones are drawn upright; turn any display with Turn portrait / Turn landscape.",
                "This phone: pixels ÷ dpi as Android reports them; makers quote ~0.1″ more.",
                "Curved screens are drawn flat.",
                "Saved on this phone only.",
            )
        )
    }
}
