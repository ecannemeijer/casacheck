package nl.ecannemeijer.casacheck.data.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Android media helpers: finalising recordings and decoding audio for transcription. */
object AudioFiles {
    /**
     * Copies the AAC frames of a raw ADTS (.aac) recording into an MP4 (.m4a) container without re-encoding.
     * ADTS survives a crash (every frame is self-contained), so even an interrupted recording can be finalised.
     */
    fun remuxToM4a(src: File, dst: File): Boolean {
        val ex = MediaExtractor()
        var mux: MediaMuxer? = null
        var started = false
        try {
            ex.setDataSource(src.absolutePath)
            val track = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: return false
            ex.selectTrack(track)
            mux = MediaMuxer(dst.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val t = mux.addTrack(ex.getTrackFormat(track))
            mux.start(); started = true
            val buf = ByteBuffer.allocate(256 * 1024)
            val info = MediaCodec.BufferInfo()
            var frames = 0
            while (true) {
                val size = ex.readSampleData(buf, 0)
                if (size < 0) break
                info.set(0, size, ex.sampleTime, if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                mux.writeSampleData(t, buf, info)
                frames++
                if (!ex.advance()) break
            }
            mux.stop(); started = false
            return frames > 0 && dst.length() > 0
        } catch (e: Exception) {
            dst.delete()
            return false
        } finally {
            runCatching { if (started) mux?.stop() }
            runCatching { mux?.release() }
            ex.release()
        }
    }

    fun durationMs(f: File): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(f.absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) { 0L } finally { runCatching { r.release() } }
    }

    /**
     * Decodes any audio file Android can read (m4a, aac, wav, mp3, ogg...) to 16 kHz mono float PCM and hands it out
     * in chunks of [chunkSec] seconds (keeps memory low for long recordings). [onChunk] returns false to cancel.
     * Returns the total decoded duration in ms.
     */
    fun decodeChunks(f: File, chunkSec: Int, onChunk: (offsetMs: Long, samples: FloatArray) -> Boolean): Long {
        val ex = MediaExtractor()
        ex.setDataSource(f.absolutePath)
        val track = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            ?: run { ex.release(); throw IllegalArgumentException("no audio track") }
        ex.selectTrack(track)
        val inFmt = ex.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(inFmt.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(inFmt, null, null, 0)
        codec.start()
        val chunkSize = chunkSec * AudioMath.WHISPER_RATE
        var pending = FloatArray(chunkSize); var filled = 0; var emitted = 0L
        var rate = inFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE); var channels = inFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val info = MediaCodec.BufferInfo()
        var inDone = false; var outDone = false; var cancelled = false
        fun push(samples: FloatArray) {
            var off = 0
            while (off < samples.size && !cancelled) {
                val n = minOf(chunkSize - filled, samples.size - off)
                System.arraycopy(samples, off, pending, filled, n); filled += n; off += n
                if (filled == chunkSize) {
                    if (!onChunk(emitted * 1000 / AudioMath.WHISPER_RATE, pending)) cancelled = true
                    emitted += filled; pending = FloatArray(chunkSize); filled = 0
                }
            }
        }
        try {
            while (!outDone && !cancelled) {
                if (!inDone) {
                    val ii = codec.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val size = ex.readSampleData(codec.getInputBuffer(ii)!!, 0)
                        if (size < 0) { codec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inDone = true }
                        else { codec.queueInputBuffer(ii, 0, size, ex.sampleTime, 0); ex.advance() }
                    }
                }
                val oi = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val of = codec.outputFormat
                        rate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE); channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    oi >= 0 -> {
                        if (info.size > 0) {
                            val bb = codec.getOutputBuffer(oi)!!.order(ByteOrder.LITTLE_ENDIAN)
                            bb.position(info.offset); bb.limit(info.offset + info.size)
                            val sb = bb.asShortBuffer(); val shorts = ShortArray(sb.remaining()); sb.get(shorts)
                            push(AudioMath.resample(AudioMath.toMonoFloat(shorts, shorts.size, channels), rate, AudioMath.WHISPER_RATE))
                        }
                        codec.releaseOutputBuffer(oi, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
                    }
                }
            }
            if (!cancelled && filled > 0) { onChunk(emitted * 1000 / AudioMath.WHISPER_RATE, pending.copyOf(filled)); emitted += filled }
        } finally {
            runCatching { codec.stop() }; codec.release(); ex.release()
        }
        return emitted * 1000 / AudioMath.WHISPER_RATE
    }
}