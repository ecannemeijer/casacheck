package nl.ecannemeijer.casacheck.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun checkStatus(v: String?) = v?.let { runCatching { CheckStatus.valueOf(it) }.getOrDefault(CheckStatus.NOT_CHECKED) }
    @TypeConverter fun checkStatus(v: CheckStatus?) = v?.name
    @TypeConverter fun severity(v: String?) = v?.let { runCatching { Severity.valueOf(it) }.getOrDefault(Severity.MINOR) }
    @TypeConverter fun severity(v: Severity?) = v?.name
    @TypeConverter fun priority(v: String?) = v?.let { runCatching { Priority.valueOf(it) }.getOrDefault(Priority.LATER) }
    @TypeConverter fun priority(v: Priority?) = v?.name
    @TypeConverter fun defectStatus(v: String?) = v?.let { runCatching { DefectStatus.valueOf(it) }.getOrDefault(DefectStatus.OPEN) }
    @TypeConverter fun defectStatus(v: DefectStatus?) = v?.name
    @TypeConverter fun photoKind(v: String?) = v?.let { runCatching { PhotoKind.valueOf(it) }.getOrDefault(PhotoKind.INSPECTION) }
    @TypeConverter fun photoKind(v: PhotoKind?) = v?.name
    @TypeConverter fun mode(v: String?) = v?.let { runCatching { InspectionMode.valueOf(it) }.getOrDefault(InspectionMode.FULL) }
    @TypeConverter fun mode(v: InspectionMode?) = v?.name
    @TypeConverter fun docStatus(v: String?) = v?.let { runCatching { DocumentStatus.valueOf(it) }.getOrDefault(DocumentStatus.NOT_REQUESTED) }
    @TypeConverter fun docStatus(v: DocumentStatus?) = v?.name
    @TypeConverter fun urgency(v: String?) = v?.let { runCatching { CostUrgency.valueOf(it) }.getOrDefault(CostUrgency.OPTIONAL) }
    @TypeConverter fun urgency(v: CostUrgency?) = v?.name
    @TypeConverter fun costType(v: String?) = v?.let { runCatching { CostType.valueOf(it) }.getOrDefault(CostType.FIXED) }
    @TypeConverter fun costType(v: CostType?) = v?.name
    @TypeConverter fun importStatus(v: String?) = v?.let { runCatching { ImportStatus.valueOf(it) }.getOrDefault(ImportStatus.FAILED) }
    @TypeConverter fun importStatus(v: ImportStatus?) = v?.name
    @TypeConverter fun listingState(v: String?) = v?.let { runCatching { ListingState.valueOf(it) }.getOrDefault(ListingState.UNKNOWN) }
    @TypeConverter fun listingState(v: ListingState?) = v?.name
    @TypeConverter fun prefType(v: String?) = v?.let { runCatching { PreferenceType.valueOf(it) }.getOrDefault(PreferenceType.OFF) }
    @TypeConverter fun prefType(v: PreferenceType?) = v?.name
    @TypeConverter fun envRating(v: String?) = v?.let { runCatching { EnvRating.valueOf(it) }.getOrDefault(EnvRating.UNKNOWN) }
    @TypeConverter fun envRating(v: EnvRating?) = v?.name
    @TypeConverter fun crackDir(v: String?) = v?.let { runCatching { CrackDirection.valueOf(it) }.getOrDefault(CrackDirection.VERTICAL) }
    @TypeConverter fun crackDir(v: CrackDirection?) = v?.name
    @TypeConverter fun roomType(v: String?) = v?.let { runCatching { RoomType.valueOf(it) }.getOrDefault(RoomType.OTHER) }
    @TypeConverter fun roomType(v: RoomType?) = v?.name
}
