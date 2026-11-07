package io.github.whatwhatwhoa.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.whatwhatwhoa.calculator.ui.MainScreen
import io.github.whatwhatwhoa.stack.StackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The model reads its saved state, so a turn of the phone (a new activity) loses nothing.
        val model = CalcModel(this)
        setContent { StackTheme { MainScreen(model) } }
    }
}
