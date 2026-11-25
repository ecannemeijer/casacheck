package nl.ecannemeijer.casacheck.data.importer

import kotlinx.coroutines.CancellationException

/** Runs a URL import with progress and cancellation; returns a typed outcome. No bypassing of blocks. */
class ImportManager(private val fetcher: HttpFetcher = HttpFetcher()) {
    suspend fun importUrl(raw: String, acceptLanguage: String, progress: (ImportProgress) -> Unit): ImportOutcome {
        progress(ImportProgress(ImportStage.VALIDATING))
        val url = UrlRecognizer.recognize(UrlRecognizer.extractUrl(raw) ?: raw) ?: return ImportOutcome.Failed("unsupported url")
        val provider = ImportProviderRegistry.forUrl(url) ?: return ImportOutcome.Failed("no provider")
        return try {
            provider.import(url, fetcher, acceptLanguage, progress).also { progress(ImportProgress(ImportStage.DONE)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ImportOutcome.Failed(e.message ?: e.javaClass.simpleName)
        }
    }
}
