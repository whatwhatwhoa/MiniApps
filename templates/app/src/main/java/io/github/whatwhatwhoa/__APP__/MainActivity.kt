package io.github.whatwhatwhoa.__APP__

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.whatwhatwhoa.__APP__.ui.MainScreen
import io.github.whatwhatwhoa.stack.StackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { StackTheme { MainScreen() } }
    }
}
