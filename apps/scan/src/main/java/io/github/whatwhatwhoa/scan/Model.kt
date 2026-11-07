package io.github.whatwhatwhoa.scan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

enum class Mode { Crop, Result }

/** One page: its photo (a JPEG in the app's files, [w] × [h]), its crop, filter and turn. */
class Page(val id: Long, val name: String, val w: Int, val h: Int, quad: Quad, filter: Filter, rot: Int) {
    var quad by mutableStateOf(quad)
    var filter by mutableStateOf(filter)
    var rot by mutableIntStateOf(rot)
    var thumb by mutableStateOf<ImageBitmap?>(null)
    val look: Look get() = Look(quad, filter, ((rot % 4) + 4) % 4)
}

/** The selected page's photo, and that page processed for one [Look]. */
class Loaded(val id: Long, val bitmap: ImageBitmap)
class Processed(val id: Long, val look: Look, val bitmap: ImageBitmap)

sealed interface ExportState {
    data class Working(val text: String) : ExportState
    data class Done(val text: String) : ExportState
    data class Failed(val text: String) : ExportState
}

/**
 * The scan: its pages in order, the selection, and the work on them. One per process (an
 * activity opened from a share uses the same), kept in the app's files so pages survive the
 * camera app, a turn of the phone and the app being closed. Heavy work runs one job at a time
 * on its own thread, so two photos are never in memory at once by accident.
 */
class ScanModel private constructor(private val app: Context) {
    companion object {
        // Holds only the application context, which lives as long as the process anyway.
        @SuppressLint("StaticFieldLeak")
        @Volatile private var instance: ScanModel? = null

        fun get(context: Context): ScanModel =
            instance ?: synchronized(this) { instance ?: ScanModel(context.applicationContext).also { instance = it } }

        const val AUTHORITY_SUFFIX = ".files"
    }

    private val dir = File(app.filesDir, "pages").apply { mkdirs() }
    private val listFile = File(dir, "pages.json")
    private val prefs = app.getSharedPreferences("scan", Context.MODE_PRIVATE)
    private val heavy = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val pages = mutableStateListOf<Page>()
    var sel by mutableIntStateOf(-1)
        private set
    var mode by mutableStateOf(Mode.Crop)
        private set
    var snap by mutableStateOf(prefs.getBoolean("snap", true))
        private set
    var pageSize by mutableStateOf(PageSize.of(prefs.getString("pageSize", null)))
        private set
    var quality by mutableStateOf(Quality.of(prefs.getString("quality", null)))
        private set

    /** "Adding 2 of 5…" while photos come in. */
    var importing by mutableStateOf<String?>(null)
        private set
    /** True while auto-detect runs on the selected page. */
    var detecting by mutableStateOf(false)
        private set
    /** A short note for the owner (a photo that couldn't be read); the page clears it after a while. */
    var message by mutableStateOf<String?>(null)
    var source by mutableStateOf<Loaded?>(null)
        private set
    var result by mutableStateOf<Processed?>(null)
        private set
    var export by mutableStateOf<ExportState?>(null)
        private set

    private var nextId = 1L
    private var sourceJob: Job? = null
    private var resultJob: Job? = null
    private var exportJob: Job? = null

    val current: Page? get() = pages.getOrNull(sel)

    init {
        load()
        scope.launch {
            for (p in pages.toList()) {
                val t = withContext(heavy) { runCatching { thumbFromFile(p) }.getOrNull() }
                if (t != null) p.thumb = t
            }
        }
        ensureSource()
    }

    private fun file(id: Long) = File(dir, "$id.jpg")

    // ---------- pages ----------

    fun select(i: Int, to: Mode? = null) {
        sel = if (pages.isEmpty()) -1 else i.coerceIn(0, pages.size - 1)
        if (to != null) mode = to
        save()
        ensureSource()
        if (mode == Mode.Result) ensureResult()
    }

    fun chooseMode(m: Mode) {
        mode = m
        if (m == Mode.Result) ensureResult()
    }

