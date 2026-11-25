package nl.ecannemeijer.casacheck.data.backup

import kotlinx.serialization.Serializable
import nl.ecannemeijer.casacheck.data.db.*
import nl.ecannemeijer.casacheck.data.settings.AppSettings

@Serializable
data class BackupManifest(
    val format: String = FORMAT,
    val exportVersion: Int = EXPORT_VERSION,
    val databaseVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val propertyCount: Int,
    val dataSha256: String,
    /** relative path inside the zip -> sha256 */
    val files: Map<String, String>,
) {
    companion object { const val FORMAT = "casacheck-backup"; const val EXPORT_VERSION = 1 }
}

@Serializable
data class PropertyBundle(
    val property: Property,
    val features: List<PropertyFeature> = emptyList(),
    val listings: List<PropertyListing> = emptyList(),
    val prices: List<PropertyPriceHistory> = emptyList(),
    val floors: List<Floor> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val items: List<InspectionItem> = emptyList(),
    val results: List<InspectionResult> = emptyList(),
    val sessions: List<InspectionSession> = emptyList(),
    val projects: List<RepairProject> = emptyList(),
    val defects: List<Defect> = emptyList(),
    val moisture: List<MoistureMeasurement> = emptyList(),
    val moisturePoints: List<MoistureMeasurementPoint> = emptyList(),
    val cracks: List<CrackMeasurement> = emptyList(),
    val costs: List<CostEstimate> = emptyList(),
    val documents: List<Document> = emptyList(),
    val documentFiles: List<DocumentFile> = emptyList(),
    val photos: List<Photo> = emptyList(),
    val listingPhotos: List<PropertyPhoto> = emptyList(),
    val annotations: List<PhotoAnnotation> = emptyList(),
    val photoLinks: List<PhotoInspectionLink> = emptyList(),
    val answers: List<AgentAnswer> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val imports: List<ImportHistory> = emptyList(),
    val environment: List<EnvironmentRating> = emptyList(),
    val distances: List<DestinationDistance> = emptyList(),
)

@Serializable
data class BackupData(
    val properties: List<PropertyBundle>,
    val questions: List<AgentQuestion> = emptyList(),
    val preferences: List<PropertyPreference> = emptyList(),
    val destinations: List<Destination> = emptyList(),
    val settings: AppSettings? = null,
)

data class BackupInfo(val manifest: BackupManifest, val data: BackupData, val duplicates: List<Pair<String, String>>)
data class RestoreReport(val added: Int, val merged: Int, val skipped: Int, val photos: Int)
enum class DuplicateMode { MERGE, SKIP }

class BackupException(val reason: Reason, msg: String = reason.name) : Exception(msg) {
    enum class Reason { NOT_A_BACKUP, NEWER_VERSION, INTEGRITY, UNSAFE_PATH, TOO_LARGE, CORRUPT }
}
