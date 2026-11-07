package io.github.whatwhatwhoa.displaycompare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.whatwhatwhoa.displaycompare.Aspect
import io.github.whatwhatwhoa.displaycompare.Display
import io.github.whatwhatwhoa.displaycompare.Fmt
import io.github.whatwhatwhoa.displaycompare.Preset
import io.github.whatwhatwhoa.displaycompare.Presets
import io.github.whatwhatwhoa.displaycompare.Units
import io.github.whatwhatwhoa.stack.FLabel
import io.github.whatwhatwhoa.stack.Pill
import io.github.whatwhatwhoa.stack.PillKind
import io.github.whatwhatwhoa.stack.Seg
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackCard
import io.github.whatwhatwhoa.stack.StackField
import io.github.whatwhatwhoa.stack.StackType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import io.github.whatwhatwhoa.displaycompare.Family
import io.github.whatwhatwhoa.stack.StackTabs
import kotlin.math.hypot

private const val MIN_DIAG = 1.0
private const val MAX_DIAG = 300.0

private fun parseDiag(s: String): Double? = s.replace(',', '.').toDoubleOrNull()?.takeIf { it in MIN_DIAG..MAX_DIAG }
private fun parsePx(s: String): Int? = s.toIntOrNull()?.takeIf { it in 1..30000 }

/** Half an inch at a time on small screens, an inch on big ones. */
private fun step(d: Double) = if (d < 15) 0.5 else 1.0

/** One display's editor: its stripe and letter in its series color, then size, shape, pixels. */
@Composable
fun DisplayCard(
    d: Display,
    units: Units,
    canRemove: Boolean,
    onChange: ((Display) -> Display) -> Unit,
    onPreset: (Preset) -> Unit,
    onThisPhone: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Stack.colors
    val color = c.series[d.slot % c.series.size]

    StackCard(modifier, stripe = color) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(d.letter, style = StackType.monoBody.copy(fontWeight = FontWeight.Medium), color = color)
            Text(d.title, Modifier.weight(1f), style = StackType.h2, color = c.ink, maxLines = 2)
            PresetButton(d, onPreset, onThisPhone, onCustom = { onChange { it.copy(name = null) } })
        }

        // Diagonal: typed, or nudged with − and +. The text keeps what is typed ("27.") until the
        // value changes from outside (a preset, a nudge).
        var diag by remember { mutableStateOf(Fmt.trim(d.diagonal)) }
        LaunchedEffect(d.diagonal) { if (parseDiag(diag) != d.diagonal) diag = Fmt.trim(d.diagonal) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FLabel("Diagonal")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StackButton("−", onClick = {
                    val v = max(MIN_DIAG, ((d.diagonal - step(d.diagonal)) * 10).roundToInt() / 10.0)
                    onChange { it.copy(diagonal = v, name = null) }
                }, modifier = Modifier.width(44.dp))
                StackField(diag, { t ->
                    diag = t
                    parseDiag(t)?.let { v -> onChange { it.copy(diagonal = v, name = null) } }
                }, Modifier.width(104.dp), suffix = "in", description = "Diagonal of ${d.letter} in inches")
                StackButton("+", onClick = {
                    val v = min(MAX_DIAG, ((d.diagonal + step(d.diagonal)) * 10).roundToInt() / 10.0)
                    onChange { it.copy(diagonal = v, name = null) }
                }, modifier = Modifier.width(44.dp))
                if (units == Units.Centimeters) {
                    Text("${Fmt.one(d.diagonal * 2.54)} cm", style = StackType.sub, color = c.muted)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FLabel("Shape")
            Seg(
                options = Aspect.entries,
                selected = d.aspect,
                onSelect = { a -> onChange { it.copy(aspect = a, name = null) } },
                label = { it.label },
                enabled = { it != Aspect.Pixels || d.hasRes },
            )
        }

        var w by remember { mutableStateOf(d.resW?.toString() ?: "") }
        var h by remember { mutableStateOf(d.resH?.toString() ?: "") }
        LaunchedEffect(d.resW, d.resH) {
            if (parsePx(w) != d.resW) w = d.resW?.toString() ?: ""
            if (parsePx(h) != d.resH) h = d.resH?.toString() ?: ""
        }
        // A full resolution sets the shape (pixels are square); clearing it falls back to the
        // nearest named shape.
        fun setRes(tw: String, th: String) {
            val pw = parsePx(tw)
            val ph = parsePx(th)
            onChange {
                if (pw != null && ph != null) it.copy(resW = pw, resH = ph, aspect = Aspect.Pixels, name = null)
                else it.copy(resW = null, resH = null, name = null,
                    aspect = if (it.aspect == Aspect.Pixels) Aspect.nearest(it.ratio) else it.aspect)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FLabel("Resolution (optional)")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StackField(w, { w = it; setRes(it, h) }, Modifier.width(96.dp), placeholder = "width",
                    keyboardType = KeyboardType.Number, description = "Resolution width of ${d.letter}")
                Text("×", style = StackType.body, color = c.muted)
                StackField(h, { h = it; setRes(w, it) }, Modifier.width(96.dp), placeholder = "height",
                    keyboardType = KeyboardType.Number, description = "Resolution height of ${d.letter}")
                Text("px", style = StackType.body, color = c.muted)
            }
        }

        val mismatch = d.pixelRatioMismatch
        if (mismatch != null || d.portrait) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (mismatch != null) Pill("Pixels are ${Fmt.pixelRatio(max(d.resW!!, d.resH!!), min(d.resW, d.resH))}", PillKind.Warn)
                if (d.portrait) Pill("Portrait", PillKind.Mute)
            }
        }

        Text(summary(d, units), style = StackType.mono.copy(fontSize = 12.5.sp), color = c.muted)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StackButton(if (d.portrait) "Turn landscape" else "Turn portrait",
                onClick = { onChange { it.copy(portrait = !it.portrait) } }, small = true)
            if (canRemove) StackButton("Remove ${d.letter}", onClick = onRemove, small = true)
        }
    }
}

