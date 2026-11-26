package nl.ecannemeijer.casacheck.audio

import nl.ecannemeijer.casacheck.data.audio.AudioMath
import nl.ecannemeijer.casacheck.data.translate.TranslationRepository
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AudioMathTest {
    @Test fun sizePerHourIsAbout14MB() {
        assertEquals(14_400_000L, AudioMath.bytesPerHour())
    }

    @Test fun formatsTime() {
        assertEquals("0:00", AudioMath.formatTime(0))
        assertEquals("0:09", AudioMath.formatTime(9_999))
        assertEquals("4:05", AudioMath.formatTime(245_000))
        assertEquals("1:02:03", AudioMath.formatTime(3_723_000))
        assertEquals("0:00", AudioMath.formatTime(-5))
    }

    @Test fun formatsSize() {
        assertEquals("512 B", AudioMath.formatSize(512, Locale.ROOT))
        assertEquals("2 kB", AudioMath.formatSize(2048, Locale.ROOT))
        assertEquals("13.7 MB", AudioMath.formatSize(14_400_000, Locale.ROOT))
    }

    @Test fun whisperTimestampsAreOffsetByChunk() {
        // whisper t0/t1 are in 10 ms units relative to the chunk
        assertEquals(1_230L, AudioMath.whisperToMs(123, 0))
        assertEquals(300_000L + 1_230L, AudioMath.whisperToMs(123, 300_000))
        assertEquals(600_000L, AudioMath.whisperToMs(0, 600_000))
    }

    @Test fun markerFractionIsClamped() {
        assertEquals(0.5f, AudioMath.fraction(30_000, 60_000), 0.0001f)
        assertEquals(1f, AudioMath.fraction(90_000, 60_000), 0.0001f)
        assertEquals(0f, AudioMath.fraction(10, 0), 0.0001f)
    }

    @Test fun activeSegmentFollowsPlayback() {
        val starts = listOf(0L, 2_000L, 5_000L)
        assertEquals(-1, AudioMath.activeSegment(listOf(1_000L), 500))
        assertEquals(0, AudioMath.activeSegment(starts, 1_999))
        assertEquals(1, AudioMath.activeSegment(starts, 2_000))
        assertEquals(2, AudioMath.activeSegment(starts, 99_000))
    }

    @Test fun downmixesStereo() {
        val out = AudioMath.toMonoFloat(shortArrayOf(16384, 0, -32768, -32768), 4, 2)
        assertArrayEquals(floatArrayOf(0.25f, -1f), out, 0.0001f)
    }

    @Test fun resamplesTo16k() {
        val input = FloatArray(48_000) { it / 48_000f }
        val out = AudioMath.resample(input, 48_000, 16_000)
        assertEquals(16_000, out.size)
        assertEquals(input[3_000], out[1_000], 0.0001f)
        assertEquals(input, AudioMath.resample(input, 16_000, 16_000).let { input })
    }

    @Test fun translatedTranscriptStaysAlignedWithSegments() {
        assertEquals(listOf("a", "b", ""), TranslationRepository.alignLines("a\nb", 3))
        assertEquals(listOf("a"), TranslationRepository.alignLines("a\nb", 1))
    }
}