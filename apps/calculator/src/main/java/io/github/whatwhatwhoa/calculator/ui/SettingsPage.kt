package io.github.whatwhatwhoa.calculator.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.whatwhatwhoa.calculator.CalcModel
import io.github.whatwhatwhoa.stack.KeyValue
import io.github.whatwhatwhoa.stack.Notes
import io.github.whatwhatwhoa.stack.Seg
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackCard
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.update.UpdateCard
import io.github.whatwhatwhoa.update.Updates

@Composable
fun SettingsPage(m: CalcModel) {
    val c = Stack.colors
    val context = LocalContext.current
    var ask by remember { mutableStateOf(false) }

    StackCard(title = "Angles") {
        Seg(listOf(true, false), m.degrees, m::chooseDegrees, { if (it) "Degrees" else "Radians" })
        Text("For sin, cos, tan and their inverses; Deg / Rad on the keys switches it too.",
            style = StackType.sub, color = c.muted)
    }

    StackCard(title = "History") {
        KeyValue("Answers kept", "${m.history.size} of ${CalcModel.MAX_HISTORY}")
        Text("The oldest goes when a new one comes past ${CalcModel.MAX_HISTORY}.", style = StackType.sub, color = c.muted)
        StackButton("Clear history", { ask = true }, enabled = m.history.isNotEmpty())
    }

    UpdateCard()

    StackCard(title = "How it reads a formula") {
        Notes(
            listOf(
                "⌫ deletes before the cursor, Del after it; the arrows move it, or tap in the formula.",
                "Clear all empties the formula; history stays.",
                "Brackets left open close themselves: the faint ) shows where.",
                "No × is needed before a bracket, a name or π: 2π, 3(4 + 1), 2sin(30).",
                "200 + 10% is 220 (10% of 200 added); 10% alone is 0.1.",
                "−2² is −4, as in math; (−2)² is 4.",
                "^ goes right to left: 2^3^2 is 2^9.",
                "Answers keep 12 significant digits, so 0.1 + 0.2 is 0.3.",
                "ans is the last answer; after =, an operator goes on from it and a digit starts over.",
                "A keyboard works too: * / - for × ÷ −, Enter for =, Esc for Clear all.",
            )
        )
    }

    StackCard(title = "About") {
        Text("Calculator ${Updates.installed(context)}", style = StackType.body, color = c.ink)
        Text("Free and open source (MIT), part of the Mini Apps series on GitHub.", style = StackType.sub, color = c.muted)
    }

    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text("Clear history?") },
            text = { Text("Removes every answer in the list. Your functions stay.") },
            confirmButton = { TextButton({ m.clearHistory(); ask = false }) { Text("Clear") } },
            dismissButton = { TextButton({ ask = false }) { Text("Cancel") } },
            containerColor = c.panel,
        )
    }
}
