package io.github.whatwhatwhoa.powermeter

import android.app.Application

class PowerMeterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Battery.init(this)
        Store.init(this)
        Alerts.init(this)
    }
}
