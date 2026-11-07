package io.github.whatwhatwhoa.powermeter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Background monitoring: keeps the sampler running, logs through [Tracker] and shows the live
 * figure in an ongoing notification. No wake lock: while the phone sleeps the readings pause and
 * the charge counter covers the gap, so the meter doesn't add to the drain it measures.
 */
class MonitorService : Service() {
    companion object {
        private const val CHANNEL = "monitor"
        private const val NOTE_ID = 1
        private const val ACTION_STOP = "stop"
        private const val NOTE_EVERY_MS = 2_000L

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, MonitorService::class.java))
            } catch (_: Exception) {
                // Not allowed from the background right now; the next app start or boot retries.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private lateinit var tracker: Tracker
    private var lastNote = 0L
    private var lastPrune = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> {
                    val on = intent.action == Intent.ACTION_SCREEN_ON
                    scope.launch { lock.withLock { tracker.screen(on, Battery.read()) } }
                }
                // Checked here, inside the broadcast, as well: a sleeping device wakes for it
                // but may sleep again before the sampler's next reading.
                Intent.ACTION_BATTERY_CHANGED, Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED -> Alerts.check(Battery.read())
            }
            Monitor.poke()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Live power", NotificationManager.IMPORTANCE_LOW).apply {
                description = "The live power reading while background monitoring is on"
                setShowBadge(false)
            }
        )
        val first = note("Power Meter", "Starting…")
        // The specialUse type exists from Android 14; before it a service needs no type.
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTE_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTE_ID, first)
        }
        tracker = Tracker(Store.get())
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }, if (Build.VERSION.SDK_INT >= 33) RECEIVER_NOT_EXPORTED else 0)

        scope.launch {
            lock.withLock { tracker.begin(Battery.read()) }
            Monitor.readings.collect { r ->
                lock.withLock { tracker.onReading(r) }
                Alerts.check(r)
                if (r.time - lastNote >= NOTE_EVERY_MS) {
                    lastNote = r.time
                    nm.notify(NOTE_ID, note(Format.headline(r), Format.noteLine(r)))
                }
                if (r.time - lastPrune > 3_600_000L) {
                    lastPrune = r.time
                    Store.get().prune(Prefs.keepDays)
                }
            }
        }
        Monitor.start("service")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.monitoring = false
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Monitor.stop("service")
        unregisterReceiver(receiver)
        scope.cancel()
        // Finish here, after any reading in flight: the last bucket must still be written.
        runBlocking { lock.withLock { tracker.finish() } }
        super.onDestroy()
    }

    private fun note(title: String, text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
    }
}
