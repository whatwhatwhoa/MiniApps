package io.github.whatwhatwhoa.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.math.max

/** A published release of this app: its version and its APK on GitHub. */
data class Release(
    val version: String,
    val apkUrl: String,
    val apkName: String,
    val size: Long,
    val page: String,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    /** [latest] is the newest release of this app on GitHub, null when none is published yet. */
    data class UpToDate(val latest: String?) : UpdateState
    data class Available(val release: Release, val note: String? = null) : UpdateState
    data class Downloading(val release: Release, val done: Long, val total: Long) : UpdateState
    data class Installing(val release: Release) : UpdateState
    data class Failed(val message: String, val release: Release?) : UpdateState
}

/**
 * Updates from the series' GitHub releases. Releases there are tagged `<app>-v<version>` (the app
 * is the last part of the package name) and carry the signed APK, so an app finds its own without
 * any setting. Android's installer does the install and asks the user first; it only accepts an
 * APK signed with the installed app's key, which [update] checks before handing it over.
 */
object Updates {
    const val REPO = "whatwhatwhoa/MiniApps"
    private const val DAY_MS = 24 * 3_600_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    fun app(context: Context): String = context.packageName.substringAfterLast('.')

    fun installed(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    private fun prefs(context: Context) = context.getSharedPreferences("update", Context.MODE_PRIVATE)

    fun autoCheck(context: Context): Boolean = prefs(context).getBoolean("auto", true)

    fun setAutoCheck(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("auto", on).apply()
    }

    /** When the last check that reached GitHub was, 0 for never. */
    fun lastCheck(context: Context): Long = prefs(context).getLong("last", 0)

    /** The version the banner was dismissed for ("Not now"); the Settings card still offers it. */
    fun skipped(context: Context): String? = prefs(context).getString("skip", null)

    fun skip(context: Context, version: String) {
        prefs(context).edit().putString("skip", version).apply()
    }

    /**
     * When the app opens: at most once a day, and quiet (a failed check shows nothing). Between
     * checks, a newer release found earlier is offered again from what was stored.
     */
    fun checkDaily(context: Context) {
        val ctx = context.applicationContext
        if (_state.value != UpdateState.Idle) return
        stored(ctx)?.let { if (newer(it.version, installed(ctx))) _state.value = UpdateState.Available(it) }
        if (!autoCheck(ctx)) return
        if (System.currentTimeMillis() - lastCheck(ctx) < DAY_MS) return
        check(ctx, quiet = true)
    }

    /** Ask GitHub now. [quiet]: no "Checking", and a failure leaves the state as it was. */
    fun check(context: Context, quiet: Boolean = false) {
        val ctx = context.applicationContext
        val now = _state.value
        if (now is UpdateState.Checking || now is UpdateState.Downloading || now is UpdateState.Installing) return
        if (!quiet) _state.value = UpdateState.Checking
        scope.launch {
            try {
                val latest = latest(ctx)
                store(ctx, latest)
                _state.value = if (latest != null && newer(latest.version, installed(ctx))) UpdateState.Available(latest)
                else UpdateState.UpToDate(latest?.version)
            } catch (e: Exception) {
                if (!quiet) _state.value = UpdateState.Failed(say(e), null)
                else if (_state.value == UpdateState.Checking) _state.value = UpdateState.Idle
            }
        }
    }

    /** Download [release], check it is this app with this app's key, and start the install. */
    fun update(context: Context, release: Release) {
        val ctx = context.applicationContext
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            // Android asks once per app: the settings page has one switch, then back here.
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            _state.value = UpdateState.Available(release, "Turn on \"Allow from this source\", come back, then tap Update again.")
            return
        }
        val now = _state.value
        if (now is UpdateState.Downloading || now is UpdateState.Installing) return
        _state.value = UpdateState.Downloading(release, 0, release.size)
        scope.launch {
            try {
                val file = download(ctx, release)
                verify(ctx, file)
                _state.value = UpdateState.Installing(release)
                install(ctx, file)
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(say(e), release)
            }
        }
    }

    /** From [InstallReceiver]: the installer's answer when it isn't a success or a question. */
    internal fun installFailed(message: String) {
        val r = (_state.value as? UpdateState.Installing)?.release
        _state.value = UpdateState.Failed(message, r)
    }

