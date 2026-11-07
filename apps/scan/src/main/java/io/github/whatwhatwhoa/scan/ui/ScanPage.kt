package io.github.whatwhatwhoa.scan.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import io.github.whatwhatwhoa.scan.Filter
import io.github.whatwhatwhoa.scan.Mode
import io.github.whatwhatwhoa.scan.Page
import io.github.whatwhatwhoa.scan.ScanModel
import io.github.whatwhatwhoa.stack.Callout
import io.github.whatwhatwhoa.stack.Chip
import io.github.whatwhatwhoa.stack.FLabel
import io.github.whatwhatwhoa.stack.MatInk
import io.github.whatwhatwhoa.stack.Seg
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.stack.Swatch
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

/** A filter's series color, fixed per filter and always shown beside its name. */
@Composable
fun filterColor(f: Filter): Color {
    val c = Stack.colors
    return when (f) {
        Filter.Original -> c.s0
        Filter.Photo -> c.s1
        Filter.Document -> c.s2
        Filter.Gray -> c.sOther
        Filter.BW -> c.s4
    }
}

/**
 * The scan: Take photo / Add photos / Export, the pages (a strip on a phone, a list beside the
 * mat from 600 dp), the toolbar of the current view, the mat, Previous / Next and the step on.
 */
@Composable
fun ScanPage(model: ScanModel, wide: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var exporting by rememberSaveable { mutableStateOf(false) }
    var askRemove by remember { mutableStateOf<Page?>(null) }

    // The camera app writes into a fixed file in the cache, so a result still lands after Android
    // recreated this screen (or the whole app) while the camera was open.
    val camFile = remember { File(context.cacheDir, "camera/capture.jpg") }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && camFile.length() > 0) model.addCameraShot(camFile)
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        model.addUris(uris)
    }
    val onTake = {
        camFile.parentFile?.mkdirs()
        val uri = FileProvider.getUriForFile(context, context.packageName + ScanModel.AUTHORITY_SUFFIX, camFile)
        try { takePhoto.launch(uri) } catch (e: ActivityNotFoundException) {
            model.message = "No camera app answered: add photos instead."
        }
    }
    val onPick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val onExport = { exporting = true }

    val msg = model.message
    LaunchedEffect(msg) { if (msg != null) { delay(6000); model.message = null } }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (model.pages.isNotEmpty()) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StackButton("Take photo", onTake)
                StackButton("Add photos", onPick)
                Spacer(Modifier.weight(1f))
                StackButton("Export PDF", onExport)
            }
        }
        if (msg != null) Callout(msg)
        if (wide) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PageList(model, Modifier.width(290.dp).fillMaxHeight()) { askRemove = it }
                Editor(model, true, Modifier.weight(1f).fillMaxHeight(), onTake, onPick, onExport)
            }
        } else {
            if (model.pages.isNotEmpty()) {
                PageStrip(model)
                PageRow(model) { askRemove = it }
            }
            Editor(model, false, Modifier.weight(1f).fillMaxWidth(), onTake, onPick, onExport)
        }
    }

    if (exporting) ExportSheet(model) { exporting = false }
    askRemove?.let { p ->
        val c = Stack.colors
        val n = model.pages.indexOf(p) + 1
        AlertDialog(
            onDismissRequest = { askRemove = null },
            title = { Text("Remove page $n?") },
            text = { Text("Scan deletes its copy of the photo. A photo picked from the gallery stays there; a camera shot is gone.") },
            confirmButton = { TextButton({ model.remove(p); askRemove = null }) { Text("Remove") } },
            dismissButton = { TextButton({ askRemove = null }) { Text("Cancel") } },
            containerColor = c.panel,
        )
    }
}

@Composable
private fun Thumb(p: Page, modifier: Modifier) {
    val c = Stack.colors
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(c.mat), contentAlignment = Alignment.Center) {
        p.thumb?.let { Image(it, null, Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit) }
    }
}

private fun two(i: Int) = String.format(Locale.ROOT, "%02d", i)

/** Phone: the pages as a row of thumbnails; tap one to open it. */
@Composable
private fun PageStrip(model: ScanModel) {
    val c = Stack.colors
    val state = rememberLazyListState()
    LaunchedEffect(model.sel) { if (model.sel >= 0) state.animateScrollToItem(maxOf(0, model.sel - 1)) }
    LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(model.pages, key = { _, p -> p.id }) { i, p ->
            val on = i == model.sel
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier.size(64.dp, 80.dp).clip(shape).background(c.mat)
                    .border(if (on) 2.dp else 1.dp, if (on) c.accent else c.line, shape)
                    .clickable { model.select(i) }
                    .semantics { contentDescription = "Page ${i + 1}" + if (on) ", open" else "" },
            ) {
                p.thumb?.let { Image(it, null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit) }
                Text(
                    two(i + 1),
                    Modifier.align(Alignment.TopStart).padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
                    style = StackType.mono.copy(fontSize = 10.5.sp), color = MatInk.text,
                )
            }
        }
    }
}

