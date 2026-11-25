package nl.ecannemeijer.casacheck.data.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import nl.ecannemeijer.casacheck.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: ReleaseInfo) : UpdateState
    data class Downloading(val info: ReleaseInfo, val progress: Float?) : UpdateState
    data class ReadyToInstall(val info: ReleaseInfo, val file: File) : UpdateState
    data class Failed(val reason: Reason, val detail: String? = null) : UpdateState
    enum class Reason { NETWORK, VERIFY, NEEDS_PERMISSION }
}

/**
 * Self-updater for the sideloaded APK: checks the public GitHub "latest release" (no token), downloads the APK asset,
 * verifies that its signing certificate equals the installed app's, and hands it to the system installer.
 * Android always asks the user to confirm a sideload install.
 */
class UpdateManager(private val context: Context) {
    companion object {
        const val LATEST_URL = "https://api.github.com/repos/ecannemeijer/casacheck/releases/latest"
        const val MIN_INTERVAL_MS = 6 * 3600_000L
        private const val PREFS = "updater"
        private const val KEY_LAST = "last_check"
        private const val KEY_SKIPPED = "skipped_tag"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    /** Automatic check (at most every 6 h, silent on errors) or a manual one ([manual] = true shows the result). */
    suspend fun check(manual: Boolean) {
        if (BuildConfig.DEBUG) { if (manual) _state.value = UpdateState.UpToDate; return }
        val s = _state.value
        if (s is UpdateState.Downloading || s is UpdateState.Checking) return
        val now = System.currentTimeMillis()
        if (!manual && now - prefs.getLong(KEY_LAST, 0) < MIN_INTERVAL_MS) return
        if (manual) _state.value = UpdateState.Checking
        val info = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(LATEST_URL).header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "CasaCheck/${BuildConfig.VERSION_NAME}").build()).execute().use { r ->
                    if (!r.isSuccessful) null else UpdateLogic.parseRelease(r.body?.string().orEmpty())
                }
            }
        }
        if (info.isFailure) { _state.value = if (manual) UpdateState.Failed(UpdateState.Reason.NETWORK, info.exceptionOrNull()?.message) else UpdateState.Idle; return }
        prefs.edit().putLong(KEY_LAST, now).apply()
        val rel = info.getOrNull()
        _state.value = when {
            rel != null && UpdateLogic.isNewer(rel.tag, BuildConfig.VERSION_NAME) && (manual || prefs.getString(KEY_SKIPPED, null) != rel.tag) -> UpdateState.Available(rel)
            manual -> UpdateState.UpToDate
            else -> UpdateState.Idle
        }
    }

    fun later(info: ReleaseInfo) { prefs.edit().putString(KEY_SKIPPED, info.tag).apply(); _state.value = UpdateState.Idle }
    fun dismiss() { _state.value = UpdateState.Idle }

    suspend fun download(info: ReleaseInfo) {
        _state.value = UpdateState.Downloading(info, null)
        val dir = File(context.cacheDir, "share/updates").apply { deleteRecursively(); mkdirs() }
        val out = File(dir, "CasaCheck-${info.tag.removePrefix("v")}.apk")
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(info.apk.url).header("User-Agent", "CasaCheck/${BuildConfig.VERSION_NAME}").build()).execute().use { r ->
                    if (!r.isSuccessful) error("HTTP ${r.code}")
                    val body = r.body ?: error("empty")
                    val total = body.contentLength().takeIf { it > 0 } ?: info.apk.size.takeIf { it > 0 }
                    var done = 0L; var lastEmit = 0L
                    body.byteStream().use { input -> out.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf); if (n < 0) break
                            o.write(buf, 0, n); done += n
                            if (total != null && done - lastEmit > 256 * 1024) { lastEmit = done; _state.value = UpdateState.Downloading(info, done.toFloat() / total) }
                        }
                    } }
                }
            }
        }
        if (ok.isFailure) { out.delete(); _state.value = UpdateState.Failed(UpdateState.Reason.NETWORK, ok.exceptionOrNull()?.message); return }
        if (!verify(out)) { out.delete(); _state.value = UpdateState.Failed(UpdateState.Reason.VERIFY); return }
        _state.value = UpdateState.ReadyToInstall(info, out)
    }

    /** Same package, higher versionCode and the same signing certificate(s) as the installed app. */
    fun verify(apk: File): Boolean = runCatching {
        val pm = context.packageManager
        val archive = archiveInfo(pm, apk) ?: return false
        if (archive.packageName != context.packageName) return false
        val installed = installedInfo(pm)
        if (versionCode(archive) <= versionCode(installed)) return false
        val a = certHashes(archive); val i = certHashes(installed)
        a.isNotEmpty() && a == i
    }.getOrDefault(false)

    /** Starts the system installer, or the "install unknown apps" setting when that is not allowed yet. */
    private var pending: ReadyToInstallData? = null
    private data class ReadyToInstallData(val info: ReleaseInfo, val file: File)

    /** Call on resume: continues an install that was waiting for the "unknown apps" permission. */
    fun onResume() {
        val p = pending ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
            pending = null
            if (p.file.exists()) _state.value = UpdateState.ReadyToInstall(p.info, p.file)
        }
    }

    fun install(file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            (state.value as? UpdateState.ReadyToInstall)?.let { pending = ReadyToInstallData(it.info, it.file) }
            _state.value = UpdateState.Failed(UpdateState.Reason.NEEDS_PERMISSION)
            return false
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { context.startActivity(i); true } catch (e: ActivityNotFoundException) { false }
    }

    fun openUnknownSourcesSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, f: File): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = pm.getPackageArchiveInfo(f.absolutePath, flags) ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo == null) return pm.getPackageArchiveInfo(f.absolutePath, PackageManager.GET_SIGNATURES)
        return info
    }

    @Suppress("DEPRECATION")
    private fun installedInfo(pm: PackageManager): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        else pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)

    @Suppress("DEPRECATION")
    private fun versionCode(p: PackageInfo): Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) p.longVersionCode else p.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun certHashes(p: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && p.signingInfo != null) {
            val si = p.signingInfo!!
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else p.signatures
        return sigs.orEmpty().map { s -> MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }
}