    fun move(from: Int, to: Int) {
        if (to < 0 || to >= pages.size || from == to) return
        val p = pages.removeAt(from)
        pages.add(to, p)
        sel = when {
            sel == from -> to
            sel in (from + 1)..to -> sel - 1
            sel in to until from -> sel + 1
            else -> sel
        }
        save()
    }

    fun remove(p: Page) {
        val i = pages.indexOf(p)
        if (i < 0) return
        pages.removeAt(i)
        file(p.id).delete()
        if (sel >= pages.size) sel = pages.size - 1 else if (i < sel) sel--
        if (source?.id == p.id) source = null
        if (result?.id == p.id) result = null
        if (pages.isEmpty()) mode = Mode.Crop
        save()
        ensureSource()
        if (mode == Mode.Result) ensureResult()
    }

    fun clearAll() {
        pages.clear()
        dir.listFiles()?.forEach { if (it.name.endsWith(".jpg")) it.delete() }
        sel = -1; mode = Mode.Crop; source = null; result = null; export = null
        save()
    }

    /** Bytes the pages' photos take in the app's files. */
    fun storedText(): String = mb(storedBytes())

    private fun storedBytes(): Long = dir.listFiles()?.filter { it.name.endsWith(".jpg") }?.sumOf { it.length() } ?: 0L

    // ---------- crop ----------

    fun moveCorner(p: Page, i: Int, pt: Pt) {
        val c = Pt(pt.x.coerceIn(0f, p.w.toFloat()), pt.y.coerceIn(0f, p.h.toFloat()))
        p.quad = p.quad.toMutableList().also { it[i] = c }
    }

    /** A drag has ended: keep the crop. */
    fun commitQuad() = save()

    fun wholePhoto() {
        val p = current ?: return
        p.quad = Detect.inset(p.w, p.h, 0f)
        save()
    }

    fun autoDetect() {
        val p = current ?: return
        if (detecting) return
        detecting = true
        scope.launch {
            val q = withContext(heavy) {
                runCatching {
                    val b = Imaging.load(file(p.id)) ?: return@runCatching null
                    val px = Imaging.pixels(b)
                    val r = Detect.detect(px, b.width, b.height)
                    b.recycle()
                    r
                }.getOrNull()
            }
            detecting = false
            if (q != null && pages.contains(p)) { p.quad = q; save() }
        }
    }

    fun toggleSnap() {
        snap = !snap
        prefs.edit { putBoolean("snap", snap) }
    }

    // ---------- filter and review ----------

    fun chooseFilter(f: Filter) {
        val p = current ?: return
        p.filter = f
        save()
        ensureResult()
    }

    fun turn(by: Int) {
        val p = current ?: return
        p.rot = (((p.rot + by) % 4) + 4) % 4
        save()
        ensureResult()
    }

    fun applyCrop() = chooseMode(Mode.Result)

    /** True while the selected page's photo or its processed result is on its way. */
    val working: Boolean
        get() {
            val p = current ?: return false
            if (detecting) return true
            return if (mode == Mode.Crop) source?.id != p.id
            else result.let { it == null || it.id != p.id || it.look != p.look }
        }

    private fun ensureSource() {
        val p = current ?: run { source = null; return }
        if (source?.id == p.id) return
        sourceJob?.cancel()
        sourceJob = scope.launch {
            val b = withContext(heavy) {
                runCatching { Imaging.load(file(p.id))?.also { it.setHasMipMap(true) } }.getOrNull()
            }
            if (b != null && current?.id == p.id) source = Loaded(p.id, b.asImageBitmap())
        }
    }

    private fun ensureResult() {
        val p = current ?: return
        val look = p.look
        val r = result
        if (r != null && r.id == p.id && r.look == look) return
        resultJob?.cancel()
        resultJob = scope.launch {
            val out = withContext(heavy) {
                runCatching {
                    val src = Imaging.load(file(p.id)) ?: return@runCatching null
                    val o = Imaging.process(src, look)
                    src.recycle()
                    val t = Imaging.shrink(o, Imaging.THUMB)
                    o.setHasMipMap(true)
                    o to t
                }.getOrNull()
            }
            if (out != null) {
                p.thumb = out.second.asImageBitmap()
                if (current?.id == p.id) result = Processed(p.id, look, out.first.asImageBitmap())
            }
        }
    }

