package io.github.whatwhatwhoa.powermeter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.powermeter.ui.HistoryScreen
import io.github.whatwhatwhoa.powermeter.ui.NowScreen
import io.github.whatwhatwhoa.powermeter.ui.PowerMeterTheme
import io.github.whatwhatwhoa.powermeter.ui.SettingsScreen
import io.github.whatwhatwhoa.stack.BlockGap
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackHeader
import io.github.whatwhatwhoa.stack.StackTabs
import io.github.whatwhatwhoa.stack.stackColumn

private val tabs = listOf("Now", "History", "Settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { PowerMeterTheme { App() } }
    }

    override fun onStart() {
        super.onStart()
        Monitor.start("ui")
        // In the foreground the service may start; this also brings it back after a force stop.
        if (Prefs.monitoring) MonitorService.start(this)
    }

    override fun onStop() {
        Monitor.stop("ui")
        super.onStop()
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var monitoring by remember { mutableStateOf(Prefs.monitoring) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val setMonitoring = { on: Boolean ->
        Prefs.monitoring = on
        monitoring = on
        if (on) {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            MonitorService.start(context)
        } else {
            MonitorService.stop(context)
        }
    }

    // The Stack frame: header, tabs under it, then the tab's page. From 600 dp the pages lay
    // out in two columns.
    val c = Stack.colors
    BoxWithConstraints(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val wide = maxWidth >= 600.dp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = if (wide) 1100.dp else 640.dp).stackColumn()) {
                StackHeader("Power Meter", count = if (monitoring) "monitoring on" else "monitoring off")
                Spacer(Modifier.height(6.dp))
                StackTabs(tabs, tab, { tab = it })
            }
            Box(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(Modifier.widthIn(max = if (wide) 1100.dp else 640.dp).stackColumn()
                    .padding(top = BlockGap, bottom = 32.dp)) {
                    when (tab) {
                        0 -> NowScreen(wide, monitoring) { setMonitoring(true) }
                        1 -> HistoryScreen(wide, monitoring) { setMonitoring(true) }
                        else -> SettingsScreen(monitoring, setMonitoring)
                    }
                }
            }
        }
    }
}
