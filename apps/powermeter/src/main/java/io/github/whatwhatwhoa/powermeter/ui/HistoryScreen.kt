package io.github.whatwhatwhoa.powermeter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import io.github.whatwhatwhoa.stack.Seg
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.powermeter.Format
import io.github.whatwhatwhoa.powermeter.KIND_CHARGE
import io.github.whatwhatwhoa.powermeter.Row as Sample
import io.github.whatwhatwhoa.powermeter.ScreenOff
import io.github.whatwhatwhoa.powermeter.Session
import io.github.whatwhatwhoa.powermeter.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val ranges = listOf("1 h" to 1L, "6 h" to 6L, "24 h" to 24L, "7 d" to 168L)

private class History(
    val from: Long,
    val to: Long,
    val rows: List<Sample>,
    val sessions: List<Session>,
    val screenOffs: List<ScreenOff>,
)

@Composable
fun HistoryScreen(wide: Boolean, monitoring: Boolean, onTurnOn: () -> Unit) {
    var range by rememberSaveable { mutableIntStateOf(1) }
    var data by remember { mutableStateOf<History?>(null) }

    LaunchedEffect(range) {
        while (true) {
            val now = System.currentTimeMillis()
            val from = now - ranges[range].second * 3_600_000L
            data = withContext(Dispatchers.IO) {
                val store = Store.get()
                History(from, now, store.rows(from, 400), store.sessionsSince(from),
                    store.screenOffs(from, 50))
            }
            delay(30_000)
        }
    }

    val d = data
    val chips = @Composable {
        // The Stack period control: one segmented strip.
        Seg(ranges.indices.toList(), range, { range = it }, { ranges[it].first })
    }
    val charts = @Composable {
        if (d != null) {
            // A gap of three buckets or more is time the meter wasn't running.
            val gap = maxOf(30_000L, (d.to - d.from) / 400 * 3)
            val times = LongArray(d.rows.size) { d.rows[it].time }
            Panel(title = "Power") {
                Chart(times, FloatArray(d.rows.size) { d.rows[it].mw }, d.from, d.to, true, gap)
            }
            Panel(title = "Battery level") {
                Chart(times, FloatArray(d.rows.size) { d.rows[it].level.toFloat() }, d.from, d.to,
                    false, gap, height = 120.dp)
            }
        }
    }
    val lists = @Composable {
        if (d != null) {
            Summary(d)
            SessionList(d.sessions)
            ScreenOffList(d.screenOffs)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!monitoring) MonitoringOff(onTurnOn)
        chips()
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { charts() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { lists() }
            }
        } else {
            charts(); lists()
        }
    }
}

@Composable
private fun Summary(d: History) {
    val charged = d.sessions.filter { it.kind == KIND_CHARGE }.sumOf { it.energyMwh }
    val used = d.sessions.filter { it.kind != KIND_CHARGE }.sumOf { it.energyMwh }
    val standby = d.screenOffs.filter { !it.plugged }
    val hours = standby.sumOf { it.hours }
    val drop = standby.sumOf { it.startLevel - it.endLevel }
    Panel(title = "Sessions in this range") {
        Line("Charged", Format.wh(charged))
        Line("Used on battery", Format.wh(used))
        Line(
            "Screen-off drain",
            if (hours >= 0.25) "${"%.1f".format(drop / hours)} %/h over ${Format.duration((hours * 3_600_000).toLong())}"
            else "—"
        )
    }
}

@Composable
private fun SessionList(sessions: List<Session>) {
    if (sessions.isEmpty()) return
    Panel(title = "Sessions") {
        sessions.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider()
            val charge = s.kind == KIND_CHARGE
            val ms = s.end - s.start
            val title = (if (charge) "Charge" else "On battery") + " · " + Format.dayClock(s.start) +
                " – " + (if (s.open) "now" else Format.clock(s.end))
            val parts = buildList {
                add("${s.startLevel}% → ${s.endLevel}%")
                add(Format.wh(s.energyMwh) + if (charge) " in" else " used")
                if (ms > 60_000) add("avg " + Format.watts(s.energyMwh * 3_600_000 / ms))
                if (charge) add("peak " + Format.watts(s.peakMw))
                add(Format.duration(ms))
                if (!charge && s.screenOffMs > 60_000) add(Format.duration(s.screenOffMs) + " screen off")
            }
            Entry(title, parts.joinToString(" · "))
        }
    }
}

@Composable
private fun ScreenOffList(items: List<ScreenOff>) {
    if (items.isEmpty()) return
    Panel(title = "Screen off") {
        items.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider()
            val title = "${Format.dayClock(s.start)} – ${Format.clock(s.end)} · " +
                Format.duration(s.end - s.start)
            val body = if (s.plugged) "On power · ${s.startLevel}% → ${s.endLevel}%"
            else buildList {
                add("${s.endLevel - s.startLevel}%")
                add("${"%.1f".format(s.pctPerHour)} %/h")
                if (!s.avgMa.isNaN()) add("${s.avgMa.toInt()} mA average")
            }.joinToString(" · ")
            Entry(title, body)
        }
    }
}

@Composable
private fun Entry(title: String, body: String) {
    Column(Modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium.merge(Numbers))
        Text(body, style = MaterialTheme.typography.bodySmall.merge(Numbers),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