    private fun thumbFromFile(p: Page): ImageBitmap? {
        val small = Imaging.loadSmall(file(p.id), maxOf(p.w, p.h), 2 * Imaging.THUMB) ?: return null
        val t = Imaging.thumb(small, p.w, p.look)
        small.recycle()
        return t.asImageBitmap()
    }

    // ---------- adding photos ----------

    fun addUris(uris: List<Uri>) {
        val list = uris.map { u -> ImageDecoder.createSource(app.contentResolver, u) to displayName(u) }
        addSources(list)
    }

    /** A photo from the camera, taken into [file] (cache); the file goes once it is in. */
    fun addCameraShot(file: File) {
        // Its own name first, so the next shot can't land in the file while this one comes in.
        val shot = File(file.parentFile, "shot-${System.currentTimeMillis()}.jpg")
        if (!file.renameTo(shot)) return
        val name = "Photo " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        addSources(listOf(ImageDecoder.createSource(shot) to name)) { shot.delete() }
    }

    private fun addSources(list: List<Pair<ImageDecoder.Source, String>>, after: () -> Unit = {}) {
        if (list.isEmpty()) return
        scope.launch {
            val first = pages.size
            var failed = 0
            for ((i, item) in list.withIndex()) {
                importing = if (list.size == 1) "Adding the photo…" else "Adding ${i + 1} of ${list.size}…"
                val id = nextId++
                val p = withContext(heavy) { runCatching { import(id, item.first, item.second) }.getOrNull() }
                if (p == null) failed++ else { pages.add(p); save() }
            }
            importing = null
            after()
            if (pages.size > first) select(first, Mode.Crop)
            if (failed > 0) message = if (failed == 1) "Couldn't read one photo." else "Couldn't read $failed photos."
        }
    }

