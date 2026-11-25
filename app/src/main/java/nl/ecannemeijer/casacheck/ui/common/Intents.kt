package nl.ecannemeijer.casacheck.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import nl.ecannemeijer.casacheck.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/** Small helpers for intents (share, open, print, maps, phone, e-mail). Only standard Android intents; no Google Play Services needed. */
object Intents {
    fun uriFor(ctx: Context, f: File): Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)

    private fun start(ctx: Context, i: Intent) {
        try { ctx.startActivity(i) } catch (_: ActivityNotFoundException) { Toast.makeText(ctx, R.string.no_app_found, Toast.LENGTH_LONG).show() }
    }

    fun share(ctx: Context, f: File, mime: String, subject: String? = null) {
        val uri = uriFor(ctx, f)
        val i = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        subject?.let { i.putExtra(Intent.EXTRA_SUBJECT, it) }
        i.clipData = ClipData.newRawUri(f.name, uri)
        start(ctx, Intent.createChooser(i, ctx.getString(R.string.share)))
    }

    fun shareText(ctx: Context, text: String) = start(ctx, Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), ctx.getString(R.string.share)))

    fun open(ctx: Context, f: File, mime: String) =
        start(ctx, Intent(Intent.ACTION_VIEW).setDataAndType(uriFor(ctx, f), mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))

    fun url(ctx: Context, url: String) = start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    fun dial(ctx: Context, phone: String) = start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))
    fun email(ctx: Context, to: String, subject: String?) = start(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(to))).apply { subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) } })

    /** Opens the maps app on a location or a search near the property. Distances are never computed by CasaCheck itself. */
    fun mapsSearch(ctx: Context, lat: Double?, lng: Double?, query: String) {
        val q = Uri.encode(query)
        val uri = if (lat != null && lng != null) "geo:$lat,$lng?q=$q" else "geo:0,0?q=$q"
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        if (i.resolveActivity(ctx.packageManager) != null) start(ctx, i)
        else url(ctx, "https://www.google.com/maps/search/?api=1&query=$q")
    }

    fun mapsLocation(ctx: Context, lat: Double?, lng: Double?, address: String?) {
        if (lat != null && lng != null) mapsSearch(ctx, lat, lng, "$lat,$lng") else if (!address.isNullOrBlank()) mapsSearch(ctx, null, null, address)
    }

    fun mapsDirections(ctx: Context, origin: String, destination: String) =
        url(ctx, "https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(origin)}&destination=${Uri.encode(destination)}")

    fun print(ctx: Context, f: File, jobName: String) {
        val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
        pm.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(old: PrintAttributes?, new: PrintAttributes?, cancel: CancellationSignal?, cb: LayoutResultCallback, extras: Bundle?) {
                if (cancel?.isCanceled == true) { cb.onLayoutCancelled(); return }
                cb.onLayoutFinished(PrintDocumentInfo.Builder(f.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
            }
            override fun onWrite(pages: Array<out PageRange>?, dest: ParcelFileDescriptor, cancel: CancellationSignal?, cb: WriteResultCallback) {
                try {
                    f.inputStream().use { i -> FileOutputStream(dest.fileDescriptor).use { o -> i.copyTo(o) } }
                    cb.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) { cb.onWriteFailed(e.message) }
            }
        }, null)
    }
}

/** Text-to-speech in Italian (for reading questions aloud to the agent). Uses the device's TTS engine. */
class Speaker(ctx: Context) {
    private var ready = false
    private val tts = TextToSpeech(ctx.applicationContext) { status -> ready = status == TextToSpeech.SUCCESS }
    fun speak(text: String, locale: Locale = Locale.ITALIAN): Boolean {
        if (!ready) return false
        val r = tts.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "casacheck")
        return true
    }
    fun stop() = tts.stop()
    fun shutdown() = tts.shutdown()
}

@Composable
fun rememberSpeaker(): Speaker {
    val ctx = LocalContext.current
    val s = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { s.shutdown() } }
    return s
}