/** "23.5 × 13.2 in · 311 in² · 109 ppi" */
fun summary(d: Display, u: Units): String {
    val parts = mutableListOf(
        "${Fmt.len(d.widthIn, u)} × ${Fmt.len(d.heightIn, u)} ${u.len}",
        "${Fmt.area(d.areaIn2, u)} ${Fmt.areaUnit(u)}",
    )
    d.ppi?.let { parts += "${it.roundToInt()} ppi" }
    return parts.joinToString(" · ")
}

/**
 * The Presets button and its sheet: a tab per family, opened on the family the display came
 * from. Custom keeps the numbers and drops the preset's name; every field stays editable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetButton(d: Display, onPreset: (Preset) -> Unit, onThisPhone: () -> Unit, onCustom: () -> Unit) {
    val c = Stack.colors
    var open by remember { mutableStateOf(false) }
    StackButton("Presets", onClick = { open = true }, small = true)
    if (!open) return
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var family by remember { mutableStateOf(Presets.familyOf(d) ?: Family.Monitors) }
    ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Presets for ${d.letter}", Modifier.weight(1f), style = StackType.h1, color = c.ink)
                StackButton("Custom", onClick = { open = false; onCustom() }, small = true)
            }
            Text("Custom keeps the numbers: type the diagonal, shape and pixels in the card.",
                style = StackType.note, color = c.muted)
            StackTabs(Family.entries.map { it.label }, family.ordinal, { family = Family.entries[it] })
            Column {
                if (family == Family.Phones) {
                    PresetRow("This phone", "Measured from this screen, as Android reports it",
                        on = d.name == "This phone") { open = false; onThisPhone() }
                }
                Presets.all.filter { it.family == family }.forEach { p ->
                    val w = if (p.portrait) min(p.resW, p.resH) else max(p.resW, p.resH)
                    val h = if (p.portrait) max(p.resW, p.resH) else min(p.resW, p.resH)
                    val ppi = hypot(p.resW.toDouble(), p.resH.toDouble()) / p.diagonal
                    PresetRow(p.name, "${Fmt.res(w, h)} · ${Fmt.pixelRatio(max(w, h), min(w, h))} · ${ppi.roundToInt()} ppi",
                        on = d.name == p.name) { open = false; onPreset(p) }
                }
            }
        }
    }
}

@Composable
private fun PresetRow(name: String, sub: String, on: Boolean, onClick: () -> Unit) {
    val c = Stack.colors
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 52.dp).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(name, style = StackType.body, color = if (on) c.accent else c.ink)
        Text(sub, style = StackType.mono, color = c.muted)
    }
    HorizontalDivider(color = c.line)
}
