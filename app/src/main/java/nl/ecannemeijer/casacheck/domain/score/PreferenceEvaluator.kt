package nl.ecannemeijer.casacheck.domain.score

import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.EnvRating
import nl.ecannemeijer.casacheck.data.db.PreferenceType
import nl.ecannemeijer.casacheck.data.db.PropertyPreference
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.domain.checklist.FeatureKeys
import nl.ecannemeijer.casacheck.domain.checklist.toTriState

enum class PrefOutcome { MET, NOT_MET, UNKNOWN }
data class PrefResult(val key: String, val type: PreferenceType, val outcome: PrefOutcome)

/** Compares a property with the personal wishes. Missing data always gives UNKNOWN, never MET. */
object PreferenceEvaluator {
    private fun cmpMax(value: Long?, limit: Double?): PrefOutcome =
        if (value == null || limit == null) PrefOutcome.UNKNOWN else if (value <= limit) PrefOutcome.MET else PrefOutcome.NOT_MET
    private fun cmpMin(value: Double?, limit: Double?): PrefOutcome =
        if (value == null || limit == null) PrefOutcome.UNKNOWN else if (value >= limit) PrefOutcome.MET else PrefOutcome.NOT_MET
    private fun bool(b: Boolean?) = when (b) { true -> PrefOutcome.MET; false -> PrefOutcome.NOT_MET; null -> PrefOutcome.UNKNOWN }
    private fun rangeMax(min: Long, max: Long, limit: Double?, hasData: Boolean) = when {
        limit == null || !hasData -> PrefOutcome.UNKNOWN
        max <= limit -> PrefOutcome.MET
        min > limit -> PrefOutcome.NOT_MET
        else -> PrefOutcome.UNKNOWN
    }

    fun evaluate(s: PropertySnapshot, prefs: List<PropertyPreference>): List<PrefResult> = prefs.filter { it.type != PreferenceType.OFF }.map { p ->
        PrefResult(p.key, p.type, outcome(s, p.key, p.numberValue))
    }

    fun outcome(s: PropertySnapshot, key: String, v: Double?): PrefOutcome {
        val prop = s.property
        val active = s.defects.filter { it.status != DefectStatus.RESOLVED }
        val hasCost = s.costs.lines.any { it.hasAmount }
        return when (key) {
            "max_purchase" -> cmpMax(prop.askingPrice, v)
            "max_renovation" -> rangeMax(s.costs.renovation.min, s.costs.renovation.max, v, hasCost)
            "max_total" -> if (s.costs.totalMin == null) PrefOutcome.UNKNOWN else rangeMax(s.costs.totalMin, s.costs.totalMax!!, v, true)
            "min_living" -> cmpMin(prop.livingArea, v)
            "min_plot" -> cmpMin(prop.plotArea, v)
            "detached" -> bool(s.features[FeatureKeys.DETACHED].toTriState())
            "garage" -> bool(prop.garage)
            "garden" -> bool(prop.garden)
            "double_glazing" -> s.features[FeatureKeys.DOUBLE_GLAZING].toTriState()?.let { bool(it) } ?: run {
                val glazing = s.items.filter { it.templateCode == "win_glazing" || it.templateCode == "room_windows" }.map { it.effectiveStatus }.filter { it.isChecked && it != CheckStatus.NOT_APPLICABLE }
                when { glazing.isEmpty() -> PrefOutcome.UNKNOWN; glazing.all { it == CheckStatus.OK } -> PrefOutcome.MET; glazing.any { it.isDefect } -> PrefOutcome.NOT_MET; else -> PrefOutcome.UNKNOWN }
            }
            "move_in_ready" -> when {
                active.any { it.severity != Severity.MINOR && it.priority == nl.ecannemeijer.casacheck.data.db.Priority.IMMEDIATE } -> PrefOutcome.NOT_MET
                active.any { it.severity == Severity.SERIOUS } -> PrefOutcome.NOT_MET
                ScoreCalculator.completion(s.items) >= 80 -> PrefOutcome.MET
                else -> PrefOutcome.UNKNOWN
            }
            "no_serious_damp" -> when {
                active.any { it.isMoisture && it.severity == Severity.SERIOUS } -> PrefOutcome.NOT_MET
                s.items.any { it.categoryCode == "MOISTURE" && it.effectiveStatus == CheckStatus.SERIOUS } -> PrefOutcome.NOT_MET
                s.items.filter { it.categoryCode == "MOISTURE" }.let { m -> m.isNotEmpty() && m.all { it.effectiveStatus.isChecked } } -> PrefOutcome.MET
                else -> PrefOutcome.UNKNOWN
            }
            "no_major_structural" -> when {
                active.any { it.isStructural && it.severity != Severity.MINOR } -> PrefOutcome.NOT_MET
                s.items.any { it.categoryCode in setOf("FOUNDATION", "STRUCTURE", "CRACKS") && (it.effectiveStatus == CheckStatus.MAJOR || it.effectiveStatus == CheckStatus.SERIOUS) } -> PrefOutcome.NOT_MET
                s.items.filter { it.categoryCode in setOf("FOUNDATION", "STRUCTURE", "CRACKS") }.let { m -> m.isNotEmpty() && m.all { it.effectiveStatus.isChecked } } -> PrefOutcome.MET
                else -> PrefOutcome.UNKNOWN
            }
            "max_sea_km" -> distanceOutcome(s.environment.firstOrNull { it.itemCode == "sea" }?.distanceKm
                ?: s.destinations.filter { it.isSea }.mapNotNull { d -> s.distances.firstOrNull { it.destinationId == d.id }?.distanceKm }.minOrNull(), v)
            "max_family_km" -> distanceOutcome(s.environment.firstOrNull { it.itemCode == "family" }?.distanceKm
                ?: s.destinations.filter { it.isFamily }.mapNotNull { d -> s.distances.firstOrNull { it.destinationId == d.id }?.distanceKm }.minOrNull(), v)
            "amenities_near" -> {
                val r = s.environment.filter { it.itemCode in setOf("supermarket", "pharmacy", "doctor") }.map { it.rating }
                when { r.any { it == EnvRating.POOR } -> PrefOutcome.NOT_MET; r.size == 3 && r.all { it == EnvRating.GOOD } -> PrefOutcome.MET; else -> PrefOutcome.UNKNOWN }
            }
            "dogs" -> when (s.environment.firstOrNull { it.itemCode == "dogs" }?.rating) { EnvRating.GOOD -> PrefOutcome.MET; EnvRating.POOR -> PrefOutcome.NOT_MET; else -> PrefOutcome.UNKNOWN }
            "solar" -> bool(s.features[FeatureKeys.SOLAR_POSSIBLE].toTriState())
            "heat_pump" -> bool(s.features[FeatureKeys.HEAT_PUMP_POSSIBLE].toTriState())
            "ev_charger" -> bool(s.features[FeatureKeys.EV_POSSIBLE].toTriState())
            else -> PrefOutcome.UNKNOWN
        }
    }

    private fun distanceOutcome(km: Double?, limit: Double?) =
        if (km == null || limit == null) PrefOutcome.UNKNOWN else if (km <= limit) PrefOutcome.MET else PrefOutcome.NOT_MET
}
