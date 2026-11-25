package nl.ecannemeijer.casacheck.domain.cost

import nl.ecannemeijer.casacheck.data.db.CostEstimate
import nl.ecannemeijer.casacheck.data.db.CostType
import nl.ecannemeijer.casacheck.data.db.CostUrgency
import nl.ecannemeijer.casacheck.data.db.Defect
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.Priority
import nl.ecannemeijer.casacheck.data.db.RepairProject

/** A cost range; [indicative] = at least one part is not based on a received quote. */
data class Range(val min: Long, val max: Long, val indicative: Boolean = true) {
    operator fun plus(o: Range) = Range(min + o.min, max + o.max, indicative || o.indicative)
    fun times(f: Double) = Range(Math.round(min * f), Math.round(max * f), indicative)
    companion object { val ZERO = Range(0, 0, false) }
}

data class CostLine(
    val source: String, // "defect", "project", "estimate"
    val id: Long,
    val title: String,
    val category: String,
    val urgency: CostUrgency,
    val range: Range,
    val hasAmount: Boolean,
)

data class CostSummary(
    val askingPrice: Long?,
    val renovation: Range,
    val immediate: Range,
    val withinYear: Range,
    val optional: Range,
    val contingency: Range,
    val purchaseCosts: Long?,
    val totalMin: Long?,
    val totalMax: Long?,
    val ownOffer: Long?,
    val totalOwnOfferMin: Long?,
    val totalOwnOfferMax: Long?,
    val lines: List<CostLine>,
    val byCategory: Map<String, Range>,
    val defectsWithoutCost: Int,
    val indicative: Boolean,
)

/**
 * Cost aggregation without double counting:
 * - A defect linked to a repair project is never counted on its own; the project is counted once.
 * - A project's amount: received quote > own estimate > sum of its cost estimates > own min/max > the LARGEST linked defect range
 *   (linked defects describe one repair, so their ranges are not summed).
 * - Cost estimates linked to a project are part of that project, never counted separately.
 * - Resolved defects are excluded.
 */
object CostCalculator {

    fun estimateRange(c: CostEstimate): Range? {
        c.quoteAmount?.takeIf { c.quoteReceived }?.let { return Range(it, it, false) }
        c.costOwn?.let { return Range(it, it, true) }
        val q = c.quantity
        val computed: Range? = when (c.type) {
            CostType.PER_SQM, CostType.PER_UNIT ->
                if (q != null && (c.unitPriceMin != null || c.unitPriceMax != null)) {
                    val lo = c.unitPriceMin ?: c.unitPriceMax!!; val hi = c.unitPriceMax ?: lo
                    Range(Math.round(lo * q), Math.round(hi * q))
                } else null
            CostType.MATERIAL, CostType.LABOR, CostType.FIXED -> null
        }
        val ml = listOfNotNull(c.materialCost, c.laborCost).takeIf { it.isNotEmpty() }?.sum()?.let { Range(it, it) }
        val direct = if (c.costMin != null || c.costMax != null) Range(c.costMin ?: c.costMax!!, c.costMax ?: c.costMin!!) else null
        return computed ?: direct ?: ml
    }

    fun defectRange(d: Defect): Range? {
        d.costOwn?.let { return Range(it, it, !d.quoteReceived) }
        if (d.costMin == null && d.costMax == null) return null
        return Range(d.costMin ?: d.costMax!!, d.costMax ?: d.costMin!!)
    }

    fun projectRange(p: RepairProject, estimates: List<CostEstimate>, defects: List<Defect>): Range? {
        p.quoteAmount?.takeIf { p.quoteReceived }?.let { return Range(it, it, false) }
        p.costOwn?.let { return Range(it, it) }
        val est = estimates.mapNotNull { estimateRange(it) }
        if (est.isNotEmpty()) return est.reduce { a, b -> a + b }
        if (p.costMin != null || p.costMax != null) return Range(p.costMin ?: p.costMax!!, p.costMax ?: p.costMin!!)
        val dr = defects.mapNotNull { defectRange(it) }
        if (dr.isEmpty()) return null
        return Range(dr.maxOf { it.min }, dr.maxOf { it.max }, dr.any { it.indicative })
    }

    private fun Priority.toUrgency() = when (this) {
        Priority.IMMEDIATE -> CostUrgency.IMMEDIATE; Priority.WITHIN_YEAR -> CostUrgency.WITHIN_YEAR; Priority.LATER -> CostUrgency.OPTIONAL
    }

    fun summarize(
        askingPrice: Long?,
        ownOffer: Long?,
        purchaseCostPercent: Double,
        contingencyPercent: Double,
        defects: List<Defect>,
        projects: List<RepairProject>,
        estimates: List<CostEstimate>,
    ): CostSummary {
        val active = defects.filter { it.status != DefectStatus.RESOLVED }
        val projectIds = projects.map { it.id }.toSet()
        val lines = mutableListOf<CostLine>()
        for (d in active.filter { it.repairProjectId == null || it.repairProjectId !in projectIds }) {
            val r = defectRange(d)
            lines += CostLine("defect", d.id, d.title, d.categoryCode ?: "OTHER", d.priority.toUrgency(), r ?: Range.ZERO, r != null)
        }
        for (p in projects) {
            val linkedDefects = active.filter { it.repairProjectId == p.id }
            val r = projectRange(p, estimates.filter { it.repairProjectId == p.id }, linkedDefects)
            lines += CostLine("project", p.id, p.name, p.costCategory, p.urgency, r ?: Range.ZERO, r != null)
        }
        for (c in estimates.filter { it.repairProjectId == null || it.repairProjectId !in projectIds }) {
            val r = estimateRange(c)
            lines += CostLine("estimate", c.id, c.description, c.costCategory, c.urgency, r ?: Range.ZERO, r != null)
        }
        fun sumOf(pred: (CostLine) -> Boolean) = lines.filter(pred).map { it.range }.fold(Range.ZERO) { a, b -> a + b }
        val immediate = sumOf { it.urgency == CostUrgency.IMMEDIATE }
        val withinYear = sumOf { it.urgency == CostUrgency.WITHIN_YEAR }
        val optional = sumOf { it.urgency == CostUrgency.OPTIONAL }
        val renovation = immediate + withinYear
        val contingency = renovation.times(contingencyPercent / 100.0)
        val purchase = askingPrice?.let { Math.round(it * purchaseCostPercent / 100.0) }
        val totalMin = askingPrice?.let { it + purchase!! + renovation.min + contingency.min }
        val totalMax = askingPrice?.let { it + purchase!! + renovation.max + contingency.max }
        val ownPurchase = ownOffer?.let { Math.round(it * purchaseCostPercent / 100.0) }
        val byCat = lines.groupBy { it.category }.mapValues { (_, l) -> l.map { it.range }.fold(Range.ZERO) { a, b -> a + b } }
        return CostSummary(
            askingPrice = askingPrice, renovation = renovation, immediate = immediate, withinYear = withinYear, optional = optional,
            contingency = contingency, purchaseCosts = purchase, totalMin = totalMin, totalMax = totalMax, ownOffer = ownOffer,
            totalOwnOfferMin = ownOffer?.let { it + ownPurchase!! + renovation.min + contingency.min },
            totalOwnOfferMax = ownOffer?.let { it + ownPurchase!! + renovation.max + contingency.max },
            lines = lines, byCategory = byCat,
            defectsWithoutCost = lines.count { it.source == "defect" && !it.hasAmount },
            indicative = lines.any { it.hasAmount && it.range.indicative } || lines.isEmpty(),
        )
    }
}
