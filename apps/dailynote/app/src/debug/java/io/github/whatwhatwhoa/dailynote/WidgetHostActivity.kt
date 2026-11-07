package io.github.whatwhatwhoa.dailynote

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import io.github.whatwhatwhoa.dailynote.widget.NoteWidget

/**
 * Debug builds only: hosts the real widget the way a launcher does, over the wallpaper, so it
 * can be tried without placing it on someone's home screen. Binding needs a one-time grant:
 * `adb shell appwidget grantbind --package io.github.whatwhatwhoa.dailynote --user 0`.
 * Optional extras `w` and `h` (dp) set the widget's size; the default nearly fills the screen.
 * The widget lives only while this screen does: closing it deletes it.
 */
class WidgetHostActivity : Activity() {
    private lateinit var host: AppWidgetHost

    @SuppressLint("SetTextI18n") // a note for the developer, never shown to users
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        host = AppWidgetHost(this, 7)
        host.deleteHost() // whatever an earlier run left behind
        val manager = AppWidgetManager.getInstance(this)
        val cfg = resources.configuration
        val w = intent.getIntExtra("w", cfg.screenWidthDp - 32)
        val h = intent.getIntExtra("h", cfg.screenHeightDp - 140)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, w)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, w)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, h)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, h)
        }
        val id = host.allocateAppWidgetId()
        if (!manager.bindAppWidgetIdIfAllowed(id, ComponentName(this, NoteWidget::class.java), options)) {
            host.deleteAppWidgetId(id)
            setContentView(TextView(this).apply { text = "Run: adb shell appwidget grantbind --package $packageName --user 0" })
            return
        }
        manager.updateAppWidgetOptions(id, options)
        val d = resources.displayMetrics.density
        val view = host.createView(this, id, manager.getAppWidgetInfo(id))
        setContentView(FrameLayout(this).apply {
            addView(view, FrameLayout.LayoutParams((w * d).toInt(), (h * d).toInt(), Gravity.CENTER))
        })
    }

    override fun onStart() {
        super.onStart()
        host.startListening()
    }

    override fun onStop() {
        host.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) host.deleteHost()
        super.onDestroy()
    }
}
