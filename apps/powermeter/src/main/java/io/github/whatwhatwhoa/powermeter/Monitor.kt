package io.github.whatwhatwhoa.powermeter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A point on the live chart. */
data class Point(val time: Long, val mw: Float, val ua: Long)

/**
 * The one sampler. It runs while anyone needs it: the screen (fast, every [Prefs.sampleMs]) or
 * the background service (every 5 s). Readings go to [live], the last [WINDOW_MS] to [recent],
 * and every reading to [readings] for the service to log.
 */
object Monitor {
    const val WINDOW_MS = 10 * 60_000L
    private const val BACKGROUND_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clients = mutableSetOf<String>()
    private var job: Job? = null
    private val wake = Channel<Unit>(Channel.CONFLATED)

    val live = MutableStateFlow<Reading?>(null)
    val recent = MutableStateFlow<List<Point>>(emptyList())
    private val _readings = MutableSharedFlow<Reading>(extraBufferCapacity = 16)
    val readings: SharedFlow<Reading> = _readings

    @Synchronized
    fun start(client: String) {
        clients += client
        poke()
        if (job?.isActive == true) return
        job = scope.launch {
            if (recent.value.isEmpty()) preload()
            while (isActive) {
                sample()
                // A battery broadcast or a new client cuts the wait short.
                withTimeoutOrNull(interval()) { wake.receive() }
            }
        }
    }

    @Synchronized
    fun stop(client: String) {
        clients -= client
        if (clients.isEmpty()) {
            job?.cancel()
            job = null
        }
    }

    /** Take a reading now (the plug changed, the screen woke, a setting moved). */
    fun poke() {
        wake.trySend(Unit)
    }

    @Synchronized
    private fun interval(): Long = if ("ui" in clients) Prefs.sampleMs else BACKGROUND_MS

    private fun sample() {
        val r = Battery.read()
        live.value = r
        val cut = r.time - WINDOW_MS
        recent.value = recent.value.dropWhile { it.time < cut } +
            Point(r.time, r.powerMw.toFloat(), r.currentUa)
        _readings.tryEmit(r)
    }

    /** The chart starts full when the service has been logging. */
    private fun preload() {
        val from = System.currentTimeMillis() - WINDOW_MS
        recent.value = Store.get().points(from)
    }
}
