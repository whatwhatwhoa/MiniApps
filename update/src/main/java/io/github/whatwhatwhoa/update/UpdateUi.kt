package io.github.whatwhatwhoa.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.stack.KeyValue
import io.github.whatwhatwhoa.stack.Notes
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackCard
import io.github.whatwhatwhoa.stack.StackType
import java.util.Locale

/**
 * The Updates card for an app's Settings or About: this version, the state of the last check,
 * Check for updates or Update to …, and the once-a-day switch. Opening it runs the daily check.
 */
@Composable
fun UpdateCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by Updates.state.collectAsState()
    var auto by remember { mutableStateOf(Updates.autoCheck(context)) }
    val installed = remember { Updates.installed(context) }
    LaunchedEffect(Unit) { Updates.checkDaily(context) }

    StackCard(
        modifier, title = "Updates",
        info = "Updates come from this app's releases on GitHub (${Updates.REPO}).",
    ) {
        KeyValue("This version", installed)
        Status(state, installed, context)
        Actions(state, context, banner = false)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Check once a day", style = StackType.body, color = Stack.colors.ink)
                Text("Asks GitHub at most once a day, so a newer release is offered without a tap.",
                    style = StackType.sub, color = Stack.colors.muted)
            }
            Switch(checked = auto, onCheckedChange = { auto = it; Updates.setAutoCheck(context, it) })
        }
        Notes(
            listOf(
                "The only internet use: asking GitHub for this app's newest release, and downloading it when you tap Update.",
                "Android's installer asks before it replaces the app; your data stays.",
                "The first time, Android asks you to allow installs from this app.",
            )
        )
    }
}

/**
 * A card for the top of an app's page, there only while a newer release is out and not put off
 * with Not now. It runs the daily check when it first shows.
 */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Updates.checkDaily(context) }
    val state by Updates.state.collectAsState()
    var hidden by remember { mutableStateOf(Updates.skipped(context)) }
    val release = when (val s = state) {
        is UpdateState.Available -> s.release.takeIf { it.version != hidden }
        is UpdateState.Downloading -> s.release
        is UpdateState.Installing -> s.release
        is UpdateState.Failed -> s.release
        else -> null
    } ?: return
    val name = remember { context.applicationInfo.loadLabel(context.packageManager).toString() }
    val installed = remember { Updates.installed(context) }

    StackCard(modifier, stripe = Stack.colors.accent) {
        Text("$name ${release.version} is out", style = StackType.h2, color = Stack.colors.ink)
        Status(state, installed, context)
        Actions(state, context, banner = true) {
            Updates.skip(context, release.version)
            hidden = release.version
        }
    }
}

@Composable
private fun Status(state: UpdateState, installed: String, context: Context) {
    val c = Stack.colors
    val text = when (state) {
        UpdateState.Idle -> Updates.lastCheck(context).let {
            if (it == 0L) "Not checked yet." else "Last checked ${ago(it)}."
        }
        UpdateState.Checking -> "Asking GitHub…"
        is UpdateState.UpToDate -> when {
            state.latest == null -> "No release of this app on GitHub yet."
            Updates.newer(installed, state.latest) -> "This copy is newer than the latest release (${state.latest})."
            else -> "Up to date: ${state.latest} is the latest release."
        }
        is UpdateState.Available -> "This is $installed." + (state.note?.let { " $it" } ?: "")
        is UpdateState.Downloading -> "Downloading ${state.release.version}: ${mb(state.done)}" +
            (if (state.total > 0) " of ${mb(state.total)}" else "")
        is UpdateState.Installing -> "Android's installer takes it from here."
        is UpdateState.Failed -> state.message
    }
    Text(text, style = StackType.note, color = if (state is UpdateState.Failed) c.danger else c.muted)
    if (state is UpdateState.Downloading) {
        if (state.total > 0) LinearProgressIndicator(
            progress = { (state.done.toFloat() / state.total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth(),
        ) else LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun Actions(state: UpdateState, context: Context, banner: Boolean, onNotNow: () -> Unit = {}) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            is UpdateState.Available -> {
                StackButton("Update to ${state.release.version}", { Updates.update(context, state.release) }, primary = true)
                WhatsNew(state.release, context)
                if (banner) StackButton("Not now", onNotNow)
            }
            is UpdateState.Failed -> {
                if (state.release != null) {
                    StackButton("Try again", { Updates.update(context, state.release) }, primary = true)
                    if (banner) StackButton("Not now", onNotNow)
                } else StackButton("Check for updates", { Updates.check(context) })
            }
            is UpdateState.Downloading, is UpdateState.Installing -> {}
            UpdateState.Checking -> StackButton("Check for updates", {}, enabled = false)
            else -> StackButton("Check for updates", { Updates.check(context) })
        }
    }
}

@Composable
private fun WhatsNew(release: Release, context: Context) {
    StackButton("What's new", {
        val page = release.page.ifBlank { "https://github.com/${Updates.REPO}/releases" }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    })
}

private fun mb(bytes: Long) = String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)

private fun ago(t: Long): String =
    DateUtils.getRelativeTimeSpanString(t, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
