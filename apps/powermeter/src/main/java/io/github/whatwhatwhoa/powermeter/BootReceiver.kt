package io.github.whatwhatwhoa.powermeter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Brings background monitoring back after a reboot or an app update, if it was on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val known = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (known && Prefs.monitoring) MonitorService.start(context)
    }
}
