package nl.ecannemeijer.casacheck.data.translate

import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.Flow
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.db.Translation
import nl.ecannemeijer.casacheck.data.settings.SettingsStore

/** Translates listing texts to Dutch and caches the result per property and field in the database. */
class TranslationRepository(private val db: CasaDatabase, private val translator: TextTranslator, private val settings: SettingsStore) {
    private val dao get() = db.translationDao()

    fun observe(pid: Long): Flow<List<Translation>> = dao.observe(pid)

    /** Returns the cached translation when the source text is unchanged, otherwise translates and stores it. */
    suspend fun translateField(pid: Long, field: String, text: String, onStage: (TranslateStage) -> Unit = {}): TranslateResult {
        val h = TranslationText.hash(text)
        dao.get(pid, field, TARGET)?.takeIf { it.sourceHash == h }?.let { return TranslateResult.Ok(it.text, it.sourceLang) }
        val r = translator.translate(text, TARGET, settings.current().translateWifiOnly, onStage)
        if (r is TranslateResult.Ok) dao.upsert(Translation(propertyId = pid, field = field, sourceHash = h, sourceLang = r.sourceLang, targetLang = TARGET, text = r.text))
        return r
    }

    /** Translates free text (pasted advertisement, OCR output) without caching. */
    suspend fun translateText(text: String, onStage: (TranslateStage) -> Unit = {}): TranslateResult =
        translator.translate(text, TARGET, settings.current().translateWifiOnly, onStage)

    companion object { const val TARGET = TranslateLanguage.DUTCH }
}