package nl.ecannemeijer.casacheck.domain.checklist

/** One practical question to ask the agent, seller or technician: Italian text plus Dutch translation. */
data class CheckpointQuestion(val it: String, val nl: String)

/**
 * Practical Italian questions per built-in checkpoint (2-5 each), with Dutch translation.
 * Structured checklist data (not UI text): the checkpoint screen, the per-property question overview,
 * the share text and the PDF report all read from here. Keep to well-known Italian documents and practice.
 * Content lives in CheckpointQuestionsData*.kt; a unit test checks that every checkpoint has >= 2 questions.
 */
object CheckpointQuestions {
    val byCode: Map<String, List<CheckpointQuestion>> by lazy { questionsPart1() + questionsPart2() + questionsPart3() + questionsPart4() }

    fun forCode(code: String): List<CheckpointQuestion> = byCode[code].orEmpty()

    /** Plain-text version for copy/share: Italian line, Dutch translation indented below it. */
    fun asText(title: String?, questions: List<CheckpointQuestion>): String = buildString {
        if (!title.isNullOrBlank()) appendLine(title)
        questions.forEach { q -> appendLine("• " + q.it); appendLine("  (" + q.nl + ")") }
    }.trimEnd()
}

/** Pairs: Italian, Dutch, Italian, Dutch, ... */
internal fun qs(vararg s: String): List<CheckpointQuestion> {
    require(s.size % 2 == 0) { "questions must come in IT/NL pairs" }
    return s.toList().chunked(2).map { CheckpointQuestion(it[0], it[1]) }
}