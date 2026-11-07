package io.github.whatwhatwhoa.powermeter.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.powermeter.Battery
import io.github.whatwhatwhoa.powermeter.BuildConfig
import io.github.whatwhatwhoa.powermeter.CurrentSign
import io.github.whatwhatwhoa.powermeter.CurrentUnit
import io.github.whatwhatwhoa.powermeter.Monitor
import io.github.whatwhatwhoa.powermeter.Prefs
import io.github.whatwhatwhoa.powermeter.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(monitoring: Boolean, onMonitoring: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Bumped after a change, so the captions that read Prefs and Battery redraw.
    var tick by remember { mutableIntStateOf(0) }
    var askClear by remember { mutableStateOf(false) }
    val live by Monitor.live.collectAsState()

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val n = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use {
                        Store.get().exportCsv(it)
                    }
                }.getOrNull()
            }
            Toast.makeText(
                context, if (n == null) "Export failed" else "Exported $n samples", Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Panel(title = "Monitoring") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Background monitoring", style = MaterialTheme.typography.bodyLarge)
                    Caption("Logs history, sessions and screen-off drain; shows the live reading in a notification.")
                }
                Switch(checked = monitoring, onCheckedChange = onMonitoring)
            }
            Choice(
                "Update rate on screen", listOf("1 s", "2 s", "5 s"),
                listOf(1000L, 2000L, 5000L).indexOf(Prefs.sampleMs).coerceAtLeast(0)
            ) {
                Prefs.sampleMs = listOf(1000L, 2000L, 5000L)[it]; Monitor.poke(); tick++
            }
            Caption("In the background the meter reads every 5 s, and not at all while the phone sleeps.")
        }

        Panel(title = "Display") {
            Choice("Temperature", listOf("°C", "°F"), if (Prefs.fahrenheit) 1 else 0) {
                Prefs.fahrenheit = it == 1; tick++
            }
        }

        Panel(title = "Current reading") {
            Choice("Direction", listOf("Auto", "Standard", "Inverted"), Prefs.sign.ordinal) {
                Prefs.sign = CurrentSign.entries[it]; Monitor.poke(); tick++
            }
            Choice("Units", listOf("Auto", "µA", "mA"), Prefs.unit.ordinal) {
                Prefs.unit = CurrentUnit.entries[it]; Monitor.poke(); tick++
            }
            Caption(detected(tick))
            live?.let {
                Caption("Raw from the device: current ${it.rawCurrent}, voltage ${it.voltageMv} mV")
            }
            Caption(
                "If charging shows as draining, set Direction to the other one. If the numbers are " +
                    "1000× off, set Units."
            )
            TextButton(onClick = { Battery.relearn(); tick++ }) { Text("Learn again") }
        }

        Panel(title = "History") {
            Choice(
                "Keep samples for", listOf("1 day", "7 days", "30 days"),
                listOf(1, 7, 30).indexOf(Prefs.keepDays).coerceAtLeast(0)
            ) {
                Prefs.keepDays = listOf(1, 7, 30)[it]; tick++
            }
            Caption("Sessions and screen-off stretches are kept for at least 90 days.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { export.launch("power-meter.csv") }) { Text("Export CSV") }
                OutlinedButton(onClick = { askClear = true }) { Text("Clear history") }
            }
        }

        Panel(title = "About") {
            Text("Power Meter ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
            Caption(
                "Readings come from the battery's fuel gauge; how exact they are depends on the " +
                    "device. Power is the net flow at the battery: while charging, the charger's " +
                    "output minus what the device itself is using."
            )
        }
    }

    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text("Clear history?") },
            text = { Text("Deletes every stored sample, session and screen-off record.") },
            confirmButton = {
                TextButton(onClick = {
                    askClear = false
                    scope.launch(Dispatchers.IO) { Store.get().clear() }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { askClear = false }) { Text("Cancel") } },
        )
    }
}

private fun detected(@Suppress("UNUSED_PARAMETER") tick: Int): String {
    val sign = when (Battery.signInUse()) {
        CurrentSign.STANDARD -> "standard"
        CurrentSign.INVERTED -> "inverted"
        CurrentSign.AUTO -> "not learned yet (needs a minute on battery)"
    }
    val unit = when (Battery.unitInUse()) {
        CurrentUnit.MICRO_AMPS -> "µA"
        CurrentUnit.MILLI_AMPS -> "mA"
        CurrentUnit.AUTO -> "not learned yet"
    }
    return "In use: direction $sign, units $unit."
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, o ->
                SegmentedButton(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(o) }
            }
        }
    }
}
