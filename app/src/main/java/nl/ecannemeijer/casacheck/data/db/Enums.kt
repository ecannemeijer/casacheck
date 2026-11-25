package nl.ecannemeijer.casacheck.data.db

/** Status of a single checkpoint. "Specialist nodig" is a separate flag on the result. */
enum class CheckStatus { NOT_CHECKED, OK, MINOR, MAJOR, SERIOUS, NOT_APPLICABLE, UNSURE;
    val isDefect get() = this == MINOR || this == MAJOR || this == SERIOUS
    val isChecked get() = this != NOT_CHECKED
}

enum class Severity { MINOR, MAJOR, SERIOUS }
enum class Priority { IMMEDIATE, WITHIN_YEAR, LATER }
enum class DefectStatus { OPEN, MONITOR, QUOTE_REQUESTED, QUOTED, RESOLVED }
enum class PhotoKind { LISTING, INSPECTION, DEFECT, REFERENCE, FLOORPLAN, DOCUMENT }
enum class InspectionMode { QUICK, FULL, BY_ROOM, BY_CATEGORY, SUSPECT_ONLY }
enum class DocumentStatus { NOT_REQUESTED, REQUESTED, RECEIVED, CHECKED, MISSING, DEVIATION, SPECIALIST_NEEDED }
enum class CostUrgency { IMMEDIATE, WITHIN_YEAR, OPTIONAL }
enum class CostType { FIXED, PER_SQM, PER_UNIT, MATERIAL, LABOR }
enum class ImportStatus { SUCCESS, PARTIAL, BLOCKED, FAILED, CANCELLED }
enum class ListingState { ACTIVE, REMOVED, BLOCKED, UNKNOWN }
enum class PreferenceType { OFF, HARD, SOFT }
enum class EnvRating { UNKNOWN, GOOD, AVERAGE, POOR, NOT_RELEVANT }
enum class CrackDirection { HORIZONTAL, VERTICAL, DIAGONAL, STEPPED, AROUND_WINDOW, AROUND_DOOR, CEILING, FOUNDATION, RETAINING_WALL, FACADE }
enum class RoomType { LIVING, KITCHEN, BEDROOM, BATHROOM, TOILET, HALLWAY, STAIRS, CELLAR, ATTIC, GARAGE, STORAGE, LAUNDRY, STUDY, TERRACE_ROOM, OTHER }
