package io.github.whatwhatwhoa.dailynote

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.drawable.toDrawable
import io.github.whatwhatwhoa.dailynote.ui.NotePage
import io.github.whatwhatwhoa.dailynote.ui.NoteTheme
import io.github.whatwhatwhoa.dailynote.widget.NoteWidget

class MainActivity : ComponentActivity() {
    /** A tap on the widget's line, waiting for the page to put the cursor there. */
    private val jump = mutableStateOf<Jump?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Paper is always light, whatever the system theme: dark icons in both bars (the page
        // turns the status bar's light over a dark binding).
        val light = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = light, navigationBarStyle = light)
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(PaperArt.paperColor(Prefs.paper).toDrawable())
        if (savedInstanceState == null) jump.value = Jump.from(intent)
        setContent { NoteTheme { NotePage(jump) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        jump.value = Jump.from(intent)
    }

    override fun onStop() {
        Note.flush()
        NoteWidget.refresh(this)
        super.onStop()
    }
}
