package nl.ecannemeijer.casacheck.domain.moisture

/**
 * Describes the pattern of readings along a wall. Readings of different meters/materials are NOT comparable,
 * so the analysis is purely relative (to the reference reading on a presumably dry wall, or to the readings themselves).
 * Results are possible explanations, never a diagnosis.
 */
enum class MoisturePattern { INSUFFICIENT, UNIFORM_LOW, DECREASING_FROM_FLOOR, HIGHEST_AT_TOP, LOCAL_PEAK, UNIFORM_ELEVATED, IRREGULAR }

data class MoistureAnalysis(val pattern: MoisturePattern, val elevatedHeights: List<Int>, val relativeToReference: Boolean)

object MoistureAnalyzer {
    const val DEFAULT_HEIGHTS_CM = "10,50,100,150,200"
    val DEFAULT_HEIGHTS = listOf(10, 50, 100, 150, 200)

    /** A reading is "elevated" when it is at least 50% above the reference (if given) or the lowest reading of the wall. */
    fun analyze(points: List<Pair<Int, Double>>, reference: Double?): MoistureAnalysis {
        val p = points.sortedBy { it.first }
        if (p.size < 3) return MoistureAnalysis(MoisturePattern.INSUFFICIENT, emptyList(), reference != null)
        val base = reference?.takeIf { it > 0 } ?: p.minOf { it.second }.takeIf { it > 0 } ?: 0.0001
        val elevated = p.filter { it.second >= base * 1.5 }.map { it.first }
        val values = p.map { it.second }
        val spread = (values.max() - values.min()) / (values.max().takeIf { it > 0 } ?: 1.0)
        val pattern = when {
            elevated.isEmpty() && (reference != null || spread < 0.34) -> MoisturePattern.UNIFORM_LOW
            reference != null && elevated.size == p.size && spread < 0.34 -> MoisturePattern.UNIFORM_ELEVATED
            isNonIncreasing(values) && values.first() >= values.last() * 1.5 -> MoisturePattern.DECREASING_FROM_FLOOR
            isNonDecreasing(values) && values.last() >= values.first() * 1.5 -> MoisturePattern.HIGHEST_AT_TOP
            hasLocalPeak(values) -> MoisturePattern.LOCAL_PEAK
            spread < 0.34 -> if (reference != null && elevated.isNotEmpty()) MoisturePattern.UNIFORM_ELEVATED else MoisturePattern.UNIFORM_LOW
            else -> MoisturePattern.IRREGULAR
        }
        return MoistureAnalysis(pattern, elevated, reference != null)
    }

    private fun isNonIncreasing(v: List<Double>) = v.zipWithNext().all { (a, b) -> b <= a * 1.05 }
    private fun isNonDecreasing(v: List<Double>) = v.zipWithNext().all { (a, b) -> b >= a * 0.95 }
    private fun hasLocalPeak(v: List<Double>): Boolean {
        for (i in 1 until v.size - 1) if (v[i] >= v[i - 1] * 1.3 && v[i] >= v[i + 1] * 1.3) return true
        return false
    }

    fun parseHeights(s: String): List<Int> = s.split(',', ';', ' ').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..1000 }.distinct().sorted()
}
