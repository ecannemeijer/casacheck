package nl.ecannemeijer.casacheck.data.recording

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import nl.ecannemeijer.casacheck.data.audio.AudioFiles
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.db.RecStatus
import nl.ecannemeijer.casacheck.data.db.Recording
import nl.ecannemeijer.casacheck.data.db.RecordingMarker
import nl.ecannemeijer.casacheck.data.db.TranscriptSegment
import nl.ecannemeijer.casacheck.data.db.TxStatus
import nl.ecannemeijer.casacheck.data.db.newUuid
import nl.ecannemeijer.casacheck.data.files.FileStore
import nl.ecannemeijer.casacheck.data.translate.TranslationRepository
import java.io.File
import java.io.IOException

/** Viewing recordings per property: files live in filesDir/recordings/<propertyUuid>/. */
class RecordingRepository(private val ctx: Context, private val db: CasaDatabase, private val files: FileStore) {
    private val dao get() = db.recordingDao()

    fun observe(pid: Long): Flow<List<Recording>> = dao.observe(pid)
    fun observeOne(id: Long): Flow<Recording?> = dao.observeOne(id)
    fun observeMarkers(id: Long): Flow<List<RecordingMarker>> = dao.observeMarkers(id)
    fun observeSegments(id: Long): Flow<List<TranscriptSegment>> = dao.observeSegments(id)
    suspend fun get(id: Long) = dao.get(id)
    suspend fun segments(id: Long) = dao.segments(id)
    suspend fun markers(id: Long) = dao.markers(id)
    fun fileOf(r: Recording): File = files.resolve(r.filePath)!!
    fun freeBytes(): Long = files.root.usableSpace

    /** Creates the DB row (status RECORDING) and returns it with the raw .aac target file. */
    suspend fun begin(pid: Long, name: String): Pair<Recording, File> = withContext(Dispatchers.IO) {
        val p = db.propertyDao().get(pid) ?: throw IOException("property $pid not found")
        val uuid = newUuid()
        val f = File(files.recordingsDir(p.uuid), "$uuid.aac")
        val r = Recording(uuid = uuid, propertyId = pid, name = name, filePath = files.relative(f), mime = MIME_AAC, status = RecStatus.RECORDING)
        r.copy(id = dao.insert(r)) to f
    }

    /** Turns the raw ADTS file into .m4a, fills in duration/size and marks it DONE. Empty recordings are removed. */
    suspend fun finalizeRecording(id: Long): Recording? = withContext(Dispatchers.IO) {
        val r = dao.get(id) ?: return@withContext null
        val src = fileOf(r)
        if (!src.exists() || src.length() < MIN_BYTES) { dao.delete(id); src.delete(); return@withContext null }
        var cur = r
        if (r.mime == MIME_AAC) {
            val dst = File(src.parentFile, src.nameWithoutExtension + ".m4a")
            if (AudioFiles.remuxToM4a(src, dst)) { src.delete(); cur = r.copy(filePath = files.relative(dst), mime = MIME_M4A) }
        }
        val f = fileOf(cur)
        val upd = cur.copy(status = RecStatus.DONE, durationMs = AudioFiles.durationMs(f), sizeBytes = f.length())
        dao.update(upd)
        upd
    }

    /** After a crash / process death: finalise recordings that were still RECORDING when the process started. */
    suspend fun recoverInterrupted(startedBefore: Long): Int {
        var n = 0
        dao.withStatus(RecStatus.RECORDING).filter { it.createdAt < startedBefore }.forEach { finalizeRecording(it.id); n++ }
        dao.pendingTranscripts().forEach { if (it.transcriptStatus == TxStatus.RUNNING) dao.setTranscriptStatus(it.id, TxStatus.NONE, null) }
        return n
    }

