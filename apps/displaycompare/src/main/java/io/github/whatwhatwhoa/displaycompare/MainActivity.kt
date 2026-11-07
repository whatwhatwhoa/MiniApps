package io.github.whatwhatwhoa.displaycompare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.whatwhatwhoa.displaycompare.ui.CompareScreen
import io.github.whatwhatwhoa.stack.StackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The activity's own context: "This phone" reads the display the app is on.
        val state = CompareState(this)
        setContent { StackTheme { CompareScreen(state) } }
    }
}