/** Phone: the open page's place, filter and its own buttons, under the strip. */
@Composable
private fun PageRow(model: ScanModel, onRemove: (Page) -> Unit) {
    val c = Stack.colors
    val p = model.current ?: return
    val i = model.sel
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Page ${i + 1} of ${model.pages.size}", style = StackType.mono, color = c.muted)
        Chip(p.filter.label, color = filterColor(p.filter))
        Spacer(Modifier.weight(1f))
        StackButton("Move left", { model.move(i, i - 1) }, small = true, enabled = i > 0)
        StackButton("Move right", { model.move(i, i + 1) }, small = true, enabled = i < model.pages.size - 1)
        StackButton("Remove", { onRemove(p) }, small = true)
    }
}

/** From 600 dp: the pages as a list beside the mat, each with its own buttons. */
@Composable
private fun PageList(model: ScanModel, modifier: Modifier, onRemove: (Page) -> Unit) {
    val c = Stack.colors
    val shape = RoundedCornerShape(12.dp)
    val state = rememberLazyListState()
    LaunchedEffect(model.sel) { if (model.sel >= 0) state.animateScrollToItem(maxOf(0, model.sel - 1)) }
    Column(modifier.clip(shape).background(c.panel).border(1.dp, c.line, shape)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            FLabel("Pages", Modifier.weight(1f))
            if (model.pages.isNotEmpty()) FLabel("tap to open")
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        if (model.pages.isEmpty()) {
            Text("No pages yet. Take or add photos of receipts or documents, in any order.",
                Modifier.padding(12.dp), style = StackType.note, color = c.muted)
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            state = state,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(model.pages, key = { _, p -> p.id }) { i, p ->
                val on = i == model.sel
                val s = RoundedCornerShape(8.dp)
                Row(
                    Modifier.fillMaxWidth().clip(s).background(if (on) c.accentSoft else Color.Transparent)
                        .border(1.dp, if (on) c.accent else Color.Transparent, s)
                        .clickable { model.select(i) }.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Thumb(p, Modifier.size(73.dp, 90.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("PAGE ${two(i + 1)}", style = StackType.mono, color = c.muted)
                        Text(p.name, style = StackType.note, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Chip(p.filter.label, color = filterColor(p.filter))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StackButton("Up", { model.move(i, i - 1) }, small = true, enabled = i > 0)
                            StackButton("Down", { model.move(i, i + 1) }, small = true, enabled = i < model.pages.size - 1)
                            StackButton("Remove", { onRemove(p) }, small = true)
                        }
                    }
                }
            }
        }
    }
}

/** The toolbar of the view, the mat, and the row that moves on. */
@Composable
private fun Editor(
    model: ScanModel, wide: Boolean, modifier: Modifier,
    onTake: () -> Unit, onPick: () -> Unit, onExport: () -> Unit,
) {
    val p = model.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (p != null) Toolbar(model, p)
        Stage(model, Modifier.fillMaxWidth().weight(1f), onTake, onPick)
        if (p != null) NavRow(model, wide, onExport)
    }
}

@Composable
private fun Toolbar(model: ScanModel, p: Page) {
    val c = Stack.colors
    val crop = model.mode == Mode.Crop
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Seg(Mode.entries, model.mode, model::chooseMode, { if (it == Mode.Crop) "Crop" else "Filter & review" })
            if (crop) {
                StackButton("Auto-detect edges", model::autoDetect, small = true, enabled = !model.detecting)
                StackButton("Use whole photo", model::wholePhoto, small = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Crosshair snap", style = StackType.note, color = c.ink)
                    Switch(model.snap, { model.toggleSnap() })
                }
            } else {
                StackButton("Turn left", { model.turn(-1) }, small = true)
                StackButton("Turn right", { model.turn(1) }, small = true)
            }
        }
        if (!crop) {
            Seg(Filter.entries, p.filter, model::chooseFilter, { it.label })
            val fc = filterColor(p.filter)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Swatch(fc)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = fc, fontWeight = FontWeight.SemiBold)) { append("${p.filter.label}: ") }
                        append(p.filter.desc)
                    },
                    style = StackType.sub, color = c.muted,
                )
            }
        }
    }
}

@Composable
private fun NavRow(model: ScanModel, wide: Boolean, onExport: () -> Unit) {
    val c = Stack.colors
    val i = model.sel
    val n = model.pages.size
    val last = i == n - 1
    val crop = model.mode == Mode.Crop
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StackButton("Previous", { model.select(i - 1) }, enabled = i > 0)
        if (wide) Text("${i + 1} / $n", style = StackType.mono, color = c.muted)
        StackButton("Next", { model.select(i + 1) }, enabled = i < n - 1)
        Spacer(Modifier.weight(1f))
        StackButton(
            when { crop -> "Apply crop"; last -> "Export PDF"; else -> "Next page" },
            {
                when {
                    crop -> model.applyCrop()
                    last -> onExport()
                    else -> model.select(i + 1, Mode.Crop)
                }
            },
            primary = true,
        )
    }
}
