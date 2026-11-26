package nl.ecannemeijer.casacheck.data.translate

import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    /**
     * Translates a transcript (one line per segment, so the timestamps stay aligned) to [target] (nl/en) and caches it
     * under field "rec:<id>". Returns the translated lines (same count as [lines]).
     */
    suspend fun translateTranscript(pid: Long, recordingId: Long, lines: List<String>, target: String, sourceLang: String?, onStage: (TranslateStage) -> Unit = {}): Pair<TranslateResult, List<String>?> {
        val text = lines.joinToString("\n") { it.replace('\n', ' ').trim() }
        val field = transcriptField(recordingId)
        val h = TranslationText.hash(text)
        dao.get(pid, field, target)?.takeIf { it.sourceHash == h }?.let { return TranslateResult.Ok(it.text, it.sourceLang) to alignLines(it.text, lines.size) }
        val r = translator.translate(text, target, settings.current().translateWifiOnly, onStage, sourceLang)
        if (r is TranslateResult.Ok) {
            dao.upsert(Translation(propertyId = pid, field = field, sourceHash = h, sourceLang = r.sourceLang, targetLang = target, text = r.text))
            return r to alignLines(r.text, lines.size)
        }
        return r to null
    }

    fun observeTranscript(pid: Long, recordingId: Long): Flow<List<Translation>> =
        dao.observe(pid).map { l -> l.filter { it.field == transcriptField(recordingId) } }

    companion object {
        const val TARGET = TranslateLanguage.DUTCH
        fun transcriptField(recordingId: Long) = "rec:$recordingId"
        /** Pads/truncates so line i always belongs to segment i. */
        fun alignLines(text: String, n: Int): List<String> = text.split("\n").let { l -> List(n) { i -> l.getOrElse(i) { "" } } }
    }
}