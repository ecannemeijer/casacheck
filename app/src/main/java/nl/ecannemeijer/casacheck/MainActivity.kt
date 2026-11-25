package nl.ecannemeijer.casacheck

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.data.importer.UrlRecognizer
import nl.ecannemeijer.casacheck.data.settings.AppSettings
import nl.ecannemeijer.casacheck.ui.nav.CasaNavHost
import nl.ecannemeijer.casacheck.ui.theme.CasaTheme

/** Content shared to CasaCheck from another app (Idealista, Immobiliare, Chrome, gallery). */
data class SharedPayload(val url: String? = null, val text: String? = null, val images: List<Uri> = emptyList()) {
    val isEmpty get() = url == null && text.isNullOrBlank() && images.isEmpty()
}

class MainActivity : AppCompatActivity() {
    private val shared = mutableStateOf<SharedPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) shared.value = parseShare(intent)
        val container = (application as CasaApp).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            CasaTheme(settings.theme) {
                CasaNavHost(container = container, shared = shared.value, onSharedConsumed = { shared.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        parseShare(intent)?.let { shared.value = it }
    }

    companion object {
        @Suppress("DEPRECATION")
        fun parseShare(intent: Intent?): SharedPayload? {
            intent ?: return null
            val images = mutableListOf<Uri>()
            when (intent.action) {
                Intent.ACTION_SEND -> {
                    val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    if (uri != null && intent.type?.startsWith("image/") == true) images += uri
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    val list: List<Uri>? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) else intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    list?.let { images += it.take(10) }
                }
                Intent.ACTION_VIEW -> intent.dataString?.let { return SharedPayload(url = it).takeIf { _ -> UrlRecognizer.isSupported(it) } }
                else -> return null
            }
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.take(50_000)
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val url = UrlRecognizer.extractUrl(text)?.takeIf { UrlRecognizer.isSupported(it) }
            val p = SharedPayload(url = url, text = listOfNotNull(subject, text).joinToString("\n").takeIf { url == null && it.isNotBlank() }, images = images)
            return p.takeIf { !it.isEmpty }
        }
    }
}
