package io.github.whatwhatwhoa.scan.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.whatwhatwhoa.scan.ExportState
import io.github.whatwhatwhoa.scan.PageSize
import io.github.whatwhatwhoa.scan.Quality
import io.github.whatwhatwhoa.scan.ScanModel
import io.github.whatwhatwhoa.stack.FLabel
import io.github.whatwhatwhoa.stack.Seg
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackField
import io.github.whatwhatwhoa.stack.StackType

/**
 * Export PDF: the file name, page size and image quality, then Save PDF (Android asks where)
 * or Share (mail, chat, a drive). The pages stay afterwards; Start a new scan clears them.
 */
@Composable
fun ExportSheet(model: ScanModel, onClose: () -> Unit) {
    val c = Stack.colors
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(model.defaultName()) }
    var askClear by remember { mutableStateOf(false) }
    val state = model.export
    val working = state is ExportState.Working
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) model.exportPdf(name, uri, null)
    }
    LaunchedEffect(Unit) { model.clearExport() }
    val n = model.pages.size

    Dialog(onDismissRequest = { if (!working) onClose() }) {
        val shape = RoundedCornerShape(14.dp)
        Column(
            Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(shape).background(c.panel).border(1.dp, c.line, shape)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Export PDF", style = StackType.h1.copy(fontSize = 17.sp), color = c.ink)
            Text(if (n == 1) "1 page." else "$n pages, in the order of the page list.", style = StackType.sub, color = c.muted)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FLabel("File name")
                StackField(name, { name = it }, Modifier.fillMaxWidth(),
                    suffix = if (name.trim().endsWith(".pdf", ignoreCase = true)) null else ".pdf",
                    keyboardType = KeyboardType.Text, mono = false, description = "File name")
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FLabel("Page size")
                Seg(PageSize.entries, model.pageSize, model::choosePageSize, { it.label })
                Text(model.pageSize.desc, style = StackType.sub, color = c.muted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FLabel("Image quality")
                Seg(Quality.entries, model.quality, model::chooseQuality, { it.label })
                Text(model.quality.desc, style = StackType.sub, color = c.muted)
            }
            when (state) {
                is ExportState.Working -> Text(state.text, style = StackType.note, color = c.muted)
                is ExportState.Done -> Text(state.text, style = StackType.note, color = c.good)
                is ExportState.Failed -> Text(state.text, style = StackType.note, color = c.danger)
                null -> Unit
            }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StackButton(if (state is ExportState.Done) "Close" else "Cancel", onClose, enabled = !working)
                StackButton("Share", { model.exportPdf(name, null) { share(context, it) } }, enabled = !working && n > 0)
                StackButton("Save PDF", { save.launch(model.fileName(name)) }, primary = true, enabled = !working && n > 0)
            }
            if (state is ExportState.Done) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Done with these pages?", style = StackType.note, color = c.muted)
                    StackButton("Start a new scan", { askClear = true }, small = true)
                }
            }
        }
    }
    if (askClear) ClearDialog(model, onDone = { askClear = false; onClose() }, onCancel = { askClear = false })
}

private fun share(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share PDF"))
}

/** Asks before every page goes; used by Settings and after an export. */
@Composable
fun ClearDialog(model: ScanModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val c = Stack.colors
    val n = model.pages.size
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (n == 1) "Remove the page?" else "Remove all $n pages?") },
        text = { Text("Scan deletes its copies of the photos. A PDF you saved stays, and so do photos in your gallery.") },
        confirmButton = { TextButton({ model.clearAll(); onDone() }) { Text("Remove") } },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } },
        containerColor = c.panel,
    )
}
