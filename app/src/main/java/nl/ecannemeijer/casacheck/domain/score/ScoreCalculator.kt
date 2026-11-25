package nl.ecannemeijer.casacheck.domain.score

import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.DocumentStatus
import nl.ecannemeijer.casacheck.data.db.EnvRating
import nl.ecannemeijer.casacheck.data.db.ItemWithResult
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointDef

/** Score groups; the user chooses a weight 0..5 per group in the settings. */
enum class ScoreGroup { MOISTURE, STRUCTURE, ROOF, INSTALLATIONS, FINISHING, COSTS, DOCUMENTS, ENVIRONMENT, PERSONAL }

data class GroupScore(val group: ScoreGroup, val score: Int?, val completion: Int?, val weight: Int)

enum class CriticalKind { STRUCTURAL, SAFETY, LEGAL, MOISTURE }
data class CriticalIssue(val kind: CriticalKind, val title: String)

data class ScoreResult(
    /** 0..100, or null when there is nothing to score. Not a guarantee of safety or suitability. */
    val total: Int?,
    val groups: List<GroupScore>,
    /** Percentage of applicable checkpoints that have been checked. */
    val completion: Int,
    val critical: List<CriticalIssue>,
)

/**
 * Transparent 0-100 score.
 * Inspection groups: each applicable checkpoint earns OK=1, MINOR=0.65, UNSURE=0.4, MAJOR=0.3, SERIOUS=0 and NOT_CHECKED=0,
 * so unchecked items never count as good; N/A items are excluded. "Specialist needed" caps an item at 0.5.
 * Costs: renovation (max) relative to asking price. Documents: received/checked share, deviations score 0.
 * Environment: GOOD=1, AVERAGE=0.5, POOR=0, UNKNOWN=0 over rated relevant items. Personal: rating 1-5.
 * The total is the weighted mean of the groups that have data. Critical issues are always reported separately.
 */
object ScoreCalculator {
    val DEFAULT_WEIGHTS = mapOf(
        ScoreGroup.MOISTURE to 4, ScoreGroup.STRUCTURE to 5, ScoreGroup.ROOF to 4, ScoreGroup.INSTALLATIONS to 3,
        ScoreGroup.FINISHING to 2, ScoreGroup.COSTS to 4, ScoreGroup.DOCUMENTS to 3, ScoreGroup.ENVIRONMENT to 3, ScoreGroup.PERSONAL to 2,
    )

    fun groupOf(category: String): ScoreGroup? = when (category) {
        "MOISTURE", "VENTILATION", "DRAINAGE", "CELLAR" -> ScoreGroup.MOISTURE
        "FOUNDATION", "STRUCTURE", "CRACKS", "RETAINING", "SLOPES", "BALCONY", "EXTERIOR_WALLS" -> ScoreGroup.STRUCTURE
        "ROOF", "GUTTERS", "DOWNPIPES", "ATTIC" -> ScoreGroup.ROOF
        "ELECTRICAL", "WATER", "SEWER", "SEPTIC", "HEATING", "HOT_WATER", "GAS", "INSULATION", "ENERGY" -> ScoreGroup.INSTALLATIONS
        "WINDOWS", "FRAMES", "DOORS", "KITCHEN", "BATHROOM", "LIVING", "BEDROOM", "ROOM", "FACADE", "GARAGE", "GARDEN", "TERRACE", "GROUNDS", "GENERAL", "PREMARKED" -> ScoreGroup.FINISHING
        "ACCESS", "SURROUNDINGS", "AMENITIES", "INTERNET", "MOBILE" -> ScoreGroup.ENVIRONMENT
        "BUILDING_DOCS", "LEGAL_DOCS" -> ScoreGroup.DOCUMENTS
        else -> null
    }

    fun itemPoints(status: CheckStatus, specialist: Boolean): Double {
        val p = when (status) {
            CheckStatus.OK -> 1.0; CheckStatus.MINOR -> 0.65; CheckStatus.UNSURE -> 0.4; CheckStatus.MAJOR -> 0.3
            CheckStatus.SERIOUS -> 0.0; CheckStatus.NOT_CHECKED -> 0.0; CheckStatus.NOT_APPLICABLE -> 0.0
        }
        return if (specialist) minOf(p, 0.5) else p
    }

    fun completion(items: List<ItemWithResult>): Int {
        val applicable = items.filter { it.effectiveStatus != CheckStatus.NOT_APPLICABLE }
        if (applicable.isEmpty()) return 0
        return Math.round(applicable.count { it.effectiveStatus.isChecked } * 100f / applicable.size)
    }

