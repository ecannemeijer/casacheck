package nl.ecannemeijer.casacheck.data.importer

import java.net.URI

enum class ListingSite(val id: String, val displayName: String) {
    IDEALISTA("idealista", "Idealista"),
    IMMOBILIARE("immobiliare", "Immobiliare.it"),
}

data class RecognizedUrl(val site: ListingSite, val listingId: String, val normalizedUrl: String, val language: String?)

/**
 * Validates and recognises supported listing URLs. Only https/http URLs of known hosts are accepted;
 * everything else is rejected before any network request is made.
 */
object UrlRecognizer {
    private val idealistaHosts = Regex("^(www\\.|m\\.)?idealista\\.(it|com|pt)$")
    private val immobiliareHosts = Regex("^(www\\.|m\\.)?immobiliare\\.it$")
    private val idealistaPath = Regex("^/(?:([a-z]{2})/)?(?:immobile|inmueble|imovel)/(\\d{4,12})/?.*$")
    private val immobiliarePath = Regex("^/(?:([a-z]{2})/)?annunci/(?:p-)?(\\d{4,12})/?.*$")
    private val urlInText = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)

    fun recognize(raw: String?): RecognizedUrl? {
        val uri = parse(raw) ?: return null
        val host = uri.host.lowercase()
        val path = uri.rawPath ?: return null
        if (idealistaHosts.matches(host)) {
            val m = idealistaPath.matchEntire(path) ?: return null
            val lang = m.groupValues[1].ifBlank { null }
            val id = m.groupValues[2]
            val segment = path.split('/').first { it in setOf("immobile", "inmueble", "imovel") }
            val normalized = "https://www.${host.removePrefix("www.").removePrefix("m.")}/" +
                (lang?.let { "$it/" } ?: "") + "$segment/$id/"
            return RecognizedUrl(ListingSite.IDEALISTA, id, normalized, lang)
        }
        if (immobiliareHosts.matches(host)) {
            val m = immobiliarePath.matchEntire(path) ?: return null
            val lang = m.groupValues[1].ifBlank { null }
            val id = m.groupValues[2]
            val normalized = "https://www.immobiliare.it/" + (lang?.let { "$it/" } ?: "") + "annunci/$id/"
            return RecognizedUrl(ListingSite.IMMOBILIARE, id, normalized, lang)
        }
        return null
    }

    /** Returns a syntactically valid http(s) URI without credentials, or null. */
    fun parse(raw: String?): URI? {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() && it.length < 2048 } ?: return null
        val uri = runCatching { URI(s) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null
        if (uri.host.isNullOrBlank() || uri.userInfo != null) return null
        return uri
    }

    fun isSupported(raw: String?) = recognize(raw) != null

    /** First URL inside a shared or pasted text (Android share sheets often send "Title https://..."). */
    fun extractUrl(text: String?): String? =
        text?.let { urlInText.find(it)?.value?.trimEnd('.', ',', ')', ';') }

    /** Hosts that are allowed for listing pages and redirects. */
    fun isAllowedPageHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return idealistaHosts.matches(h) || immobiliareHosts.matches(h)
    }
}
