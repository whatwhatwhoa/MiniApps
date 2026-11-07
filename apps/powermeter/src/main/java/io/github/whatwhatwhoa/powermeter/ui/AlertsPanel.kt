package io.github.whatwhatwhoa.powermeter.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.whatwhatwhoa.powermeter.Alert
import io.github.whatwhatwhoa.powermeter.Alerts
import io.github.whatwhatwhoa.powermeter.During
import io.github.whatwhatwhoa.powermeter.Format
import io.github.whatwhatwhoa.powermeter.Prefs
import io.github.whatwhatwhoa.powermeter.Watch

/** Settings → Alerts: the list, each with its switch; tap one to change it. */
@Composable
fun AlertsPanel(monitoring: Boolean, onMonitoring: (Boolean) -> Unit) {
    val context = LocalContext.current
    val alerts by Alerts.list.collectAsState()
    val fired by Alerts.firedAt.collectAsState()
    var editing by remember { mutableStateOf<Alert?>(null) }
    // Re-read on resume: the user may have just allowed notifications in system settings.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumes++; onPauseOrDispose { } }
    val canNotify = remember(resumes) { notificationsAllowed(context) }
    // Android's own prompt first; once it has been refused for good, the app's system settings.
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) openNotificationSettings(context)
        resumes++
    }

    // Alerts are checked by background monitoring, so switching one on turns it on too.
    val save = { list: List<Alert> ->
        Alerts.save(list)
        if (list.any { it.on } && !monitoring) {
            onMonitoring(true)
            Toast.makeText(context, "Background monitoring is on, for the alerts", Toast.LENGTH_SHORT).show()
        }
    }

    Panel(title = "Alerts") {
        if (alerts.isEmpty()) {
            Caption("None yet. For example: battery at 80% while plugged in, or charging below 5 W for 2 min.")
        }
        for (a in alerts) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { editing = a }.padding(vertical = 4.dp)) {
                    Text(Format.alert(a), style = MaterialTheme.typography.bodyLarge)
                    fired[a.id]?.let { Caption("Last alert ${Format.dayClock(it)}") }
                }
                Switch(checked = a.on, onCheckedChange = { on ->
                    save(alerts.map { if (it.id == a.id) it.copy(on = on) else it })
                })
            }
        }
        if (!canNotify && alerts.any { it.on }) {
            Caption("Notifications are off for Power Meter, so alerts can't show.")
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                else openNotificationSettings(context)
            }) { Text("Allow notifications") }
        } else if (!monitoring && alerts.any { it.on }) {
            Caption("Background monitoring is off, so alerts aren't checked.")
            TextButton(onClick = { onMonitoring(true) }) { Text("Turn on monitoring") }
        }
        OutlinedButton(onClick = { editing = newAlert() }) { Text("Add alert") }
        Caption("• Checked by background monitoring, about every 5 s while the device is awake.")
        Caption("• It never wakes the device to check: power alerts count awake time only.")
        Caption("• An alert shows once, then again after its condition has cleared for a minute.")
    }

    editing?.let { a ->
        val isNew = alerts.none { it.id == a.id }
        AlertEditor(
            initial = a,
            isNew = isNew,
            onSave = { saved ->
                editing = null
                save(if (isNew) alerts + saved else alerts.map { if (it.id == saved.id) saved else it })
            },
            onDelete = {
                editing = null
                Alerts.save(alerts.filter { it.id != a.id })
            },
            onDismiss = { editing = null },
        )
    }
}

private fun newAlert() = Alert(
    id = Alerts.nextId(), on = true, watch = Watch.LEVEL, above = true, value = 80.0,
    forMs = 0, during = During.PLUGGED,
)

/** What a new choice of [Watch] starts with: the common case for each. */
private fun defaultsFor(w: Watch, a: Alert): Alert = when (w) {
    Watch.LEVEL -> a.copy(watch = w, above = true, value = 80.0, forMs = 0, during = During.PLUGGED)
    Watch.CHARGING -> a.copy(watch = w, above = false, value = 5.0, forMs = 120_000, during = During.ANY)
    Watch.DRAIN -> a.copy(watch = w, above = true, value = 3.0, forMs = 300_000, during = During.ANY)
    Watch.TEMP -> a.copy(watch = w, above = true, value = 40.0, forMs = 60_000, during = During.ANY)
}

