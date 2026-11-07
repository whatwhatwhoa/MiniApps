package io.github.whatwhatwhoa.scan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.whatwhatwhoa.scan.Filter
import io.github.whatwhatwhoa.scan.Imaging
import io.github.whatwhatwhoa.scan.ScanModel
import io.github.whatwhatwhoa.stack.KeyValue
import io.github.whatwhatwhoa.stack.Notes
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackCard
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.update.UpdateCard
import io.github.whatwhatwhoa.update.Updates

@Composable
fun SettingsPage(model: ScanModel) {
    val c = Stack.colors
    val context = LocalContext.current
    var ask by remember { mutableStateOf(false) }

    StackCard(title = "Crop") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Crosshair snap", style = StackType.body, color = c.ink)
                Text("On: a tap anywhere moves the nearest corner there. Off: only a dragged corner moves, and one finger pans.",
                    style = StackType.sub, color = c.muted)
            }
            Switch(model.snap, { model.toggleSnap() })
        }
    }

    val stored = remember(model.pages.size) { model.storedText() }
    StackCard(title = "Pages", info = "Scan keeps its own copy of each photo until you remove the page, so the pages survive closing the app.") {
        KeyValue("Pages", "${model.pages.size}")
        KeyValue("Stored on this device", stored)
        StackButton("Remove all pages", { ask = true }, enabled = model.pages.isNotEmpty())
    }

    UpdateCard()

    StackCard(title = "Filters") {
        Notes(Filter.entries.map { "${it.label}: ${it.desc}" })
    }

    StackCard(title = "How it works") {
        Notes(
            listOf(
                "Take photo opens your camera app; Add photos opens the photo picker; photos shared to Scan come in too.",
                "Each photo is kept at most ${Imaging.MAX_SRC} px on its long side, on this device only.",
                "When a photo comes in, Scan looks for the paper's edges; drag a corner to fix the crop.",
                "The magnifier at the top right shows what is under your finger while you drag a corner.",
                "Pinch to zoom and move with two fingers; the % button fits the photo again.",
                "Apply crop straightens the page; then pick a filter, and turn the page if it lies sideways.",
                "Export PDF keeps the order of the page list: Save PDF asks where, Share sends it to another app.",
                "The pages stay after an export, until you remove them.",
            )
        )
    }

    StackCard(title = "About") {
        Text("Scan ${Updates.installed(context)}", style = StackType.body, color = c.ink)
        Text("Free and open source (MIT), part of the Mini Apps series on GitHub.", style = StackType.sub, color = c.muted)
    }

    if (ask) ClearDialog(model, onDone = { ask = false }, onCancel = { ask = false })
}
