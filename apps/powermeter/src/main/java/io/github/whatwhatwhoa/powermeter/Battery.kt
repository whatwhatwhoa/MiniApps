package io.github.whatwhatwhoa.powermeter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlin.math.abs
import kotlin.math.exp

/** A typical phone charging circuit passes on about 90% of what it takes in. */
const val CHARGE_EFFICIENCY = 0.9

/** One look at the battery. Current is signed: + flows into the battery, − out of it. */
data class Reading(
    val time: Long,
    val rawCurrent: Int,       // CURRENT_NOW exactly as the device reports it
    val currentUa: Long,       // normalized µA
    val voltageMv: Int,        // 0 when the device doesn't say
    val level: Int,            // 0..100
    val status: Int,           // BatteryManager.BATTERY_STATUS_*
    val plugged: Int,          // BatteryManager.BATTERY_PLUGGED_*, 0 = on battery
    val tempDeciC: Int,
    val health: Int,
    val chargeUah: Long,       // charge left in µAh, -1 when the gauge doesn't report it
    val cycles: Int,           // -1 when unknown (Android 14+ only)
    val technology: String?,
    val fullInMs: Long,        // Android's own time-to-full estimate, -1 when it has none
    val screenOn: Boolean,
    val chargerMaxMw: Double,  // the charger's negotiated maximum, 0 when unknown or unplugged
    val chargerSince: Long,    // when the app first saw this rating (see Battery.trackCharger)
    val chargerSinceExact: Boolean, // true: the app saw it change then; false: it was already there
    val chargerStale: Boolean, // the rating predates this plug-in: may be an earlier charger's
    val ownUseMw: Double,      // what the device itself draws in this screen state, NaN unknown
) {
    /** µA × mV = nW; / 10⁶ = mW. */
    val powerMw: Double get() = currentUa * voltageMv / 1_000_000.0
    val onPower: Boolean get() = plugged != 0

    /**
     * What the cable delivers, estimated: Android reports only the battery's share, so add what
     * the device draws by itself (learned on battery) and the charging circuit's losses. NaN when
     * unplugged or the device's own draw isn't learned yet.
     */
    val cableMw: Double
        get() = if (!onPower || ownUseMw.isNaN()) Double.NaN
        else ((powerMw + ownUseMw) / CHARGE_EFFICIENCY).coerceAtLeast(0.0)

    /** Full capacity estimated from the charge counter, in µAh; -1 when it can't be. */
    val capacityUah: Long get() = if (chargeUah > 0 && level >= 5) chargeUah * 100 / level else -1
}

/**
 * Reads the battery and turns the gauge's numbers into one convention. Devices disagree on two
 * things Android specifies: CURRENT_NOW should be µA, positive while charging, but many report mA
 * and many flip the sign. Auto mode learns both from what the device does, then remembers them.
 */
@SuppressLint("StaticFieldLeak") // the application context, which lives as long as the process
object Battery {
    private lateinit var app: Context
    private lateinit var bm: BatteryManager
    private lateinit var pm: PowerManager

    // Learning state, in memory; only the verdicts go to Prefs.
    private var maxRaw = 0
    private var mAVotes = 0
    private var signVotes = 0

    // Gauges report an averaged current that lags a plug change by seconds to a minute, so
    // nothing is learned until the power source has been steady for SETTLE_MS.
    private const val SETTLE_MS = 60_000L
    private var lastPlugged = -1
    private var plugChangedAt = 0L

    // What the device draws by itself on battery, screen on and off: a moving average over a few
    // minutes, kept in Prefs so it is there the next time the charger goes in.
    private var ownOn = Double.NaN
    private var ownOff = Double.NaN
    private var lastOwn = 0L
    private var savedOwn = 0L

    // The charger rating's history as this app has seen it; the last time seen on battery is
    // saved at most once a minute.
    private var lastOnBattery = 0L
    private var savedOnBattery = 0L