    suspend fun rename(id: Long, name: String) { dao.get(id)?.let { dao.update(it.copy(name = name.trim().ifBlank { it.name }.take(120))) } }
    suspend fun setNotes(id: Long, notes: String) { dao.get(id)?.let { dao.update(it.copy(notes = notes.ifBlank { null })) } }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val r = dao.get(id) ?: return@withContext
        dao.delete(id)
        db.translationDao().deleteFieldPrefix(r.propertyId, TranslationRepository.transcriptField(r.id))
        files.delete(r.filePath)
    }

    suspend fun addMarker(recordingId: Long, positionMs: Long, label: String?, itemId: Long?): Long =
        dao.insertMarker(RecordingMarker(recordingId = recordingId, positionMs = positionMs, label = label, itemId = itemId))
    suspend fun deleteMarker(id: Long) = dao.deleteMarker(id)

    suspend fun setTranscriptStatus(id: Long, status: String, error: String? = null) = dao.setTranscriptStatus(id, status, error)

    suspend fun saveTranscript(id: Long, segments: List<TranscriptSegment>, lang: String?, model: String) {
        db.withTransaction {
            dao.deleteSegments(id)
            dao.insertSegments(segments.mapIndexed { i, s -> s.copy(id = 0, recordingId = id, idx = i) })
            dao.get(id)?.let { r -> dao.update(r.copy(transcriptStatus = TxStatus.DONE, transcriptLang = lang, transcriptError = null, transcriptModel = model)) }
            db.recordingDao().get(id)?.let { db.translationDao().deleteFieldPrefix(it.propertyId, TranslationRepository.transcriptField(id)) }
        }
    }

    suspend fun clearTranscript(id: Long) {
        dao.deleteSegments(id)
        dao.get(id)?.let { r -> dao.update(r.copy(transcriptStatus = TxStatus.NONE, transcriptLang = null, transcriptError = null)); db.translationDao().deleteFieldPrefix(r.propertyId, TranslationRepository.transcriptField(id)) }
    }

    /** Imports an existing audio file (e.g. a recording made with another app) for a property. */
    suspend fun importAudio(pid: Long, uri: Uri): Recording = withContext(Dispatchers.IO) {
        val p = db.propertyDao().get(pid) ?: throw IOException("property not found")
        val cr = ctx.contentResolver
        val mime = cr.getType(uri) ?: "audio/*"
        if (!mime.startsWith("audio/") && mime != "application/ogg" && mime != "video/mp4") throw IOException("not audio: $mime")
        val display = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "audio"
        val ext = display.substringAfterLast('.', "").lowercase().takeIf { it.length in 2..4 && it.all(Char::isLetterOrDigit) } ?: "m4a"
        val uuid = newUuid()
        val f = File(files.recordingsDir(p.uuid), "$uuid.$ext")
        cr.openInputStream(uri)?.use { input -> f.outputStream().use { out ->
            val buf = ByteArray(64 * 1024); var total = 0L
            while (true) { val n = input.read(buf); if (n < 0) break; total += n; if (total > MAX_IMPORT) { f.delete(); throw IOException("too large") }; out.write(buf, 0, n) }
        } } ?: throw IOException("cannot open")
        val dur = AudioFiles.durationMs(f)
        if (dur <= 0) { f.delete(); throw IOException("unreadable audio") }
        val r = Recording(uuid = uuid, propertyId = pid, name = display.substringBeforeLast('.').take(120), filePath = files.relative(f),
            mime = if (ext == "m4a") MIME_M4A else mime, durationMs = dur, sizeBytes = f.length(), status = RecStatus.DONE)
        r.copy(id = dao.insert(r))
    }

    companion object {
        const val MIME_AAC = "audio/aac"
        const val MIME_M4A = "audio/mp4"
        const val MIN_BYTES = 64L
        const val MAX_IMPORT = 1024L * 1024 * 1024
        /** Refuse to start below this; stop a running recording below [STOP_FREE_BYTES]. */
        const val MIN_FREE_BYTES = 50L * 1024 * 1024
        const val STOP_FREE_BYTES = 20L * 1024 * 1024
    }
}