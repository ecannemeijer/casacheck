package nl.ecannemeijer.casacheck.data.transcribe

import nl.ecannemeijer.casacheck.data.audio.AudioFiles
import nl.ecannemeijer.casacheck.data.audio.AudioMath
import nl.ecannemeijer.casacheck.data.db.TranscriptSegment
import java.io.File
import java.io.IOException

/** Result of transcribing one recording. */
data class Transcript(val segments: List<TranscriptSegment>, val language: String?, val audioMs: Long, val elapsedMs: Long)

/** Pure helpers for combining per-chunk whisper output (unit-tested). */
object TranscriptMath {
    /** One raw whisper segment: t0/t1 in 10 ms units relative to its chunk. */
    data class Raw(val t0: Long, val t1: Long, val text: String)

    fun toSegments(raw: List<Raw>, chunkOffsetMs: Long, startIdx: Int): List<TranscriptSegment> =
        raw.mapNotNull { r ->
            val text = r.text.trim()
            if (text.isEmpty() || isNoise(text)) null else Pair(r, text)
        }.mapIndexed { i, (r, text) ->
            TranscriptSegment(recordingId = 0, idx = startIdx + i, startMs = AudioMath.whisperToMs(r.t0, chunkOffsetMs),
                endMs = AudioMath.whisperToMs(maxOf(r.t1, r.t0), chunkOffsetMs), text = text)
        }

    /** Whisper emits things like "[Music]", "(silence)" or "[BLANK_AUDIO]" for non-speech. */
    fun isNoise(text: String): Boolean = Regex("""^[\[(][^\])]*[\])]$""").matches(text.trim())

    /** Overall progress over all chunks, 0..1. */
    fun progress(chunkIndex: Int, chunkCount: Int, chunkPercent: Int): Float =
        if (chunkCount <= 0) 0f else ((chunkIndex + chunkPercent.coerceIn(0, 100) / 100f) / chunkCount).coerceIn(0f, 1f)

    fun chunkCount(durationMs: Long, chunkSec: Int): Int = ((durationMs + chunkSec * 1000L - 1) / (chunkSec * 1000L)).toInt().coerceAtLeast(1)
}

/** Runs whisper.cpp over an audio file in chunks of [CHUNK_SEC] seconds (bounded memory for long recordings). */
class Transcriber(private val modelFile: File) {
    /**
     * @param language "auto", "it", "nl" or "en". With "auto" the language detected in the first chunk is used for the rest.
     * @param onProgress overall 0..1
     */
    fun transcribe(audio: File, durationMs: Long, language: String, threads: Int, onProgress: (Float) -> Unit, isCancelled: () -> Boolean): Transcript {
        if (!WhisperLib.load()) throw IOException("speech engine not available on this device")
        val ctx = WhisperLib.initContext(modelFile.absolutePath)
        if (ctx == 0L) throw IOException("cannot load speech model")
        val t0 = System.currentTimeMillis()
        try {
            val out = mutableListOf<TranscriptSegment>()
            var lang = language
            var chunk = 0
            val chunks = TranscriptMath.chunkCount(durationMs, CHUNK_SEC)
            var failure: Int? = null
            val total = AudioFiles.decodeChunks(audio, CHUNK_SEC) { offsetMs, samples ->
                if (isCancelled()) return@decodeChunks false
                val idx = chunk
                val listener = object : WhisperListener {
                    override fun onProgress(percent: Int) = onProgress(TranscriptMath.progress(idx, chunks, percent))
                    override fun isCancelled(): Boolean = isCancelled()
                }
                val rc = WhisperLib.fullTranscribe(ctx, threads, lang, samples, listener)
                if (rc != 0) { if (!isCancelled()) failure = rc; return@decodeChunks false }
                if (lang == "auto") WhisperLib.detectedLanguage(ctx).takeIf { it.isNotBlank() }?.let { lang = it }
                val raw = (0 until WhisperLib.segmentCount(ctx)).map { i ->
                    TranscriptMath.Raw(WhisperLib.segmentT0(ctx, i), WhisperLib.segmentT1(ctx, i), String(WhisperLib.segmentText(ctx, i), Charsets.UTF_8))
                }
                out += TranscriptMath.toSegments(raw, offsetMs, out.size)
                chunk++
                onProgress(TranscriptMath.progress(chunk, chunks, 0))
                true
            }
            failure?.let { throw IOException("speech recognition failed ($it)") }
            return Transcript(out, lang.takeIf { it != "auto" }, total, System.currentTimeMillis() - t0)
        } finally {
            WhisperLib.freeContext(ctx)
        }
    }

    companion object {
        const val CHUNK_SEC = 300
        fun defaultThreads(): Int = Runtime.getRuntime().availableProcessors().let { if (it >= 8) 4 else (it - 1).coerceIn(1, 4) }
    }
}