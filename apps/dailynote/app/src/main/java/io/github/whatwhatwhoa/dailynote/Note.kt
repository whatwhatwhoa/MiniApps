package io.github.whatwhatwhoa.dailynote

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * The one note: a text file in the app's private storage. Edits are written half a second after
 * typing stops, and at once when the app leaves the screen; AtomicFile means a write cut short
 * never leaves half a note.
 */
object Note {
    private lateinit var file: AtomicFile
    private val main = Handler(Looper.getMainLooper())
    private var pending: String? = null
    private val writeLater = Runnable { flush() }

    fun init(context: Context) {
        file = AtomicFile(File(context.filesDir, "note.txt"))
    }

    /** The newest text, saved or not, without trailing blank lines left by taps below the text. */
    fun text(): String = (pending ?: read()).trimEnd()

    fun read(): String = try {
        file.readFully().decodeToString()
    } catch (_: FileNotFoundException) {
        ""
    }

    fun edit(text: String) {
        pending = text
        main.removeCallbacks(writeLater)
        main.postDelayed(writeLater, 500)
    }

    fun flush() {
        val text = pending ?: return
        pending = null
        main.removeCallbacks(writeLater)
        val out = file.startWrite()
        try {
            out.write(text.encodeToByteArray())
            file.finishWrite(out)
        } catch (_: IOException) {
            file.failWrite(out)
        }
    }
}

/** Where a tap on the widget puts the cursor: [newlines] blank lines added, then [offset]. */
data class Jump(val offset: Int, val newlines: Int) {
    companion object {
        const val OFFSET = "io.github.whatwhatwhoa.dailynote.OFFSET"
        const val NEWLINES = "io.github.whatwhatwhoa.dailynote.NEWLINES"

        fun from(intent: Intent?): Jump? {
            if (intent == null || !intent.hasExtra(OFFSET)) return null
            return Jump(intent.getIntExtra(OFFSET, 0), intent.getIntExtra(NEWLINES, 0))
        }
    }
}
