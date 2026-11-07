package io.github.whatwhatwhoa.powermeter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.powermeter.Format
import io.github.whatwhatwhoa.powermeter.KIND_CHARGE
import io.github.whatwhatwhoa.powermeter.Monitor
import io.github.whatwhatwhoa.powermeter.Point
import io.github.whatwhatwhoa.powermeter.Reading
import io.github.whatwhatwhoa.powermeter.Session
import io.github.whatwhatwhoa.powermeter.Tracker
import io.github.whatwhatwhoa.stack.StackButton

@Composable
fun NowScreen(wide: Boolean, monitoring: Boolean, onTurnOn: () -> Unit) {
    val r by Monitor.live.collectAsState()
    val points by Monitor.recent.collectAsState()
    val session by Tracker.current.collectAsState()
    val reading = r ?: return

    val hero = @Composable { Hero(reading, points) }
    val chart = @Composable { LiveChart(points) }
    val details = @Composable { Details(reading) }
    val log = @Composable {
        val s = session
        if (s != null) SessionPanel(s) else if (!monitoring) MonitoringOff(onTurnOn)
    }

    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                hero(); chart()
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                details(); log()
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            hero(); chart(); log(); details()
        }
    }
}

@Composable
private fun Hero(r: Reading, points: List<Point>) {
    val flow = LocalFlow.current
    val color = flowColor(r.powerMw, flow, MaterialTheme.colorScheme.onSurface)
    // The last minute's average current: steadier than one reading for a time estimate.
    val since = r.time - 60_000
    val minute = points.filter { it.time >= since }
    val avgUa = if (minute.isEmpty()) r.currentUa else minute.sumOf { it.ua } / minute.size

    Panel {
        Text(
            Format.direction(r) + " · " + Format.plug(r.plugged),
            style = MaterialTheme.typography.titleMedium, color = color
        )
        Text(
            if (r.voltageMv > 0) Format.watts(r.powerMw) else Format.milliamps(r.currentUa),
            style = MaterialTheme.typography.displayLarge.merge(Numbers), color = color
        )
        Text(
            "${Format.milliamps(r.currentUa)} · ${Format.volts(r.voltageMv)} · ${r.level}%",
            style = MaterialTheme.typography.bodyLarge.merge(Numbers)
        )
        if (r.onPower) Cable(r)
        Format.estimate(r, avgUa)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The cable's share, which Android doesn't report: see [Reading.cableMw]. */
@Composable
private fun Cable(r: Reading) {
    val max = when {
        r.chargerMaxMw <= 0 -> ""
        r.chargerStale -> " · charger max ${Format.watts(r.chargerMaxMw)} (may be stale)"
        else -> " · charger max ${Format.watts(r.chargerMaxMw)}"
    }
    val cable = r.cableMw
    Text(
        if (cable.isNaN()) "From the cable: not known yet$max"
        else "From the cable ≈ ${Format.watts(cable)}$max",
        style = MaterialTheme.typography.bodyLarge.merge(Numbers)
    )
    val battery = Format.watts(r.powerMw)
    val own = Format.watts(r.ownUseMw)
    Text(
        when {
            cable.isNaN() -> "The big number is what goes into the battery. To estimate the " +
                "cable, the meter first learns what this device uses by itself: leave it on " +
                "battery with the app open for a few minutes."
            r.powerMw >= 0 -> "Estimate: $battery into the battery + $own the device uses by " +
                "itself (learned on battery) + about 10% charging losses."
            // Plugged in but draining: the cable isn't covering the device's own use.
            -r.powerMw >= r.ownUseMw -> "The battery is supplying $battery, at least what this " +
                "device uses by itself (about $own on battery), so the cable is giving little " +
                "or nothing."
            else -> "Estimate: the device uses about $own by itself (learned on battery); the " +
                "battery covers $battery of it and the cable the rest, plus about 10% losses."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LiveChart(points: List<Point>) {
    val now = points.lastOrNull()?.time ?: System.currentTimeMillis()
    val values = FloatArray(points.size) { points[it].mw }
    Panel(title = "Last 10 minutes") {
        Chart(
            LongArray(points.size) { points[it].time }, values,
            now - Monitor.WINDOW_MS, now, signed = true, gapMs = 30_000
        )
        spread(values)?.let { (lo, avg, hi) ->
            Text(
                "Min ${Format.signedWatts(lo)} · Avg ${Format.signedWatts(avg)} · Max ${Format.signedWatts(hi)}",
                style = MaterialTheme.typography.bodySmall.merge(Numbers),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Details(r: Reading) {
    val items = buildList {
        add("Level" to "${r.level}%")
        add("Current" to Format.milliamps(r.currentUa))
        add("Voltage" to Format.volts(r.voltageMv))
        add("Temperature" to Format.temp(r.tempDeciC))
        add("Source" to Format.plug(r.plugged))
        if (r.chargerMaxMw > 0) {
            add("Charger max" to Format.watts(r.chargerMaxMw))
            Format.chargerSince(r).takeIf { it.isNotEmpty() }?.let { add("" to it) }
        }
        if (!r.ownUseMw.isNaN()) {
            add("Own use, screen ${if (r.screenOn) "on" else "off"}" to Format.watts(r.ownUseMw))
        }
        add("Health" to Format.health(r.health))
        add("Charge left" to Format.mah(r.chargeUah))
        add("Full capacity (est.)" to Format.mah(r.capacityUah))
        if (r.cycles >= 0) add("Charge cycles" to r.cycles.toString())
        r.technology?.takeIf { it.isNotBlank() }?.let { add("Chemistry" to it) }
    }
    Panel(title = "Battery") {
        items.forEach { (label, value) ->
            // An unlabeled item is a note on the line above it.
            if (label.isEmpty()) Text(value, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Line(label, value)
        }
    }
}

@Composable
private fun SessionPanel(s: Session) {
    val charge = s.kind == KIND_CHARGE
    val ms = s.end - s.start
    val change = s.endLevel - s.startLevel
    Panel(title = if (charge) "This charge" else "On battery") {
        Line("Since", Format.dayClock(s.start))
        Line("Duration", Format.duration(ms))
        Line("Level", "${s.startLevel}% → ${s.endLevel}% (${if (change >= 0) "+" else ""}$change%)")
        Line(if (charge) "Energy in" else "Energy used", Format.wh(s.energyMwh))
        if (ms > 60_000) Line("Average", Format.watts(s.energyMwh * 3_600_000 / ms))
        Line("Peak", Format.watts(s.peakMw))
        if (!charge) Line("Screen off", Format.duration(s.screenOffMs))
    }
}

@Composable
fun MonitoringOff(onTurnOn: () -> Unit) {
    Panel(title = "Background monitoring is off") {
        Text(
            "Turn it on to keep a history, charge sessions and screen-off drain. " +
                "It shows a quiet notification with the live reading.",
            style = MaterialTheme.typography.bodyMedium
        )
        StackButton("Turn on", onClick = onTurnOn, modifier = Modifier.padding(top = 4.dp), primary = true)
    }
}
