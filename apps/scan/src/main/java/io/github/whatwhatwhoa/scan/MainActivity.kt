package io.github.whatwhatwhoa.scan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import io.github.whatwhatwhoa.scan.ui.MainScreen
import io.github.whatwhatwhoa.stack.StackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // One model per process: an activity opened from a share works on the same pages.
        val model = ScanModel.get(this)
        if (savedInstanceState == null) takeShared(intent, model)
        setContent { StackTheme { MainScreen(model) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeShared(intent, ScanModel.get(this))
    }

    /** Photos shared to Scan from the gallery or another app become pages. */
    private fun takeShared(intent: Intent?, model: ScanModel) {
        val uris: List<Uri> = when (intent?.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
        if (uris.isNotEmpty()) model.addUris(uris)
    }
}