private val watchLabels = listOf(
    Watch.LEVEL to "Battery level (%)",
    Watch.CHARGING to "Charging power (W), while plugged in",
    Watch.DRAIN to "Drain (W), on battery",
    Watch.TEMP to "Battery temperature",
)

@Composable
private fun AlertEditor(
    initial: Alert,
    isNew: Boolean,
    onSave: (Alert) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var a by remember { mutableStateOf(initial) }
    val fahrenheit = Prefs.fahrenheit
    fun shown(x: Alert) = Format.plain(
        if (x.watch == Watch.TEMP && fahrenheit) Math.round(Format.cToF(x.value) * 10) / 10.0 else x.value
    )
    var valueText by remember { mutableStateOf(shown(initial)) }
    val inMinutes = initial.forMs % 60_000 == 0L
    var forMinutes by remember { mutableStateOf(inMinutes) }
    var forText by remember {
        mutableStateOf((if (inMinutes) initial.forMs / 60_000 else initial.forMs / 1000).toString())
    }

    val unit = when (a.watch) {
        Watch.LEVEL -> "%"
        Watch.CHARGING, Watch.DRAIN -> "W"
        Watch.TEMP -> if (fahrenheit) "°F" else "°C"
    }
    val typed = valueText.replace(',', '.').toDoubleOrNull()
    // Stored in °C; the field shows the unit chosen under Display.
    val value = if (typed != null && a.watch == Watch.TEMP && fahrenheit) Format.fToC(typed) else typed
    val range = when (a.watch) {
        Watch.LEVEL -> 1.0..100.0
        Watch.CHARGING, Watch.DRAIN -> 0.0..200.0
        Watch.TEMP -> -20.0..80.0
    }
    val valueOk = value != null && value in range
    val forN = forText.trim().toLongOrNull()
    val forMs = forN?.let { it * if (forMinutes) 60_000 else 1000 }
    val forOk = forMs != null && forMs in 0..24 * 3_600_000L
    val result = if (valueOk && forOk) a.copy(value = value!!, forMs = forMs!!) else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New alert" else "Edit alert") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text("Watch", style = MaterialTheme.typography.bodyLarge)
                    for ((w, label) in watchLabels) {
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(selected = a.watch == w, role = Role.RadioButton) {
                                    if (a.watch != w) {
                                        a = defaultsFor(w, a)
                                        valueText = shown(a)
                                        forMinutes = a.forMs % 60_000 == 0L
                                        forText = (if (forMinutes) a.forMs / 60_000 else a.forMs / 1000).toString()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = a.watch == w, onClick = null)
                            Text(label, Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp))
                        }
                    }
                }
                Choice("Alert when it is", listOf("At or above", "At or below"), if (a.above) 0 else 1) {
                    a = a.copy(above = it == 0)
                }
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("Value") },
                    suffix = { Text(unit) },
                    singleLine = true,
                    isError = !valueOk,
                    supportingText = if (valueOk) null else {
                        { Text("From ${Format.plain(range.start)} to ${Format.plain(range.endInclusive)} ${if (a.watch == Watch.TEMP) "°C" else unit}") }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = forText,
                        onValueChange = { forText = it },
                        label = { Text("For at least") },
                        suffix = { Text(if (forMinutes) "min" else "s") },
                        singleLine = true,
                        isError = !forOk,
                        supportingText = { Text(if (forOk) "0 = as soon as it happens" else "Up to 24 h") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Choice(null, listOf("Seconds", "Minutes"), if (forMinutes) 1 else 0) {
                        forMinutes = it == 1
                    }
                }
                if (!a.isPower) {
                    Choice(
                        "When", listOf("Any time", "Plugged in", "On battery"), a.during.ordinal
                    ) { a = a.copy(during = During.entries[it]) }
                }
                if (a.watch == Watch.CHARGING) {
                    Caption("• Charging slows by itself near full; full or held at a limit doesn't count.")
                }
                if (a.isPower) {
                    Caption("• The first minute after plugging in or out doesn't count: the gauge lags.")
                }
                result?.let {
                    Text(Format.alert(it), style = MaterialTheme.typography.titleSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { result?.let(onSave) }, enabled = result != null) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun notificationsAllowed(context: Context): Boolean {
    val nm = context.getSystemService(NotificationManager::class.java)
    val channel = nm.getNotificationChannel("alerts")
    return nm.areNotificationsEnabled() &&
        (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )
}
