package io.github.whatwhatwhoa.powermeter

import android.content.Context
import android.content.SharedPreferences

/** How the gauge reports current: the setting, and what auto-detection learned. */
enum class CurrentUnit { AUTO, MICRO_AMPS, MILLI_AMPS }
enum class CurrentSign { AUTO, STANDARD, INVERTED }

/** The app's settings, in one SharedPreferences file. Reads are in-memory, so cheap per sample. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    var monitoring: Boolean
        get() = sp.getBoolean("monitoring", false)
        set(v) = sp.edit().putBoolean("monitoring", v).apply()

    /** Sampling while the app is on screen; the background service samples every 5 s. */
    var sampleMs: Long
        get() = sp.getLong("sample_ms", 1000L)
        set(v) = sp.edit().putLong("sample_ms", v).apply()

    var unit: CurrentUnit
        get() = CurrentUnit.entries[sp.getInt("unit", 0)]
        set(v) = sp.edit().putInt("unit", v.ordinal).apply()

    var sign: CurrentSign
        get() = CurrentSign.entries[sp.getInt("sign", 0)]
        set(v) = sp.edit().putInt("sign", v.ordinal).apply()

    var fahrenheit: Boolean
        get() = sp.getBoolean("fahrenheit", false)
        set(v) = sp.edit().putBoolean("fahrenheit", v).apply()

    var keepDays: Int
        get() = sp.getInt("keep_days", 7)
        set(v) = sp.edit().putInt("keep_days", v).apply()

    /** The device's own draw on battery, screen on / off, in mW; NaN until learned. */
    var ownUseOnMw: Float
        get() = sp.getFloat("own_on", Float.NaN)
        set(v) = sp.edit().putFloat("own_on", v).apply()

    var ownUseOffMw: Float
        get() = sp.getFloat("own_off", Float.NaN)
        set(v) = sp.edit().putFloat("own_off", v).apply()

    /** The charger rating last seen ("µA/µV") and when; see Battery.trackCharger. */
    var chargerKey: String?
        get() = sp.getString("charger_key", null)
        set(v) = sp.edit().putString("charger_key", v).apply()

    var chargerFirstSeen: Long
        get() = sp.getLong("charger_first_seen", 0L)
        set(v) = sp.edit().putLong("charger_first_seen", v).apply()

    /** When the app saw the rating change; 0 = it was already there when first seen. */
    var chargerChangedAt: Long
        get() = sp.getLong("charger_changed_at", 0L)
        set(v) = sp.edit().putLong("charger_changed_at", v).apply()

    var lastOnBattery: Long
        get() = sp.getLong("last_on_battery", 0L)
        set(v) = sp.edit().putLong("last_on_battery", v).apply()

    var detectVersion: Int
        get() = sp.getInt("detect_version", 0)
        set(v) = sp.edit().putInt("detect_version", v).apply()

    // What auto-detection learned about this device's gauge; 0 = not known yet.
    var detectedUnit: Int // 1 = µA, 2 = mA
        get() = sp.getInt("detected_unit", 0)
        set(v) = sp.edit().putInt("detected_unit", v).apply()

    var detectedInverted: Int // 1 = standard, 2 = inverted
        get() = sp.getInt("detected_inverted", 0)
        set(v) = sp.edit().putInt("detected_inverted", v).apply()

    /** The alerts as JSON (see Alerts); null = none set. */
    var alerts: String?
        get() = sp.getString("alerts", null)
        set(v) = sp.edit().putString("alerts", v).apply()

    fun resetDetection() {
        sp.edit().remove("detected_unit").remove("detected_inverted").apply()
    }
}
