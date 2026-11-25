package nl.ecannemeijer.casacheck.domain

import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointQuestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointQuestionsTest {
    @Test fun everyCheckpointHasAtLeastTwoQuestionsInItalianAndDutch() {
        val missing = mutableListOf<String>()
        for (def in CheckpointLibrary.all) {
            val qs = CheckpointQuestions.forCode(def.code)
            if (qs.size < 2) missing += "${def.code} (${qs.size})"
            qs.forEach { q ->
                assertTrue("blank IT in ${def.code}", q.it.isNotBlank())
                assertTrue("blank NL in ${def.code}", q.nl.isNotBlank())
                assertTrue("IT and NL identical in ${def.code}: ${q.it}", q.it != q.nl)
            }
            assertTrue("more than 5 questions for ${def.code}", qs.size <= 5)
        }
        assertTrue("checkpoints without enough questions: $missing", missing.isEmpty())
    }

    @Test fun noQuestionsForUnknownCodes() {
        val known = CheckpointLibrary.all.map { it.code }.toSet()
        val unknown = CheckpointQuestions.byCode.keys - known
        assertTrue("unknown codes: $unknown", unknown.isEmpty())
        assertEquals(known.size, CheckpointQuestions.byCode.size)
    }

    @Test fun italianQuestionsEndWithQuestionMark() {
        CheckpointQuestions.byCode.forEach { (code, qs) -> qs.forEach { q -> assertTrue("$code: ${q.it}", q.it.trimEnd().endsWith("?")) } }
    }

    @Test fun asTextContainsItalianAndDutch() {
        val qs = CheckpointQuestions.forCode("moist_rising")
        val txt = CheckpointQuestions.asText("Optrekkend vocht", qs)
        assertTrue(txt.startsWith("Optrekkend vocht"))
        qs.forEach { assertTrue(txt.contains(it.it)); assertTrue(txt.contains(it.nl)) }
        assertTrue(CheckpointQuestions.forCode("does_not_exist").isEmpty())
    }
}