package io.github.whatwhatwhoa.powermeter

import android.os.BatteryManager
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Every number the app shows goes through here, so units and rounding stay the same. */
object Format {
    private fun f(x: Double, decimals: Int) = String.format(Locale.getDefault(), "%.${decimals}f", x)

    /** "4.21 W" under 10 W, "18.4 W" above; unsigned. */
    fun watts(mw: Double): String {
        val w = abs(mw) / 1000
        return if (w < 10) "${f(w, 2)} W" else "${f(w, 1)} W"
    }

    fun signedWatts(mw: Double): String = (if (mw < 0) "−" else "+") + watts(mw)

    fun milliamps(ua: Long): String = "${abs(ua / 1000)} mA"

    fun volts(mv: Int): String = if (mv > 0) "${f(mv / 1000.0, 3)} V" else "—"

    fun mah(uah: Long): String = if (uah > 0) "%,d mAh".format(uah / 1000) else "—"

    fun wh(mwh: Double): String = "${f(abs(mwh) / 1000, if (abs(mwh) < 10_000) 2 else 1)} Wh"

    fun temp(deciC: Int): String = when {
        deciC == Int.MIN_VALUE -> "—"
        Prefs.fahrenheit -> "${f(deciC / 10.0 * 9 / 5 + 32, 1)} °F"
        else -> "${f(deciC / 10.0, 1)} °C"
    }

    fun duration(ms: Long): String {
        val m = (ms / 60_000.0).roundToLong()
        return when {
            m < 1 -> "<1 m"
            m < 60 -> "$m m"
            m < 48 * 60 -> "${m / 60} h ${"%02d".format(m % 60)} m"
            else -> "${m / 1440} d ${(m % 1440) / 60} h"
        }
    }

    fun clock(t: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t))

    fun dayClock(t: Long): String {
        val today = System.currentTimeMillis() / 86_400_000L == t / 86_400_000L
        return if (today) clock(t)
        else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(t))
    }

    fun direction(r: Reading): String = when {
        r.status == BatteryManager.BATTERY_STATUS_FULL -> "Full"
        r.currentUa > 0 -> "Charging"
        r.onPower -> "Plugged in, still draining"
        else -> "Discharging"
    }

    fun plug(plugged: Int): String = when (plugged) {
        0 -> "Battery"
        BatteryManager.BATTERY_PLUGGED_AC -> "Charger"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
        else -> "Plugged in"
    }

    fun health(h: Int): String = when (h) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    }

    /** "first seen 1:02 AM" or "changed 11/1 12:20 AM", plus a staleness note. */
    fun chargerSince(r: Reading): String {
        if (r.chargerSince <= 0) return ""
        val what = if (r.chargerSinceExact) "reported" else "first seen"
        return "$what ${dayClock(r.chargerSince)}" +
            if (r.chargerStale) ", before this plug-in: may be an earlier charger's" else ""
    }

    /** Notification title: "Charging 18.4 W". */
    fun headline(r: Reading): String =
        if (r.voltageMv == 0) direction(r) else "${direction(r)} ${watts(r.powerMw)}"

    fun noteLine(r: Reading): String {
        val cable = if (r.cableMw.isNaN()) "" else " · cable ≈ ${watts(r.cableMw)}"
        return "${r.level}%$cable · ${milliamps(r.currentUa)} · ${volts(r.voltageMv)} · ${temp(r.tempDeciC)}"
    }

    /**
     * "Full in about 40 m" or "About 6 h 10 m left at this rate", from Android's own estimate
     * when charging, else from the charge counter and the last minute's average current.
     */
    fun estimate(r: Reading, avgUa: Long): String? {
        if (r.status == BatteryManager.BATTERY_STATUS_FULL) return null
        if (r.onPower && avgUa > 0) {
            if (r.fullInMs > 0) return "Full in about ${duration(r.fullInMs)}"
            val cap = r.capacityUah
            if (cap > 0) return "Full in about ${duration((cap - r.chargeUah) * 3_600_000L / avgUa)}"
            return null
        }
        if (!r.onPower && avgUa < 0 && r.chargeUah > 0) {
            return "About ${duration(r.chargeUah * 3_600_000L / -avgUa)} left at this rate"
        }
        return null
    }

    /** A number as typed: "80", "4.5"; at most two decimals, no trailing zeros. */
    fun plain(x: Double): String =
        String.format(Locale.getDefault(), "%.2f", x).trimEnd('0').trimEnd('.', ',')

    fun cToF(c: Double) = c * 9 / 5 + 32
    fun fToC(f: Double) = (f - 32) * 5 / 9

    /** An alert's value with its unit: "80%", "5 W", "40 °C" (or °F when set). */
    fun alertValue(a: Alert): String = when (a.watch) {
        Watch.LEVEL -> "${plain(a.value)}%"
        Watch.CHARGING, Watch.DRAIN -> "${plain(a.value)} W"
        Watch.TEMP -> if (Prefs.fahrenheit) "${plain(Math.round(cToF(a.value) * 10) / 10.0)} °F"
        else "${plain(a.value)} °C"
    }

    /** "45 s", "2 min", "1 h 30 min". */
    fun alertFor(ms: Long): String {
        val s = ms / 1000
        return when {
            s < 60 || s % 60 != 0L -> "$s s"
            s < 3600 -> "${s / 60} min"
            s % 3600 == 0L -> "${s / 3600} h"
            else -> "${s / 3600} h ${(s % 3600) / 60} min"
        }
    }

    /** One line for an alert: "Charging ≤ 5 W for 2 min", "Level ≥ 80% while plugged in". */
    fun alert(a: Alert): String {
        val what = when (a.watch) {
            Watch.LEVEL -> "Level"
            Watch.CHARGING -> "Charging"
            Watch.DRAIN -> "Drain"
            Watch.TEMP -> "Temperature"
        }
        val op = if (a.above) "≥" else "≤"
        val time = if (a.forMs > 0) " for ${alertFor(a.forMs)}" else ""
        val state = if (a.isPower) "" else when (a.during) {
            During.ANY -> ""
            During.PLUGGED -> " while plugged in"
            During.BATTERY -> " on battery"
        }
        return "$what $op ${alertValue(a)}$time$state"
    }

    /** The alert notification's title, with the figure that set it off. */
    fun alertTitle(a: Alert, r: Reading, v: Double): String = when (a.watch) {
        Watch.LEVEL -> "Battery at ${r.level}%"
        Watch.CHARGING ->
            if (v < 0) "Plugged in, still draining ${watts(v * 1000)}" else "Charging at ${watts(v * 1000)}"
        Watch.DRAIN -> "Draining ${watts(v * 1000)}"
        Watch.TEMP -> "Battery at ${temp(r.tempDeciC)}"
    }
}