    fun init(context: Context) {
        app = context.applicationContext
        bm = app.getSystemService(BatteryManager::class.java)
        pm = app.getSystemService(PowerManager::class.java)
        // 0.1.0's first builds could learn the sign from the lag after unplugging: start over.
        if (Prefs.detectVersion < 2) {
            Prefs.resetDetection()
            Prefs.ownUseOnMw = Float.NaN
            Prefs.ownUseOffMw = Float.NaN
            Prefs.detectVersion = 2
        }
        ownOn = Prefs.ownUseOnMw.toDouble()
        ownOff = Prefs.ownUseOffMw.toDouble()
        lastOnBattery = Prefs.lastOnBattery
    }

    @Synchronized
    fun read(): Reading {
        val i: Intent? = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
        val rawLevel = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val level = if (rawLevel >= 0) rawLevel * 100 / scale
        else bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0
        val plugged = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val screenOn = pm.isInteractive
        val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeIf { it != Int.MIN_VALUE } ?: 0
        val counter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val cycles = if (Build.VERSION.SDK_INT >= 34)
            i?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1 else -1

        val now = System.currentTimeMillis()
        if (plugged != lastPlugged) {
            lastPlugged = plugged
            plugChangedAt = now
        }
        val settled = now - plugChangedAt >= SETTLE_MS
        if (settled) learn(raw, plugged, screenOn)
        val ua = toMicroAmps(raw)
        trackCharger(i, plugged, now)
        val since = Prefs.chargerChangedAt.takeIf { it > 0 } ?: Prefs.chargerFirstSeen
        val r = Reading(
            time = now,
            rawCurrent = raw,
            currentUa = if (inverted(raw, plugged, status)) -ua else ua,
            voltageMv = toMillivolts(i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0),
            level = level.coerceIn(0, 100),
            status = status,
            plugged = plugged,
            tempDeciC = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?: Int.MIN_VALUE,
            health = i?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0,
            // Some gauges return 0 or MIN_VALUE for "not supported".
            chargeUah = if (counter > 0) counter.toLong() else -1,
            cycles = cycles,
            technology = i?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
            fullInMs = bm.computeChargeTimeRemaining(),
            screenOn = screenOn,
            chargerMaxMw = if (plugged == 0) 0.0 else chargerMax(i),
            chargerSince = since,
            chargerSinceExact = Prefs.chargerChangedAt > 0,
            chargerStale = plugged != 0 && since > 0 && lastOnBattery > since,
            ownUseMw = if (screenOn) ownOn else ownOff,
        )
        if (settled) learnOwnUse(r)
        return r
    }

    /**
     * Android keeps the last charger's max current and voltage in the battery broadcast and
     * sends new ones only when a charger reports different ones, without a date. So the app
     * dates the rating itself: when it saw the value change, or else when it first saw it at all.
     * A rating already there before the device's last time on battery wasn't re-reported for
     * this connection, and may be an earlier charger's.
     */
    private fun trackCharger(i: Intent?, plugged: Int, now: Long) {
        val ua = i?.getIntExtra("max_charging_current", 0) ?: 0
        val uv = i?.getIntExtra("max_charging_voltage", 0) ?: 0
        val key = "$ua/$uv"
        val stored = Prefs.chargerKey
        if (stored == null) {
            Prefs.chargerKey = key
            Prefs.chargerFirstSeen = now
        } else if (stored != key) {
            Prefs.chargerKey = key
            Prefs.chargerFirstSeen = now
            Prefs.chargerChangedAt = now
        }
        if (plugged == 0) {
            lastOnBattery = now
            if (now - savedOnBattery > 60_000) {
                savedOnBattery = now
                Prefs.lastOnBattery = now
            }
        } else if (savedOnBattery != lastOnBattery) {
            // Just plugged in: save the last moment on battery now, it decides staleness.
            savedOnBattery = lastOnBattery
            Prefs.lastOnBattery = lastOnBattery
        }
    }

    /** From the battery broadcast's (hidden but public) extras: µA × µV; 5 V when unsaid. */
    private fun chargerMax(i: Intent?): Double {
        val ua = i?.getIntExtra("max_charging_current", 0) ?: 0
        val uv = (i?.getIntExtra("max_charging_voltage", 0) ?: 0).takeIf { it > 0 } ?: 5_000_000
        return if (ua > 0) ua / 1e6 * (uv / 1e6) * 1000 else 0.0
    }