    /** The user said no in the installer's dialog: offer the update again. */
    internal fun installCancelled() {
        (_state.value as? UpdateState.Installing)?.let { _state.value = UpdateState.Available(it.release) }
    }

    /** a > b, comparing the numbers in "0.2.0"-style versions part by part. */
    fun newer(a: String, b: String): Boolean {
        val x = parts(a)
        val y = parts(b)
        for (i in 0 until max(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    private fun parts(v: String) = v.split('.', '-').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    private fun open(url: String, ctx: Context): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            // GitHub wants a User-Agent; the app's name is all it gets.
            setRequestProperty("User-Agent", "MiniApps-${app(ctx)}")
        }

    /** The newest release tagged `<app>-v…` with an APK, null when there is none. */
    private fun latest(ctx: Context): Release? {
        val prefix = app(ctx) + "-v"
        val conn = open("https://api.github.com/repos/$REPO/releases?per_page=100", ctx)
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            when (val code = conn.responseCode) {
                200 -> {}
                403, 429 -> throw IOException("GitHub's limit for checks was reached; try again in an hour.")
                else -> throw IOException("GitHub answered $code.")
            }
            val list = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            var best: Release? = null
            for (i in 0 until list.length()) {
                val o = list.getJSONObject(i)
                if (o.optBoolean("draft") || o.optBoolean("prerelease")) continue
                val tag = o.optString("tag_name")
                if (!tag.startsWith(prefix)) continue
                val assets = o.optJSONArray("assets") ?: continue
                val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") } ?: continue
                val r = Release(
                    tag.removePrefix(prefix), apk.getString("browser_download_url"), apk.getString("name"),
                    apk.optLong("size"), o.optString("html_url"),
                )
                if (best == null || newer(r.version, best.version)) best = r
            }
            return best
        } finally {
            conn.disconnect()
        }
    }

    private fun download(ctx: Context, release: Release): File {
        val dir = File(ctx.cacheDir, "update").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "update.apk")
        val conn = open(release.apkUrl, ctx)
        try {
            if (conn.responseCode != 200) throw IOException("The download answered ${conn.responseCode}.")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.size
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var shown = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (done - shown >= 128 * 1024) {
                            _state.value = UpdateState.Downloading(release, done, total)
                            shown = done
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return file
    }

    /** The installer would refuse another app or another key with a vague message: say it plainly. */
    private fun verify(ctx: Context, file: File) {
        val pm = ctx.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val got = pm.getPackageArchiveInfo(file.path, flags)
            ?: throw IOException("The download isn't a whole app; try again.")
        if (got.packageName != ctx.packageName) throw IOException("The download is another app (${got.packageName}).")
        val mine = pm.getPackageInfo(ctx.packageName, flags).signingInfo?.apkContentsSigners?.toSet()
        val theirs = got.signingInfo?.apkContentsSigners?.toSet()
        if (mine == null || theirs == null || mine != theirs) {
            throw IOException("The release is signed with another key than this copy of the app, so Android " +
                "won't install it over it. Uninstall this copy first, then install the release from GitHub.")
        }
    }

    private fun install(ctx: Context, file: File) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(ctx.packageName)
            setSize(file.length())
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("update.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // Mutable: the installer adds its status and, first, the confirmation to show.
            val intent = Intent(ctx, InstallReceiver::class.java).setPackage(ctx.packageName)
            val pending = PendingIntent.getBroadcast(
                ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
        }
    }

    private fun store(ctx: Context, r: Release?) {
        val e = prefs(ctx).edit().putLong("last", System.currentTimeMillis())
        if (r == null) e.remove("version") else {
            e.putString("version", r.version).putString("url", r.apkUrl).putString("name", r.apkName)
                .putLong("size", r.size).putString("page", r.page)
        }
        e.apply()
    }

    private fun stored(ctx: Context): Release? {
        val p = prefs(ctx)
        val v = p.getString("version", null) ?: return null
        return Release(v, p.getString("url", null) ?: return null, p.getString("name", "") ?: "",
            p.getLong("size", 0), p.getString("page", "") ?: "")
    }

    private fun say(e: Exception): String = when (e) {
        is UnknownHostException -> "No connection to GitHub."
        is SocketTimeoutException -> "GitHub didn't answer in time; try again."
        is IOException -> e.message ?: "The connection failed."
        else -> e.message ?: e.javaClass.simpleName
    }
}
