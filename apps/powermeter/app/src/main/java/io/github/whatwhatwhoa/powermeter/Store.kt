package io.github.whatwhatwhoa.powermeter

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.Writer

const val KIND_CHARGE = 1
const val KIND_DISCHARGE = 2

/** A stretch on one power source: plugged in (charge) or on battery (discharge). */
data class Session(
    val id: Long,
    val kind: Int,
    val start: Long,
    val end: Long,          // last update while open
    val open: Boolean,
    val startLevel: Int,
    val endLevel: Int,
    val energyMwh: Double,  // into the battery for a charge, out of it for a discharge
    val peakMw: Double,     // same direction as energy
    val screenOffMs: Long,
)

/** A screen-off stretch: what standby cost. */
data class ScreenOff(
    val start: Long,
    val end: Long,
    val startLevel: Int,
    val endLevel: Int,
    val startUah: Long,
    val endUah: Long,
    val plugged: Boolean,   // on power at any time, so not a drain figure
) {
    val hours: Double get() = (end - start) / 3_600_000.0
    val pctPerHour: Double get() = if (hours > 0) (startLevel - endLevel) / hours else 0.0
    val avgMa: Double
        get() = if (hours > 0 && startUah > 0 && endUah > 0) (startUah - endUah) / 1000.0 / hours
        else Double.NaN
}

/** One stored sample row: the average over a few seconds. */
data class Row(val time: Long, val mw: Float, val level: Int)