    private fun import(id: Long, source: ImageDecoder.Source, name: String): Page {
        val b = Imaging.decode(source)
        FileOutputStream(file(id)).use { b.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        val quad = Detect.detect(Imaging.pixels(b), b.width, b.height)
        val p = Page(id, name, b.width, b.height, quad, Filter.Document, 0)
        val small = Imaging.shrink(b, 2 * Imaging.THUMB)
        p.thumb = Imaging.thumb(small, b.width, p.look).asImageBitmap()
        small.recycle()
        b.recycle()
        return p
    }

    private fun displayName(u: Uri): String =
        runCatching {
            app.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "Photo"

    // ---------- export ----------

    fun defaultName(): String = "scan-" + SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

    fun choosePageSize(s: PageSize) { pageSize = s; prefs.edit { putString("pageSize", s.key) } }
    fun chooseQuality(q: Quality) { quality = q; prefs.edit { putString("quality", q.key) } }

    fun clearExport() { if (exportJob?.isActive != true) export = null }

    /** The file name as saved: no characters a file system refuses, and ".pdf" on the end. */
    fun fileName(typed: String): String {
        var n = typed.trim().ifEmpty { "scan" }.replace(Regex("[\\\\/:*?\"<>|]+"), "-")
        if (!n.endsWith(".pdf", ignoreCase = true)) n += ".pdf"
        return n
    }

    /** Builds the PDF into [save] (a document the owner picked), or into the cache for [share]. */
    fun exportPdf(name: String, save: Uri?, share: ((Uri) -> Unit)?) {
        if (pages.isEmpty() || exportJob?.isActive == true) return
        val fname = fileName(name)
        val jobs = pages.map { file(it.id) to it.look }
        val size = pageSize; val q = quality
        exportJob = scope.launch {
            export = ExportState.Working("Rendering page 1 of ${jobs.size}…")
            try {
                if (save != null) {
                    val bytes = withContext(heavy) {
                        val os = app.contentResolver.openOutputStream(save, "wt") ?: error("the file couldn't be opened")
                        os.use { writePdf(it, jobs, size, q) }
                    }
                    export = ExportState.Done("Saved $fname (${mb(bytes)}).")
                } else {
                    val f = File(File(app.cacheDir, "pdf").apply { deleteRecursively(); mkdirs() }, fname)
                    val bytes = withContext(heavy) { FileOutputStream(f).use { writePdf(it, jobs, size, q) } }
                    export = ExportState.Done("Ready to share: $fname (${mb(bytes)}).")
                    share?.invoke(FileProvider.getUriForFile(app, app.packageName + AUTHORITY_SUFFIX, f))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                export = ExportState.Failed("Something went wrong building the PDF: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private suspend fun writePdf(os: OutputStream, jobs: List<Pair<File, Look>>, size: PageSize, q: Quality): Long {
        var bytes = 0L
        val counted = object : OutputStream() {
            override fun write(b: Int) { os.write(b); bytes++ }
            override fun write(b: ByteArray, off: Int, len: Int) { os.write(b, off, len); bytes += len }
            override fun flush() = os.flush()
        }
        val stamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(Date())
        val pdf = PdfWriter(counted, jobs.size, "Scan (Mini Apps)", stamp)
        for ((i, job) in jobs.withIndex()) {
            withContext(Dispatchers.Main) { export = ExportState.Working("Rendering page ${i + 1} of ${jobs.size}…") }
            val src = Imaging.load(job.first) ?: error("page ${i + 1}'s photo is missing")
            val o = Imaging.process(src, job.second)
            src.recycle()
            val jpeg = java.io.ByteArrayOutputStream().also { o.compress(Bitmap.CompressFormat.JPEG, q.jpeg, it) }.toByteArray()
            pdf.addPage(jpeg, o.width, o.height, Layout.place(size, o.width, o.height))
            o.recycle()
        }
        pdf.finish()
        return bytes
    }

    private fun mb(bytes: Long): String =
        if (bytes < 1_000_000) "${(bytes + 999) / 1000} KB" else String.format(Locale.getDefault(), "%.1f MB", bytes / 1_000_000.0)

    // ---------- saved state ----------

    private fun save() {
        val arr = JSONArray()
        for (p in pages) {
            val q = JSONArray()
            for (pt in p.quad) { q.put(pt.x.toDouble()); q.put(pt.y.toDouble()) }
            arr.put(JSONObject().put("id", p.id).put("name", p.name).put("w", p.w).put("h", p.h)
                .put("quad", q).put("filter", p.filter.key).put("rot", p.rot))
        }
        val text = JSONObject().put("sel", sel).put("mode", mode.name).put("pages", arr).toString()
        runCatching {
            val tmp = File(dir, "pages.json.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(listFile)) { listFile.delete(); tmp.renameTo(listFile) }
        }
    }

    private fun load() {
        val o = runCatching { JSONObject(listFile.readText()) }.getOrNull() ?: return
        val arr = o.optJSONArray("pages") ?: return
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val id = j.getLong("id")
            if (!file(id).exists()) continue
            val qa = j.getJSONArray("quad")
            val quad = List(4) { Pt(qa.getDouble(2 * it).toFloat(), qa.getDouble(2 * it + 1).toFloat()) }
            pages.add(Page(id, j.optString("name", "Photo"), j.getInt("w"), j.getInt("h"), quad,
                Filter.of(j.optString("filter")), j.optInt("rot")))
        }
        sel = if (pages.isEmpty()) -1 else o.optInt("sel", 0).coerceIn(0, pages.size - 1)
        nextId = (pages.maxOfOrNull { it.id } ?: 0L) + 1
        // A photo with no page (the app was stopped while it came in) is left over: tidy it away.
        val kept = pages.map { "${it.id}.jpg" }.toSet()
        dir.listFiles()?.forEach { if (it.name.endsWith(".jpg") && it.name !in kept) it.delete() }
        mode = runCatching { Mode.valueOf(o.optString("mode")) }.getOrDefault(Mode.Crop)
        if (mode == Mode.Result) ensureResult()
    }
}
