package nl.ecannemeijer.casacheck.domain.checklist

import nl.ecannemeijer.casacheck.data.db.InspectionItem
import nl.ecannemeijer.casacheck.data.db.Property
import nl.ecannemeijer.casacheck.data.db.Room
import nl.ecannemeijer.casacheck.data.db.RoomType

/** Feature keys stored in property_feature (value "true"/"false"; absent = unknown). */
object FeatureKeys {
    const val HILLSIDE = "HILLSIDE"
    const val SEPTIC = "SEPTIC"
    const val GAS = "GAS"
    const val DETACHED = "DETACHED"
    const val DOUBLE_GLAZING = "DOUBLE_GLAZING"
    const val SOLAR_POSSIBLE = "SOLAR_POSSIBLE"
    const val HEAT_PUMP_POSSIBLE = "HEAT_PUMP_POSSIBLE"
    const val EV_POSSIBLE = "EV_POSSIBLE"
    const val PERMANENT_LIVING = "PERMANENT_LIVING"
    val BOOL_KEYS = listOf(HILLSIDE, SEPTIC, GAS, DETACHED, DOUBLE_GLAZING, SOLAR_POSSIBLE, HEAT_PUMP_POSSIBLE, EV_POSSIBLE, PERMANENT_LIVING)
}

/**
 * Decides which checkpoints apply to a property, based on its (confirmed) features and rooms.
 * - A condition excludes a checkpoint only when the feature is explicitly false (unknown keeps it, so nothing is skipped silently).
 * - Room-type checkpoints are created once per matching room; when no matching room exists they are created at property level.
 * - "ANY" checkpoints are repeated for every room so that e.g. damp in bedroom 1 is independent from bedroom 2.
 */
object ChecklistGenerator {

    data class Key(val code: String, val roomId: Long?)

    fun featureState(property: Property, features: Map<String, String?>, cond: String): Boolean? = when (cond) {
        "CELLAR" -> property.cellar
        "GARAGE" -> property.garage
        "ATTIC" -> property.attic
        "GARDEN" -> property.garden
        "TERRACE" -> property.terrace
        "BALCONY" -> property.balcony
        "SEPTIC" -> features[FeatureKeys.SEPTIC].toTriState()
        "GAS" -> features[FeatureKeys.GAS].toTriState()
        "HILLSIDE" -> features[FeatureKeys.HILLSIDE].toTriState()
        "HOUSE" -> features[FeatureKeys.DETACHED].toTriState()
            ?: property.propertyType?.lowercase()?.let { t -> if (listOf("appart", "apartment", "flat", "attico", "loft").any { it in t }) false else null }
        else -> null
    }

    fun applies(def: CheckpointDef, property: Property, features: Map<String, String?>): Boolean =
        def.conditions.none { featureState(property, features, it) == false }

    fun roomMatches(def: CheckpointDef, room: Room): Boolean {
        if ("ANY" in def.roomTypes) return true
        return def.roomTypes.any { it == room.type.name }
    }

    /** Desired item keys for this property. */
    fun desired(
        library: List<CheckpointDef>,
        property: Property,
        features: Map<String, String?>,
        rooms: List<Room>,
    ): List<Pair<CheckpointDef, Long?>> {
        val out = mutableListOf<Pair<CheckpointDef, Long?>>()
        for (def in library) {
            if (!applies(def, property, features)) continue
            when {
                def.roomTypes.isEmpty() -> out += def to null
                "ANY" in def.roomTypes -> rooms.forEach { out += def to it.id }
                else -> {
                    val matching = rooms.filter { roomMatches(def, it) }
                    if (matching.isEmpty()) out += def to null else matching.forEach { out += def to it.id }
                }
            }
        }
        return out
    }

    /** New items to insert (existing items and their results are never touched). */
    fun newItems(
        library: List<CheckpointDef>,
        property: Property,
        features: Map<String, String?>,
        rooms: List<Room>,
        existing: List<InspectionItem>,
    ): List<InspectionItem> {
        val have = existing.map { Key(it.templateCode, it.roomId) }.toMutableSet()
        val roomOrder = rooms.withIndex().associate { it.value.id to it.index }
        return desired(library, property, features, rooms).mapNotNull { (def, roomId) ->
            val key = Key(def.code, roomId)
            if (!have.add(key)) return@mapNotNull null
            // A room-type check created at property level earlier is superseded by the room-specific one.
            InspectionItem(
                propertyId = property.id, templateCode = def.code, categoryCode = def.category, roomId = roomId,
                quick = def.quick, sortOrder = def.order * 100 + (roomId?.let { (roomOrder[it] ?: 0) + 1 } ?: 0),
            )
        }
    }

    /**
     * Property-level placeholders of room-type checks that are now covered by real rooms and still unchecked:
     * these may be removed. Items that already have a result are always kept.
     */
    fun obsoletePlaceholders(
        library: Map<String, CheckpointDef>,
        rooms: List<Room>,
        existing: List<InspectionItem>,
        checkedItemIds: Set<Long>,
    ): List<Long> = existing.filter { item ->
        val def = library[item.templateCode] ?: return@filter false
        item.roomId == null && def.roomTypes.isNotEmpty() && "ANY" !in def.roomTypes &&
            rooms.any { roomMatches(def, it) } && item.id !in checkedItemIds
    }.map { it.id }

    fun defaultRoomTypeFor(cond: String): RoomType? = when (cond) {
        "CELLAR" -> RoomType.CELLAR; "GARAGE" -> RoomType.GARAGE; "ATTIC" -> RoomType.ATTIC; else -> null
    }
}

fun String?.toTriState(): Boolean? = when (this?.lowercase()) { "true", "1", "yes" -> true; "false", "0", "no" -> false; else -> null }
