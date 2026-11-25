package nl.ecannemeijer.casacheck.translate

import kotlinx.coroutines.test.runTest
import nl.ecannemeijer.casacheck.data.translate.TranslationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TranslationTextTest {
    @Test fun keepsLayoutAndSkipsLinesWithoutLetters() = runTest {
        val calls = mutableListOf<String>()
        val out = TranslationText.translateLines("Casa in pietra\n\n120 m² - 3\nPrezzo trattabile") { s -> calls += s; "[" + s + "]" }
        assertEquals("[Casa in pietra]\n\n[120 m² - 3]\n[Prezzo trattabile]", out)
        assertEquals(3, calls.size)
    }

    @Test fun numbersOnlyLineIsCopied() = runTest {
        val out = TranslationText.translateLines("€ 120.000\n---") { "X" }
        assertEquals("€ 120.000\n---", out)
    }

    @Test fun hashIsStableAndChangesWithText() {
        assertEquals(TranslationText.hash("Casa"), TranslationText.hash("  Casa \n"))
        assertNotEquals(TranslationText.hash("Casa"), TranslationText.hash("Casa grande"))
        assertEquals(32, TranslationText.hash("x").length)
    }
}