/** SQLite without a library: three small tables. */
class Store private constructor(context: Context) :
    SQLiteOpenHelper(context, "powermeter.db", null, 1) {

    companion object {
        @Volatile private var instance: Store? = null
        fun init(context: Context) {
            instance = Store(context.applicationContext)
        }
        fun get(): Store = instance!!
    }

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE samples (t INTEGER PRIMARY KEY, dur INTEGER, ua INTEGER, mv INTEGER,
               mw REAL, level INTEGER, temp INTEGER, status INTEGER, plugged INTEGER,
               screen INTEGER)"""
        )
        db.execSQL(
            """CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, kind INTEGER,
               start INTEGER, updated INTEGER, open INTEGER, start_level INTEGER,
               end_level INTEGER, energy_mwh REAL, peak_mw REAL, screen_off_ms INTEGER)"""
        )
        db.execSQL(
            """CREATE TABLE screen_off (id INTEGER PRIMARY KEY AUTOINCREMENT, start INTEGER,
               end INTEGER, start_level INTEGER, end_level INTEGER, start_uah INTEGER,
               end_uah INTEGER, plugged INTEGER)"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addSample(t: Long, dur: Long, ua: Long, mv: Int, mw: Double, r: Reading) {
        writableDatabase.insertWithOnConflict("samples", null, ContentValues().apply {
            put("t", t); put("dur", dur); put("ua", ua); put("mv", mv); put("mw", mw)
            put("level", r.level); put("temp", r.tempDeciC); put("status", r.status)
            put("plugged", r.plugged); put("screen", if (r.screenOn) 1 else 0)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun points(from: Long): List<Point> =
        readableDatabase.rawQuery(
            "SELECT t, mw, ua FROM samples WHERE t >= ? ORDER BY t", arrayOf(from.toString())
        ).use { c ->
            List(c.count) { c.moveToNext(); Point(c.getLong(0), c.getFloat(1), c.getLong(2)) }
        }

    /** About [buckets] averaged rows between [from] and now, for the history charts. */
    fun rows(from: Long, buckets: Int): List<Row> {
        val step = ((System.currentTimeMillis() - from) / buckets).coerceAtLeast(5_000)
        return readableDatabase.rawQuery(
            """SELECT MIN(t), SUM(mw * dur) / SUM(dur), AVG(level) FROM samples WHERE t >= ?
               GROUP BY t / ? ORDER BY 1""",
            arrayOf(from.toString(), step.toString())
        ).use { c ->
            List(c.count) { c.moveToNext(); Row(c.getLong(0), c.getFloat(1), c.getInt(2)) }
        }
    }

    // --- Sessions

    fun openSession(kind: Int, t: Long, level: Int): Long =
        writableDatabase.insert("sessions", null, ContentValues().apply {
            put("kind", kind); put("start", t); put("updated", t); put("open", 1)
            put("start_level", level); put("end_level", level); put("energy_mwh", 0.0)
            put("peak_mw", 0.0); put("screen_off_ms", 0L)
        })

    fun updateSession(s: Session) {
        writableDatabase.update("sessions", ContentValues().apply {
            put("updated", s.end); put("open", if (s.open) 1 else 0); put("end_level", s.endLevel)
            put("energy_mwh", s.energyMwh); put("peak_mw", s.peakMw)
            put("screen_off_ms", s.screenOffMs)
        }, "id = ?", arrayOf(s.id.toString()))
    }

    fun lastOpenSession(): Session? = sessions("WHERE open = 1", 1).firstOrNull()

    fun sessions(limit: Int): List<Session> = sessions("", limit)

    fun sessionsSince(from: Long): List<Session> = sessions("WHERE updated >= $from", 1000)

    private fun sessions(where: String, limit: Int): List<Session> =
        readableDatabase.rawQuery(
            """SELECT id, kind, start, updated, open, start_level, end_level, energy_mwh,
               peak_mw, screen_off_ms FROM sessions $where ORDER BY start DESC LIMIT $limit""",
            null
        ).use { c ->
            List(c.count) {
                c.moveToNext()
                Session(
                    c.getLong(0), c.getInt(1), c.getLong(2), c.getLong(3), c.getInt(4) == 1,
                    c.getInt(5), c.getInt(6), c.getDouble(7), c.getDouble(8), c.getLong(9)
                )
            }
        }

    // --- Screen off

    fun addScreenOff(s: ScreenOff) {
        writableDatabase.insert("screen_off", null, ContentValues().apply {
            put("start", s.start); put("end", s.end); put("start_level", s.startLevel)
            put("end_level", s.endLevel); put("start_uah", s.startUah); put("end_uah", s.endUah)
            put("plugged", if (s.plugged) 1 else 0)
        })
    }

    fun screenOffs(from: Long, limit: Int): List<ScreenOff> =
        readableDatabase.rawQuery(
            """SELECT start, end, start_level, end_level, start_uah, end_uah, plugged
               FROM screen_off WHERE end >= ? ORDER BY start DESC LIMIT $limit""",
            arrayOf(from.toString())
        ).use { c ->
            List(c.count) {
                c.moveToNext()
                ScreenOff(
                    c.getLong(0), c.getLong(1), c.getInt(2), c.getInt(3), c.getLong(4),
                    c.getLong(5), c.getInt(6) == 1
                )
            }
        }

    // --- Upkeep

    fun prune(keepDays: Int) {
        val now = System.currentTimeMillis()
        val cut = now - keepDays * 86_400_000L
        // Sessions and screen-off rows are tiny; they keep a longer record than the samples.
        val longCut = now - maxOf(keepDays, 90) * 86_400_000L
        writableDatabase.delete("samples", "t < ?", arrayOf(cut.toString()))
        writableDatabase.delete("sessions", "updated < ? AND open = 0", arrayOf(longCut.toString()))
        writableDatabase.delete("screen_off", "end < ?", arrayOf(longCut.toString()))
    }

    fun clear() {
        writableDatabase.delete("samples", null, null)
        writableDatabase.delete("sessions", "open = 0", null)
        writableDatabase.delete("screen_off", null, null)
    }

    /** Every stored sample as CSV; returns the row count. */
    fun exportCsv(out: Writer): Int {
        out.write("time_ms,duration_ms,current_ma,voltage_v,power_w,level_pct,temp_c,status,plugged,screen_on\n")
        fun f(x: Double, decimals: Int) = "%.${decimals}f".format(java.util.Locale.ROOT, x)
        var n = 0
        readableDatabase.rawQuery(
            "SELECT t, dur, ua, mv, mw, level, temp, status, plugged, screen FROM samples ORDER BY t",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out.write(
                    listOf(
                        c.getLong(0), c.getLong(1), f(c.getLong(2) / 1000.0, 1),
                        f(c.getInt(3) / 1000.0, 3), f(c.getDouble(4) / 1000.0, 3), c.getInt(5),
                        f(c.getInt(6) / 10.0, 1), c.getInt(7), c.getInt(8), c.getInt(9)
                    ).joinToString(",", postfix = "\n")
                )
                n++
            }
        }
        return n
    }
}
