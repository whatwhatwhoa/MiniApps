package io.github.whatwhatwhoa.powermeter

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Turns the stream of readings into the log: averaged samples, sessions (one per stretch on a
 * power source) and screen-off stretches. Runs in the background service only, on one thread.
 */
class Tracker(private val store: Store) {
    companion object {
        /** The session in progress, for the Now screen; null while the service is off. */
        val current = MutableStateFlow<Session?>(null)

        private const val BUCKET_MS = 5_000L
        /** Longer than this between readings, the phone slept: use the charge counter instead. */
        private const val GAP_MS = 30_000L
        private const val RESUME_MS = 15 * 60_000L
        private const val MIN_SCREEN_OFF_MS = 60_000L
    }

    private var prev: Reading? = null
    private var session: Session? = null

    // The bucket being averaged into one stored sample, weighted by time.
    private var bStart = 0L
    private var bDur = 0L
    private var bUa = 0.0
    private var bMv = 0.0
    private var bMw = 0.0

    private var offStart: Reading? = null
    private var offPlugged = false

    fun begin(r: Reading) {
        val last = store.lastOpenSession()
        session = if (last != null && last.kind == kindOf(r) && r.time - last.end < RESUME_MS) {
            last
        } else {
            last?.let { store.updateSession(it.copy(open = false)) }
            newSession(r)
        }
        current.value = session
        if (!r.screenOn) screen(false, r)
        onReading(r)
    }

    fun onReading(r: Reading) {
        val p = prev
        prev = r
        var s = session ?: newSession(r)
        if (kindOf(r) != s.kind) {
            flush()
            store.updateSession(s.copy(end = r.time, endLevel = r.level, open = false))
            s = newSession(r)
        }
        if (offStart != null && r.onPower) offPlugged = true

        val dt = if (p == null) 0L else r.time - p.time
        if (p != null && dt > 0) {
            val sign = if (s.kind == KIND_CHARGE) 1 else -1
            val mwh = if (dt <= GAP_MS) {
                (p.powerMw + r.powerMw) / 2 * dt / 3_600_000.0
            } else if (p.chargeUah > 0 && r.chargeUah > 0) {
                // µAh × mV = 10⁻⁹ Wh, so / 10⁶ gives mWh.
                (r.chargeUah - p.chargeUah) * (p.voltageMv + r.voltageMv) / 2.0 / 1_000_000.0
            } else 0.0
            s = s.copy(
                end = r.time,
                endLevel = r.level,
                energyMwh = s.energyMwh + sign * mwh,
                peakMw = maxOf(s.peakMw, sign * r.powerMw),
                screenOffMs = s.screenOffMs + if (!p.screenOn && !r.screenOn) dt else 0L,
            )
        }
        session = s
        current.value = s

        // A reading after a gap stands for one nominal bucket, not the whole sleep.
        if (dt > GAP_MS) flush()
        val w = if (dt in 1..GAP_MS) dt.toDouble() else BUCKET_MS.toDouble()
        if (bDur == 0L) bStart = r.time
        bDur += w.toLong()
        bUa += r.currentUa * w
        bMv += r.voltageMv * w
        bMw += r.powerMw * w
        if (r.time - bStart >= BUCKET_MS || bDur >= BUCKET_MS) flush()
    }

    fun screen(on: Boolean, r: Reading) {
        if (!on) {
            if (offStart == null) {
                offStart = r
                offPlugged = r.onPower
            }
            return
        }
        val start = offStart ?: return
        offStart = null
        if (r.time - start.time < MIN_SCREEN_OFF_MS) return
        store.addScreenOff(
            ScreenOff(
                start.time, r.time, start.level, r.level, start.chargeUah, r.chargeUah,
                offPlugged || r.onPower
            )
        )
    }

    fun finish() {
        flush()
        current.value = null
    }

    private fun flush() {
        val r = prev ?: return
        if (bDur > 0) {
            store.addSample(
                bStart, bDur, (bUa / bDur).toLong(), (bMv / bDur).toInt(), bMw / bDur, r
            )
        }
        bDur = 0; bUa = 0.0; bMv = 0.0; bMw = 0.0
        session?.let { store.updateSession(it) }
    }

    private fun newSession(r: Reading): Session {
        val kind = kindOf(r)
        val id = store.openSession(kind, r.time, r.level)
        return Session(id, kind, r.time, r.time, true, r.level, r.level, 0.0, 0.0, 0L)
    }

    private fun kindOf(r: Reading) = if (r.onPower) KIND_CHARGE else KIND_DISCHARGE
}
