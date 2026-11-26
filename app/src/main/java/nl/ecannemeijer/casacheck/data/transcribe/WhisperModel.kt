package nl.ecannemeijer.casacheck.data.transcribe

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import nl.ecannemeijer.casacheck.data.files.FileStore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * The speech model is not shipped in the APK: it is downloaded once on first use (Hugging Face, ggml format)
 * and verified by size and SHA-256 before use. Stored in noBackupFilesDir (not part of backups).
 */
class WhisperModel(private val ctx: Context) {
    data class Spec(val name: String, val url: String, val bytes: Long, val sha256: String)

    class NeedsWifiException : IOException("model download needs Wi-Fi")

    val spec = BASE_Q5_1
    val file: File get() = File(File(ctx.noBackupFilesDir, "whisper").apply { mkdirs() }, spec.name)
    fun isPresent(): Boolean = file.isFile && file.length() == spec.bytes

    private fun onWifi(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || !cm.isActiveNetworkMetered
    }

    /** Downloads (resuming a partial file) and verifies the model. [onProgress] gets 0..1. */
    suspend fun ensure(wifiOnly: Boolean, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        if (isPresent()) return@withContext file
        if (wifiOnly && !onWifi()) throw NeedsWifiException()
        val part = File(file.parentFile, spec.name + ".part")
        if (part.length() > spec.bytes) part.delete()
        val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
        val start = part.length()
        val req = Request.Builder().url(spec.url).apply { if (start > 0) header("Range", "bytes=$start-") }.build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val append = start > 0 && resp.code == 206
            if (!append) part.delete()
            val body = resp.body ?: throw IOException("empty body")
            var done = if (append) start else 0L
            java.io.FileOutputStream(part, append).use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(128 * 1024)
                    var last = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf); if (n < 0) break
                        out.write(buf, 0, n); done += n
                        if (done > spec.bytes) throw IOException("model larger than expected")
                        if (done - last > 512 * 1024) { onProgress(done.toFloat() / spec.bytes); last = done }
                    }
                }
            }
        }
        if (part.length() != spec.bytes || !FileStore.sha256(part).equals(spec.sha256, ignoreCase = true)) {
            part.delete(); throw IOException("model checksum mismatch")
        }
        onProgress(1f)
        if (!part.renameTo(file)) throw IOException("cannot store model")
        file
    }

    fun delete() { file.delete(); File(file.parentFile, spec.name + ".part").delete() }

    companion object {
        /** Multilingual "base" model, 5-bit quantised: 57 MB, good for Italian/Dutch/English conversation. */
        val BASE_Q5_1 = Spec(
            name = "ggml-base-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
            bytes = 59_707_625L,
            sha256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
        )
    }
}