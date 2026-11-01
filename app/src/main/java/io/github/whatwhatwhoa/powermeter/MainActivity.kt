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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.powermeter.ui.HistoryScreen
import io.github.whatwhatwhoa.powermeter.ui.NowScreen
import io.github.whatwhatwhoa.powermeter.ui.PowerMeterTheme
import io.github.whatwhatwhoa.powermeter.ui.SettingsScreen

private data class Tab(val label: String, val icon: Int)

private val tabs = listOf(
    Tab("Now", R.drawable.ic_tab_now),
    Tab("History", R.drawable.ic_tab_history),
    Tab("Settings", R.drawable.ic_tab_settings),
)

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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Tablets and phones in landscape: a rail on the side and two columns.
        val wide = maxWidth >= 600.dp
        val body = @Composable { mod: Modifier ->
            Box(
                mod.fillMaxSize().verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(Modifier.widthIn(max = if (wide) 1100.dp else 640.dp).fillMaxWidth().padding(16.dp)) {
                    when (tab) {
                        0 -> NowScreen(wide, monitoring) { setMonitoring(true) }
                        1 -> HistoryScreen(wide, monitoring) { setMonitoring(true) }
                        else -> SettingsScreen(monitoring, setMonitoring)
                    }
                }
            }
        }

        if (wide) {
            Row(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { i, t ->
                        NavigationRailItem(
                            selected = i == tab, onClick = { tab = i },
                            icon = { Icon(painterResource(t.icon), null) },
                            label = { Text(t.label) },
                        )
                    }
                }
                body(Modifier.weight(1f))
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = i == tab, onClick = { tab = i },
                                icon = { Icon(painterResource(t.icon), null) },
                                label = { Text(t.label) },
                            )
                        }
                    }
                }
            ) { pad -> body(Modifier.padding(pad)) }
        }
    }
}
