package nl.ecannemeijer.casacheck.ui.common

import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CostType
import nl.ecannemeijer.casacheck.data.db.CostUrgency
import nl.ecannemeijer.casacheck.data.db.CrackDirection
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.DocumentStatus
import nl.ecannemeijer.casacheck.data.db.EnvRating
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.data.db.PreferenceType
import nl.ecannemeijer.casacheck.data.db.Priority
import nl.ecannemeijer.casacheck.data.db.RoomType
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.score.ScoreGroup

/** Enum -> string resource mappings for the UI. */
object Labels {
    fun costCat(code: String): Int = when (code) {
        "MOISTURE" -> R.string.cost_cat_MOISTURE; "ROOF" -> R.string.cost_cat_ROOF; "FACADE" -> R.string.cost_cat_FACADE
        "STRUCTURAL" -> R.string.cost_cat_STRUCTURAL; "FRAMES" -> R.string.cost_cat_FRAMES; "DOUBLE_GLAZING" -> R.string.cost_cat_DOUBLE_GLAZING
        "ELECTRICAL" -> R.string.cost_cat_ELECTRICAL; "WATER" -> R.string.cost_cat_WATER; "SEWER" -> R.string.cost_cat_SEWER
        "HEATING" -> R.string.cost_cat_HEATING; "INSULATION" -> R.string.cost_cat_INSULATION; "KITCHEN" -> R.string.cost_cat_KITCHEN
        "BATHROOM" -> R.string.cost_cat_BATHROOM; "PAINTING" -> R.string.cost_cat_PAINTING; "PLASTERING" -> R.string.cost_cat_PLASTERING
        "FLOORS" -> R.string.cost_cat_FLOORS; "DRAINAGE" -> R.string.cost_cat_DRAINAGE; "RETAINING" -> R.string.cost_cat_RETAINING
        "GARDEN" -> R.string.cost_cat_GARDEN; else -> R.string.cost_cat_OTHER
    }
    fun category(code: String): Int = CheckpointLibrary.categories.firstOrNull { it.code == code }?.name ?: R.string.cat_premarked
    fun roomType(t: RoomType) = when (t) {
        RoomType.LIVING -> R.string.rt_living; RoomType.KITCHEN -> R.string.rt_kitchen; RoomType.BEDROOM -> R.string.rt_bedroom
        RoomType.BATHROOM -> R.string.rt_bathroom; RoomType.TOILET -> R.string.rt_toilet; RoomType.HALLWAY -> R.string.rt_hallway
        RoomType.STAIRS -> R.string.rt_stairs; RoomType.CELLAR -> R.string.rt_cellar; RoomType.ATTIC -> R.string.rt_attic
        RoomType.GARAGE -> R.string.rt_garage; RoomType.STORAGE -> R.string.rt_storage; RoomType.LAUNDRY -> R.string.rt_laundry
        RoomType.STUDY -> R.string.rt_study; RoomType.TERRACE_ROOM -> R.string.rt_terrace; RoomType.OTHER -> R.string.rt_other
    }
    fun severity(s: Severity) = when (s) { Severity.MINOR -> R.string.status_minor; Severity.MAJOR -> R.string.status_major; Severity.SERIOUS -> R.string.status_serious }
    fun priority(p: Priority) = when (p) { Priority.IMMEDIATE -> R.string.prio_immediate; Priority.WITHIN_YEAR -> R.string.prio_within_year; Priority.LATER -> R.string.prio_later }
    fun defectStatus(s: DefectStatus) = when (s) {
        DefectStatus.OPEN -> R.string.ds_open; DefectStatus.MONITOR -> R.string.ds_monitor; DefectStatus.QUOTE_REQUESTED -> R.string.ds_quote_requested
        DefectStatus.QUOTED -> R.string.ds_quoted; DefectStatus.RESOLVED -> R.string.ds_resolved
    }
    fun docStatus(s: DocumentStatus) = when (s) {
        DocumentStatus.NOT_REQUESTED -> R.string.doc_status_not_requested; DocumentStatus.REQUESTED -> R.string.doc_status_requested
        DocumentStatus.RECEIVED -> R.string.doc_status_received; DocumentStatus.CHECKED -> R.string.doc_status_checked
        DocumentStatus.MISSING -> R.string.doc_status_missing; DocumentStatus.DEVIATION -> R.string.doc_status_deviation
        DocumentStatus.SPECIALIST_NEEDED -> R.string.doc_status_specialist
    }
    fun crack(d: CrackDirection) = when (d) {
        CrackDirection.HORIZONTAL -> R.string.crack_HORIZONTAL; CrackDirection.VERTICAL -> R.string.crack_VERTICAL; CrackDirection.DIAGONAL -> R.string.crack_DIAGONAL
        CrackDirection.STEPPED -> R.string.crack_STEPPED; CrackDirection.AROUND_WINDOW -> R.string.crack_AROUND_WINDOW; CrackDirection.AROUND_DOOR -> R.string.crack_AROUND_DOOR
        CrackDirection.CEILING -> R.string.crack_CEILING; CrackDirection.FOUNDATION -> R.string.crack_FOUNDATION
        CrackDirection.RETAINING_WALL -> R.string.crack_RETAINING_WALL; CrackDirection.FACADE -> R.string.crack_FACADE
    }
    fun env(r: EnvRating) = when (r) { EnvRating.UNKNOWN -> R.string.unknown; EnvRating.GOOD -> R.string.env_good; EnvRating.AVERAGE -> R.string.env_average; EnvRating.POOR -> R.string.env_poor; EnvRating.NOT_RELEVANT -> R.string.env_not_relevant }
    fun prefType(t: PreferenceType) = when (t) { PreferenceType.OFF -> R.string.pref_off; PreferenceType.HARD -> R.string.pref_hard; PreferenceType.SOFT -> R.string.pref_soft }
    fun mode(m: InspectionMode) = when (m) { InspectionMode.QUICK -> R.string.mode_quick; InspectionMode.FULL -> R.string.mode_full; InspectionMode.BY_ROOM -> R.string.mode_by_room; InspectionMode.BY_CATEGORY -> R.string.mode_by_category; InspectionMode.SUSPECT_ONLY -> R.string.mode_suspect }
    fun modeHelp(m: InspectionMode) = when (m) { InspectionMode.QUICK -> R.string.mode_quick_help; InspectionMode.FULL -> R.string.mode_full_help; InspectionMode.BY_ROOM -> R.string.mode_by_room_help; InspectionMode.BY_CATEGORY -> R.string.mode_by_category_help; InspectionMode.SUSPECT_ONLY -> R.string.mode_suspect_help }
    fun photoKind(k: PhotoKind) = when (k) { PhotoKind.LISTING -> R.string.pk_listing; PhotoKind.INSPECTION -> R.string.pk_inspection; PhotoKind.DEFECT -> R.string.pk_defect; PhotoKind.REFERENCE -> R.string.pk_reference; PhotoKind.FLOORPLAN -> R.string.pk_floorplan; PhotoKind.DOCUMENT -> R.string.pk_document }
    fun urgency(u: CostUrgency) = when (u) { CostUrgency.IMMEDIATE -> R.string.cost_immediate; CostUrgency.WITHIN_YEAR -> R.string.cost_within_year; CostUrgency.OPTIONAL -> R.string.cost_optional }
    fun costType(t: CostType) = when (t) { CostType.FIXED -> R.string.ct_fixed; CostType.PER_SQM -> R.string.ct_per_sqm; CostType.PER_UNIT -> R.string.ct_per_unit; CostType.MATERIAL -> R.string.ct_material; CostType.LABOR -> R.string.ct_labor }
    fun group(g: ScoreGroup) = when (g) {
        ScoreGroup.MOISTURE -> R.string.sg_moisture; ScoreGroup.STRUCTURE -> R.string.sg_structure; ScoreGroup.ROOF -> R.string.sg_roof
        ScoreGroup.INSTALLATIONS -> R.string.sg_installations; ScoreGroup.FINISHING -> R.string.sg_finishing; ScoreGroup.COSTS -> R.string.sg_costs
        ScoreGroup.DOCUMENTS -> R.string.sg_documents; ScoreGroup.ENVIRONMENT -> R.string.sg_environment; ScoreGroup.PERSONAL -> R.string.sg_personal
    }
    fun feature(key: String) = when (key) {
        "HILLSIDE" -> R.string.feat_hillside; "SEPTIC" -> R.string.feat_septic; "GAS" -> R.string.feat_gas; "DETACHED" -> R.string.feat_detached
        "DOUBLE_GLAZING" -> R.string.feat_double_glazing; "SOLAR_POSSIBLE" -> R.string.feat_solar; "HEAT_PUMP_POSSIBLE" -> R.string.feat_heat_pump
        "EV_POSSIBLE" -> R.string.feat_ev; "PERMANENT_LIVING" -> R.string.feat_permanent; else -> R.string.unknown
    }
}
