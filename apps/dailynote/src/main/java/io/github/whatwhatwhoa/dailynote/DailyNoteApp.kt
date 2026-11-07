package io.github.whatwhatwhoa.dailynote

import android.app.Application

class DailyNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Note.init(this)
    }
}