    fun calculate(s: PropertySnapshot, weights: Map<ScoreGroup, Int>, library: Map<String, CheckpointDef>): ScoreResult {
        val groups = mutableListOf<GroupScore>()
        val byGroup = s.items.groupBy { groupOf(it.categoryCode) }
        for (g in listOf(ScoreGroup.MOISTURE, ScoreGroup.STRUCTURE, ScoreGroup.ROOF, ScoreGroup.INSTALLATIONS, ScoreGroup.FINISHING)) {
            val list = byGroup[g].orEmpty().filter { it.effectiveStatus != CheckStatus.NOT_APPLICABLE }
            val score = if (list.isEmpty()) null else Math.round(list.sumOf { itemPoints(it.effectiveStatus, it.specialistNeeded == true) } * 100 / list.size).toInt()
            groups += GroupScore(g, score, if (list.isEmpty()) null else completion(list), weights[g] ?: 0)
        }
        // Costs relative to the asking price
        val price = s.property.askingPrice
        val costScore = if (price != null && price > 0 && s.costs.lines.isNotEmpty()) {
            val ratio = s.costs.renovation.max.toDouble() / price
            (100 - (ratio * 200)).coerceIn(0.0, 100.0).toInt()
        } else null
        groups += GroupScore(ScoreGroup.COSTS, costScore, null, weights[ScoreGroup.COSTS] ?: 0)
        // Documents (only those that are tracked)
        val tracked = s.documents.filter { it.status != DocumentStatus.NOT_REQUESTED }
        val docScore = if (tracked.isEmpty()) null else Math.round(tracked.sumOf {
            when (it.status) {
                DocumentStatus.CHECKED -> 1.0; DocumentStatus.RECEIVED -> 0.7; DocumentStatus.REQUESTED -> 0.2
                else -> 0.0
            }
        } * 100 / tracked.size).toInt()
        val docItems = byGroup[ScoreGroup.DOCUMENTS].orEmpty().filter { it.effectiveStatus != CheckStatus.NOT_APPLICABLE }
        val docItemScore = if (docItems.isEmpty()) null else Math.round(docItems.sumOf { itemPoints(it.effectiveStatus, it.specialistNeeded == true) } * 100 / docItems.size).toInt()
        val docCombined = listOfNotNull(docScore, docItemScore).takeIf { it.isNotEmpty() }?.average()?.toInt()
        groups += GroupScore(ScoreGroup.DOCUMENTS, docCombined, null, weights[ScoreGroup.DOCUMENTS] ?: 0)
        // Environment: ratings + environment checkpoints
        val rated = s.environment.filter { it.rating != EnvRating.NOT_RELEVANT }
        val envRatingScore = if (rated.none { it.rating != EnvRating.UNKNOWN }) null else Math.round(rated.sumOf {
            when (it.rating) { EnvRating.GOOD -> 1.0; EnvRating.AVERAGE -> 0.5; else -> 0.0 }
        } * 100 / rated.size).toInt()
        val envItems = byGroup[ScoreGroup.ENVIRONMENT].orEmpty().filter { it.effectiveStatus != CheckStatus.NOT_APPLICABLE }
        val envItemScore = if (envItems.isEmpty()) null else Math.round(envItems.sumOf { itemPoints(it.effectiveStatus, it.specialistNeeded == true) } * 100 / envItems.size).toInt()
        groups += GroupScore(ScoreGroup.ENVIRONMENT, listOfNotNull(envRatingScore, envItemScore).takeIf { it.isNotEmpty() }?.average()?.toInt(), null, weights[ScoreGroup.ENVIRONMENT] ?: 0)
        groups += GroupScore(ScoreGroup.PERSONAL, s.property.personalRating?.let { ((it.coerceIn(1, 5) - 1) * 25) }, null, weights[ScoreGroup.PERSONAL] ?: 0)

        val usable = groups.filter { it.score != null && it.weight > 0 }
        val total = if (usable.isEmpty()) null else Math.round(usable.sumOf { it.score!! * it.weight }.toDouble() / usable.sumOf { it.weight })
            .toInt()
        return ScoreResult(total, groups, completion(s.items), criticalIssues(s, library))
    }

    fun criticalIssues(s: PropertySnapshot, library: Map<String, CheckpointDef>): List<CriticalIssue> {
        val out = mutableListOf<CriticalIssue>()
        for (d in s.defects.filter { it.status != DefectStatus.RESOLVED && it.severity == Severity.SERIOUS }) {
            when {
                d.isStructural -> out += CriticalIssue(CriticalKind.STRUCTURAL, d.title)
                d.isSafety -> out += CriticalIssue(CriticalKind.SAFETY, d.title)
                d.isLegal -> out += CriticalIssue(CriticalKind.LEGAL, d.title)
                d.isMoisture -> out += CriticalIssue(CriticalKind.MOISTURE, d.title)
            }
        }
        val covered = s.defects.mapNotNull { it.itemId }.toSet()
        for (i in s.items.filter { it.effectiveStatus == CheckStatus.SERIOUS && it.id !in covered }) {
            val def = library[i.templateCode] ?: continue
            val kind = when { def.isStructural -> CriticalKind.STRUCTURAL; def.isSafety -> CriticalKind.SAFETY; def.isLegal -> CriticalKind.LEGAL; def.isMoisture -> CriticalKind.MOISTURE; else -> null }
            if (kind != null) out += CriticalIssue(kind, i.customTitle ?: i.templateCode)
        }
        for (doc in s.documents.filter { it.status == DocumentStatus.DEVIATION }) out += CriticalIssue(CriticalKind.LEGAL, doc.typeCode)
        return out
    }
}
