package nl.ecannemeijer.casacheck.data.translate

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Outcome of a translation attempt; errors are typed so the UI can show a clear Dutch/English/Italian message. */
sealed interface TranslateResult {
    data class Ok(val text: String, val sourceLang: String) : TranslateResult
    /** The text is already in the target language; nothing to translate. */
    data object AlreadyTarget : TranslateResult
    /** The language model is not on the device and only Wi-Fi downloads are allowed. */
    data object NeedsWifi : TranslateResult
    /** The model could not be downloaded (no internet, storage, ...). */
    data class DownloadFailed(val message: String?) : TranslateResult
    data class Failed(val message: String?) : TranslateResult
}

/** Progress stages for the UI (ML Kit gives no byte-level progress for model downloads). */
enum class TranslateStage { DETECTING, DOWNLOADING_MODEL, TRANSLATING }

/**
 * On-device translation with ML Kit. Models (~30 MB per language) are downloaded once on first use;
 * after that translation works fully offline. Text never leaves the device.
 */
class TextTranslator(private val context: Context) {

    suspend fun isModelDownloaded(lang: String): Boolean = runCatching {
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel::class.java).await().any { it.language == lang }
    }.getOrDefault(false)

    private fun onWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || !cm.isActiveNetworkMetered
    }

    /** Detects the language; falls back to Italian when unsure (listings are Italian). */
    suspend fun detect(text: String): String {
        val sample = text.take(2000)
        val id = LanguageIdentification.getClient()
        val tag = try { runCatching { id.identifyLanguage(sample).await() }.getOrNull() } finally { id.close() }
        return TranslateLanguage.fromLanguageTag(tag ?: "und") ?: TranslateLanguage.ITALIAN
    }

    suspend fun translate(text: String, target: String = TranslateLanguage.DUTCH, wifiOnly: Boolean = false, onStage: (TranslateStage) -> Unit = {}): TranslateResult {
        if (text.isBlank()) return TranslateResult.Ok("", TranslateLanguage.ITALIAN)
        onStage(TranslateStage.DETECTING)
        val source = detect(text)
        if (source == target) return TranslateResult.AlreadyTarget
        val needed = listOf(source, target).filter { it != TranslateLanguage.ENGLISH }
        val missing = needed.filterNot { isModelDownloaded(it) }
        val translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build())
        try {
            if (missing.isNotEmpty()) {
                if (wifiOnly && !onWifi()) return TranslateResult.NeedsWifi
                onStage(TranslateStage.DOWNLOADING_MODEL)
                val cond = DownloadConditions.Builder().apply { if (wifiOnly) requireWifi() }.build()
                try { translator.downloadModelIfNeeded(cond).await() } catch (e: Exception) { return TranslateResult.DownloadFailed(e.message) }
            }
            onStage(TranslateStage.TRANSLATING)
            val out = TranslationText.translateLines(text) { p -> translator.translate(p).await() }
            return TranslateResult.Ok(out, source)
        } catch (e: Exception) {
            return TranslateResult.Failed(e.message)
        } finally {
            translator.close()
        }
    }
}

/** Pure helpers (no Android/ML Kit), unit-tested on the JVM. */
object TranslationText {
    /** Translates line by line so the layout (paragraphs, bullet lists) of the original is kept. Lines without letters are copied. */
    suspend fun translateLines(text: String, tr: suspend (String) -> String): String =
        text.lines().map { line -> if (line.isBlank() || line.none { it.isLetter() }) line else tr(line) }.joinToString("\n")

    /** Stable hash of the source text, used to invalidate cached translations when the listing text changes. */
    fun hash(text: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(text.trim().toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
}
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}