package io.github.whatwhatwhoa.powermeter

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** What an alert watches. Power alerts carry their own state: charging = plugged in. */
enum class Watch { LEVEL, CHARGING, DRAIN, TEMP }

/** When a level or temperature alert counts. */
enum class During { ANY, PLUGGED, BATTERY }

/**
 * One alert: [watch] at or above (or at or below) [value] for at least [forMs].
 * [value] is in % for the level, W for power, °C for temperature (shown in °F when set).
 */
data class Alert(
    val id: Int,
    val on: Boolean,
    val watch: Watch,
    val above: Boolean,
    val value: Double,
    val forMs: Long,
    val during: During,
) {
    val isPower: Boolean get() = watch == Watch.CHARGING || watch == Watch.DRAIN
}

/**
 * Checks the alerts against each reading and posts a notification when one is breached for its
 * whole duration. Runs inside background monitoring, so like it there is no wake lock: readings
 * pause while the device sleeps, and the battery's own broadcasts still bring level changes.
 */
@SuppressLint("StaticFieldLeak") // the application context, which lives as long as the process
object Alerts {
    private const val CHANNEL = "alerts"
    private const val NOTE_BASE = 100

    /** The gauge's averaged current lags a plug change; power doesn't count for this long. */
    private const val SETTLE_MS = 60_000L

    /** Longer than this between readings, the device slept: a power alert's clock restarts. */
    private const val GAP_MS = 30_000L

    /** A fired alert fires again only after its condition has been off this long in a row. */
    const val REARM_MS = 60_000L

    private lateinit var app: Context
    val list = MutableStateFlow<List<Alert>>(emptyList())

    /** When each alert last fired, for the settings screen; this run of the app only. */
    val firedAt = MutableStateFlow<Map<Int, Long>>(emptyMap())

    private class State {
        var since = 0L    // the condition holds since; 0 = it doesn't
        var offSince = 0L // the condition is off since; 0 = it holds
        var fired = false
        var last = 0L     // the last reading this alert saw
    }

    private val states = HashMap<Int, State>()
    private var lastTime = 0L
    private var lastPlugged = -1
    private var plugChangedAt = 0L

    fun init(context: Context) {
        app = context.applicationContext
        list.value = decode(Prefs.alerts)
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "The alerts you set in Settings: level, power and temperature"
            }
        )
    }

    fun nextId(): Int = (list.value.maxOfOrNull { it.id } ?: 0) + 1

    /** Replace the list; an alert that is new or changed starts over (not fired, no clock). */
    @Synchronized
    fun save(alerts: List<Alert>) {
        val old = list.value.associateBy { it.id }
        for (a in alerts) if (old[a.id] != a) states.remove(a.id)
        states.keys.retainAll(alerts.map { it.id }.toSet())
        list.value = alerts
        Prefs.alerts = encode(alerts)
    }

    @Synchronized
    fun check(r: Reading) {
        // Readings come from the sampler and from battery broadcasts; skip one that arrives late.
        if (r.time < lastTime) return
        lastTime = r.time
        if (r.plugged != lastPlugged) {
            if (lastPlugged != -1) plugChangedAt = r.time
            lastPlugged = r.plugged
        }
        val settled = r.time - plugChangedAt >= SETTLE_MS
        for (a in list.value) {
            if (!a.on) continue
            val s = states.getOrPut(a.id) { State() }
            val gap = s.last > 0 && r.time - s.last > GAP_MS
            s.last = r.time
            val v = valueOf(a, r, settled)
            val hit = v != null && if (a.above) v >= a.value else v <= a.value
            if (hit) {
                s.offSince = 0
                // Power isn't steady through a sleep, so after a gap its clock starts again;
                // level and temperature change slowly enough to bridge it.
                if (s.since == 0L || (gap && a.isPower)) s.since = r.time
                if (!s.fired && r.time - s.since >= a.forMs) {
                    s.fired = true
                    notify(a, r, v!!)
                }
            } else {
                s.since = 0
                if (s.offSince == 0L) s.offSince = r.time
                if (s.fired && r.time - s.offSince >= REARM_MS) s.fired = false
            }
        }
    }

    /** The figure an alert watches, in its own unit; null while the alert doesn't apply. */
    private fun valueOf(a: Alert, r: Reading, settled: Boolean): Double? = when (a.watch) {
        Watch.LEVEL -> if (applies(a.during, r)) r.level.toDouble() else null
        Watch.TEMP ->
            if (r.tempDeciC != Int.MIN_VALUE && applies(a.during, r)) r.tempDeciC / 10.0 else null
        // Full, or held by a charge limit or the heat: not charging slowly, so it doesn't count.
        Watch.CHARGING ->
            if (r.onPower && settled && r.voltageMv > 0 &&
                r.status != BatteryManager.BATTERY_STATUS_FULL &&
                r.status != BatteryManager.BATTERY_STATUS_NOT_CHARGING
            ) r.powerMw / 1000 else null
        Watch.DRAIN -> if (!r.onPower && settled && r.voltageMv > 0) -r.powerMw / 1000 else null
    }

    private fun applies(d: During, r: Reading) = when (d) {
        During.ANY -> true
        During.PLUGGED -> r.onPower
        During.BATTERY -> !r.onPower
    }

    private fun notify(a: Alert, r: Reading, v: Double) {
        firedAt.value = firedAt.value + (a.id to r.time)
        val open = PendingIntent.getActivity(
            app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val note = Notification.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(Format.alertTitle(a, r, v))
            .setContentText("Alert: ${Format.alert(a)}")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            app.getSystemService(NotificationManager::class.java).notify(NOTE_BASE + a.id, note)
        } catch (_: SecurityException) {
            // Notifications not allowed; the settings screen says so.
        }
    }

    private fun encode(alerts: List<Alert>): String = JSONArray().apply {
        for (a in alerts) put(
            JSONObject()
                .put("id", a.id).put("on", a.on).put("watch", a.watch.name)
                .put("above", a.above).put("value", a.value).put("forMs", a.forMs)
                .put("during", a.during.name)
        )
    }.toString()

    private fun decode(s: String?): List<Alert> {
        if (s.isNullOrEmpty()) return emptyList()
        val arr = runCatching { JSONArray(s) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                Alert(
                    id = o.getInt("id"),
                    on = o.getBoolean("on"),
                    watch = Watch.valueOf(o.getString("watch")),
                    above = o.getBoolean("above"),
                    value = o.getDouble("value"),
                    forMs = o.getLong("forMs"),
                    during = During.valueOf(o.getString("during")),
                )
            }.getOrNull()
        }
    }
}
