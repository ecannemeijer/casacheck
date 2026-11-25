package nl.ecannemeijer.casacheck.data.importer

/**
 * Modular import architecture. New websites can be added by implementing [UrlImportProvider]
 * and registering it in [ImportProviderRegistry].
 */
interface PropertyImportProvider {
    val id: String
}

interface UrlImportProvider : PropertyImportProvider {
    val site: ListingSite
    fun canHandle(url: RecognizedUrl): Boolean = url.site == site
    /** Pure parsing of a fetched page (unit-tested with stored fixtures). */
    fun parse(html: String, url: RecognizedUrl): ListingData

    suspend fun import(url: RecognizedUrl, fetcher: HttpFetcher, acceptLanguage: String, progress: (ImportProgress) -> Unit): ImportOutcome {
        progress(ImportProgress(ImportStage.FETCHING))
        return when (val r = fetcher.fetchPage(url.normalizedUrl, acceptLanguage)) {
            is FetchResult.Ok -> {
                progress(ImportProgress(ImportStage.PARSING))
                val data = parse(r.body, url)
                val found = data.foundFields()
                progress(ImportProgress(ImportStage.PHOTOS, found.size, data.photos.size))
                if (found.size <= 2 && data.photos.isEmpty()) ImportOutcome.Failed("no listing data found")
                else ImportOutcome.Success(data, partial = isPartial(data))
            }
            is FetchResult.Blocked -> ImportOutcome.Blocked(site.displayName, r.code, r.reason)
            is FetchResult.HttpError -> if (r.code == 404 || r.code == 410) ImportOutcome.NotFound(site.displayName, r.code)
                else ImportOutcome.Failed("HTTP ${r.code}")
            is FetchResult.NetworkError -> ImportOutcome.Failed(r.message)
            is FetchResult.Rejected -> ImportOutcome.Failed(r.reason)
        }
    }

    fun isPartial(d: ListingData): Boolean =
        listOf(d.title, d.price, d.livingArea, d.city).any { it == null } || d.photos.isEmpty()
}

class IdealistaImportProvider : UrlImportProvider {
    override val id = "idealista"
    override val site = ListingSite.IDEALISTA
    override fun parse(html: String, url: RecognizedUrl) = IdealistaParser.parse(html, url.normalizedUrl, url.listingId)
}

class ImmobiliareImportProvider : UrlImportProvider {
    override val id = "immobiliare"
    override val site = ListingSite.IMMOBILIARE
    override fun parse(html: String, url: RecognizedUrl) = ImmobiliareParser.parse(html, url.normalizedUrl, url.listingId)
}

/** Pasted advertisement text. */
class TextImportProvider : PropertyImportProvider {
    override val id = "text"
    fun import(text: String): ListingData = TextListingParser.parse(text, "text")
}

/** Abstraction over the on-device OCR engine so the provider can be tested without ML Kit. */
fun interface OcrEngine {
    suspend fun recognize(imagePath: String): String
}

/** Screenshots -> local OCR (ML Kit, bundled model, fully offline) -> text parser. */
class ScreenshotImportProvider(private val ocr: OcrEngine) : PropertyImportProvider {
    override val id = "screenshot"
    suspend fun import(imagePaths: List<String>, onProgress: (Int) -> Unit = {}): Pair<ListingData, String> {
        val texts = imagePaths.mapIndexed { i, p -> onProgress(i); ocr.recognize(p) }
        val text = texts.joinToString("\n")
        return TextListingParser.parse(text, "screenshot") to text
    }
}

object ImportProviderRegistry {
    val urlProviders: List<UrlImportProvider> = listOf(IdealistaImportProvider(), ImmobiliareImportProvider())
    fun forUrl(url: RecognizedUrl): UrlImportProvider? = urlProviders.firstOrNull { it.canHandle(url) }
}
