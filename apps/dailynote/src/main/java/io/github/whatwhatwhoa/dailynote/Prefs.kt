package io.github.whatwhatwhoa.dailynote

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** The settings, stored by name so reordering the enums loses nothing. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    var paper: Paper
        get() = Paper.entries.firstOrNull { it.name == sp.getString("paper", null) } ?: Paper.YELLOW
        set(v) = sp.edit { putString("paper", v.name) }

    var size: NoteSize
        get() = NoteSize.entries.firstOrNull { it.name == sp.getString("size", null) } ?: NoteSize.MEDIUM
        set(v) = sp.edit { putString("size", v.name) }

    var font: NoteFont
        get() = NoteFont.entries.firstOrNull { it.name == sp.getString("font", null) } ?: NoteFont.NORMAL
        set(v) = sp.edit { putString("font", v.name) }

    var swipeErase: Boolean
        get() = sp.getBoolean("swipe_erase", true)
        set(v) = sp.edit { putBoolean("swipe_erase", v) }
}
