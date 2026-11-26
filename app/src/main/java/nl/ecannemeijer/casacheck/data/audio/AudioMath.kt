package nl.ecannemeijer.casacheck.data.audio

import java.util.Locale

/** Pure audio/time helpers (no Android dependencies, unit-tested). */
object AudioMath {
    /** Recording settings: speech-optimised AAC-LC, mono, 16 kHz, 32 kbit/s (about 14 MB per hour). */
    const val SAMPLE_RATE = 16_000
    const val BIT_RATE = 32_000
    const val WHISPER_RATE = 16_000

    fun bytesPerHour(bitRate: Int = BIT_RATE): Long = bitRate / 8L * 3600L

    /** "4:05" or "1:02:03". */
    fun formatTime(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000)
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
    }

    fun formatSize(bytes: Long, locale: Locale = Locale.getDefault()): String = when {
        bytes >= 1024L * 1024 * 1024 -> String.format(locale, "%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
        bytes >= 1024L * 1024 -> String.format(locale, "%.1f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> String.format(locale, "%d kB", bytes / 1024)
        else -> "$bytes B"
    }

    /** Readable, safe file name for sharing (instead of the internal uuid name). */
    fun shareName(name: String, path: String): String {
        val ext = path.substringAfterLast('.', "m4a").lowercase().takeIf { it.length in 2..4 } ?: "m4a"
        val base = name.trim().replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").trim('_').take(80).ifBlank { "audio" }
        return "$base.$ext"
    }

    /** Position of a marker on the seek bar, 0..1. */
    fun fraction(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0) 0f else (positionMs.toDouble() / durationMs).toFloat().coerceIn(0f, 1f)

    /** Whisper timestamps are in 10 ms units, relative to the chunk that was processed. */
    fun whisperToMs(t: Long, chunkOffsetMs: Long): Long = chunkOffsetMs + t * 10

    /** Index of the transcript segment that contains [positionMs] (or the last one that started before it); -1 if none. */
    fun activeSegment(startsMs: List<Long>, positionMs: Long): Int {
        var idx = -1
        for (i in startsMs.indices) { if (startsMs[i] <= positionMs) idx = i else break }
        return idx
    }

    /** Down-mixes interleaved 16-bit PCM to mono floats in -1..1. */
    fun toMonoFloat(pcm: ShortArray, count: Int, channels: Int): FloatArray {
        val ch = channels.coerceAtLeast(1)
        val frames = count / ch
        val out = FloatArray(frames)
        for (f in 0 until frames) {
            var sum = 0f
            for (c in 0 until ch) sum += pcm[f * ch + c]
            out[f] = sum / ch / 32768f
        }
        return out
    }

    /** Linear resampling (good enough for speech recognition). */
    fun resample(input: FloatArray, inRate: Int, outRate: Int): FloatArray {
        if (inRate == outRate || input.isEmpty()) return input
        val n = ((input.size.toLong() * outRate) / inRate).toInt().coerceAtLeast(1)
        val out = FloatArray(n)
        val step = inRate.toDouble() / outRate
        for (i in 0 until n) {
            val pos = i * step
            val i0 = pos.toInt().coerceAtMost(input.lastIndex)
            val i1 = (i0 + 1).coerceAtMost(input.lastIndex)
            val frac = (pos - i0).toFloat()
            out[i] = input[i0] * (1 - frac) + input[i1] * frac
        }
        return out
    }

    /** Default recording name, e.g. "Bezichtiging 26-11-2026 14:05" (prefix is localised by the caller). */
    fun defaultName(prefix: String, millis: Long, locale: Locale = Locale.getDefault()): String =
        prefix + " " + java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", locale).format(java.util.Date(millis))
}