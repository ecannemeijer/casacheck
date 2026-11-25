package nl.ecannemeijer.casacheck.domain.checklist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class CategoryDef(val code: String, @StringRes val name: Int, val order: Int)

/**
 * A built-in checkpoint. All texts are string resources (nl default, it, en). A value of 0 means "not present".
 * [steps] is one string with steps separated by " | ".
 */
data class CheckpointDef(
    val code: String,
    val category: String,
    val order: Int,
    val quick: Boolean,
    @StringRes val title: Int,
    @StringRes val explanation: Int,
    @StringRes val steps: Int,
    @StringRes val lookFor: Int,
    @StringRes val normal: Int,
    @StringRes val abnormal: Int,
    @StringRes val causes: Int,
    @StringRes val consequences: Int,
    @StringRes val repair: Int,
    @StringRes val warning: Int,
    @StringRes val expert: Int,
    /** Indicative Italian repair costs in euro (0/0 = no indication). Always editable by the user. */
    val costMinEur: Long,
    val costMaxEur: Long,
    val tools: List<String>,
    @DrawableRes val illustration: Int,
    /** Feature conditions (CELLAR, GARAGE, ...): the item is skipped only when the feature is explicitly absent. */
    val conditions: List<String>,
    /** Room types this check belongs to; "ANY" = repeated for every room; empty = property level. */
    val roomTypes: List<String>,
    /** M = moisture, S = structural, V = safety, L = legal. */
    val flags: String,
) {
    val isMoisture get() = 'M' in flags || category == "MOISTURE"
    val isStructural get() = 'S' in flags || category in setOf("FOUNDATION", "STRUCTURE", "CRACKS", "RETAINING")
    val isSafety get() = 'V' in flags
    val isLegal get() = 'L' in flags || category in setOf("BUILDING_DOCS", "LEGAL_DOCS")
    val hasCost get() = costMaxEur > 0
}

fun splitSteps(s: String): List<String> = s.split("|").map { it.trim() }.filter { it.isNotEmpty() }