    private fun learnOwnUse(r: Reading) {
        val dt = r.time - lastOwn
        lastOwn = r.time
        if (r.onPower || r.powerMw >= 0 || r.voltageMv == 0 || dt <= 0 || dt > 60_000) return
        val use = -r.powerMw
        val a = 1 - exp(-dt / 120_000.0) // about a two-minute average
        if (r.screenOn) ownOn = if (ownOn.isNaN()) use else ownOn + a * (use - ownOn)
        else ownOff = if (ownOff.isNaN()) use else ownOff + a * (use - ownOff)
        if (r.time - savedOwn > 60_000) {
            savedOwn = r.time
            Prefs.ownUseOnMw = ownOn.toFloat()
            Prefs.ownUseOffMw = ownOff.toFloat()
        }
    }

    /** Which unit and sign are in use, for the settings screen. */
    fun unitInUse(): CurrentUnit = when (Prefs.unit) {
        CurrentUnit.AUTO -> when (Prefs.detectedUnit) {
            2 -> CurrentUnit.MILLI_AMPS
            1 -> CurrentUnit.MICRO_AMPS
            else -> CurrentUnit.AUTO
        }
        else -> Prefs.unit
    }

    fun signInUse(): CurrentSign = when (Prefs.sign) {
        CurrentSign.AUTO -> when (Prefs.detectedInverted) {
            2 -> CurrentSign.INVERTED
            1 -> CurrentSign.STANDARD
            else -> CurrentSign.AUTO
        }
        else -> Prefs.sign
    }

    @Synchronized
    fun relearn() {
        Prefs.resetDetection()
        Prefs.ownUseOnMw = Float.NaN
        Prefs.ownUseOffMw = Float.NaN
        maxRaw = 0; mAVotes = 0; signVotes = 0
        ownOn = Double.NaN; ownOff = Double.NaN
    }

    private fun learn(raw: Int, plugged: Int, screenOn: Boolean) {
        if (raw == 0) return
        val a = abs(raw)
        if (Prefs.detectedUnit == 0) {
            if (a > maxRaw) maxRaw = a
            // No phone or tablet moves 20 A, so a value that high can only be µA.
            if (maxRaw >= 20_000) Prefs.detectedUnit = 1
            // A lit screen on battery draws well over 20 mA, which in µA would be ≥ 20 000.
            else if (plugged == 0 && screenOn && ++mAVotes >= 30) Prefs.detectedUnit = 2
        }
        if (Prefs.detectedInverted == 0 && plugged == 0) {
            // On battery the current can only flow out, so the sign it shows is "discharging".
            signVotes += if (raw < 0) 1 else -1
            if (signVotes >= 20) Prefs.detectedInverted = 1
            else if (signVotes <= -20) Prefs.detectedInverted = 2
        }
    }

    private fun toMicroAmps(raw: Int): Long = when (unitInUse()) {
        CurrentUnit.MICRO_AMPS -> raw.toLong()
        CurrentUnit.MILLI_AMPS -> raw * 1000L
        // Not learned yet: guess per sample.
        CurrentUnit.AUTO -> if (abs(raw) < 10_000) raw * 1000L else raw.toLong()
    }

    private fun inverted(raw: Int, plugged: Int, status: Int): Boolean = when (signInUse()) {
        CurrentSign.STANDARD -> false
        CurrentSign.INVERTED -> true
        // Not learned yet: make the direction agree with what Android says is happening.
        CurrentSign.AUTO ->
            if (plugged != 0 && status == BatteryManager.BATTERY_STATUS_CHARGING) raw < 0 else raw > 0
    }

    /** EXTRA_VOLTAGE should be mV; a few devices send V or µV. */
    private fun toMillivolts(v: Int): Int = when {
        v <= 0 -> 0
        v < 100 -> v * 1000
        v > 100_000 -> v / 1000
        else -> v
    }
}
