package nl.ecannemeijer.casacheck.domain.content

import androidx.annotation.StringRes

data class DocumentDef(
    val code: String,
    /** Italian official name, shown in every language. */
    val italianName: String,
    @StringRes val name: Int,
    @StringRes val why: Int,
    @StringRes val check: Int,
    @StringRes val who: Int,
    /** Example question for the agent, always in Italian. */
    val questionIt: String,
    /** The same question in the app language. */
    @StringRes val question: Int,
)

data class AgentQuestionDef(val code: String, @StringRes val text: Int, val italian: String)

/** Environment item; [mapsQuery] is an Italian search term for a Google Maps "nearby" intent (null = rating only). */
data class EnvItemDef(val code: String, @StringRes val name: Int, val mapsQuery: String?)

enum class PreferenceKind { MAX_EUR, MIN_SQM, BOOL, MAX_KM }
data class PreferenceDef(val code: String, val kind: PreferenceKind, @StringRes val name: Int)

/** The 20 cost categories of the cost module (names in string resources cost_cat_<code>). */
val COST_CATEGORIES = listOf(
    "MOISTURE", "ROOF", "FACADE", "STRUCTURAL", "FRAMES", "DOUBLE_GLAZING", "ELECTRICAL", "WATER", "SEWER", "HEATING",
    "INSULATION", "KITCHEN", "BATHROOM", "PAINTING", "PLASTERING", "FLOORS", "DRAINAGE", "RETAINING", "GARDEN", "OTHER",
)

/** Maps a checkpoint category to a cost category (used when a defect is created from a checkpoint). */
fun costCategoryFor(checkCategory: String?): String = when (checkCategory) {
    "MOISTURE", "VENTILATION" -> "MOISTURE"
    "ROOF", "GUTTERS", "DOWNPIPES", "ATTIC" -> "ROOF"
    "FACADE", "EXTERIOR_WALLS" -> "FACADE"
    "FOUNDATION", "STRUCTURE", "CRACKS", "BALCONY" -> "STRUCTURAL"
    "FRAMES", "DOORS" -> "FRAMES"
    "WINDOWS" -> "DOUBLE_GLAZING"
    "ELECTRICAL" -> "ELECTRICAL"
    "WATER", "HOT_WATER" -> "WATER"
    "SEWER", "SEPTIC" -> "SEWER"
    "HEATING", "GAS", "ENERGY" -> "HEATING"
    "INSULATION" -> "INSULATION"
    "KITCHEN" -> "KITCHEN"
    "BATHROOM" -> "BATHROOM"
    "DRAINAGE", "SLOPES" -> "DRAINAGE"
    "RETAINING" -> "RETAINING"
    "GARDEN", "GROUNDS", "TERRACE" -> "GARDEN"
    else -> "OTHER"
}
