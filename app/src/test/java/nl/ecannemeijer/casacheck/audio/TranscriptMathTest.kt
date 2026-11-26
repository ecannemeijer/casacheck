package nl.ecannemeijer.casacheck.audio

import nl.ecannemeijer.casacheck.data.transcribe.TranscriptMath
import nl.ecannemeijer.casacheck.data.transcribe.TranscriptMath.Raw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptMathTest {
    @Test fun segmentsOfSecondChunkAreShiftedByChunkOffset() {
        val first = TranscriptMath.toSegments(listOf(Raw(0, 250, " Buongiorno "), Raw(250, 610, "la casa è del 1920.")), 0, 0)
        val second = TranscriptMath.toSegments(listOf(Raw(120, 480, "Il tetto è nuovo.")), 300_000, first.size)
        assertEquals(listOf(0L, 2_500L), first.map { it.startMs })
        assertEquals(listOf(2_500L, 6_100L), first.map { it.endMs })
        assertEquals("Buongiorno", first[0].text)
        assertEquals(301_200L, second[0].startMs)
        assertEquals(304_800L, second[0].endMs)
        assertEquals(2, second[0].idx)
    }

    @Test fun noiseAndEmptySegmentsAreDroppedAndIndicesStayContiguous() {
        val s = TranscriptMath.toSegments(listOf(Raw(0, 100, "[BLANK_AUDIO]"), Raw(100, 200, "  "), Raw(200, 300, "Ciao"), Raw(300, 400, "(musica)"), Raw(400, 500, "Grazie")), 0, 5)
        assertEquals(listOf("Ciao", "Grazie"), s.map { it.text })
        assertEquals(listOf(5, 6), s.map { it.idx })
        assertTrue(TranscriptMath.isNoise("[Music]"))
        assertFalse(TranscriptMath.isNoise("[sic] il muro"))
    }

    @Test fun endIsNeverBeforeStart() {
        val s = TranscriptMath.toSegments(listOf(Raw(500, 400, "x")), 0, 0)
        assertEquals(5_000L, s[0].endMs)
    }

    @Test fun progressAndChunkCount() {
        assertEquals(1, TranscriptMath.chunkCount(11_000, 300))
        assertEquals(1, TranscriptMath.chunkCount(300_000, 300))
        assertEquals(2, TranscriptMath.chunkCount(300_001, 300))
        assertEquals(12, TranscriptMath.chunkCount(3_600_000, 300))
        assertEquals(0.5f, TranscriptMath.progress(1, 2, 0), 0.0001f)
        assertEquals(0.75f, TranscriptMath.progress(1, 2, 50), 0.0001f)
        assertEquals(1f, TranscriptMath.progress(2, 2, 0), 0.0001f)
    }
}