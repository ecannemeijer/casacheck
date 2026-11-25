package nl.ecannemeijer.casacheck.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

fun newUuid(): String = UUID.randomUUID().toString()

/**
 * A property (woning). Unknown values stay null; nothing is ever invented.
 * Booleans are tri-state: null = unknown, true = yes, false = no.
 */
@Serializable
@Entity(tableName = "property", indices = [Index("uuid", unique = true), Index("listingUrl"), Index("archived"), Index("favorite")])
data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val title: String = "",
    val address: String? = null,
    val city: String? = null,
    val province: String? = null,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val askingPrice: Long? = null,
    val originalPrice: Long? = null,
    val pricePerSqm: Long? = null,
    val buildYear: Int? = null,
    val propertyType: String? = null,
    val livingArea: Double? = null,
    val plotArea: Double? = null,
    val rooms: Int? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val floors: Int? = null,
    val condition: String? = null,
    val energyLabel: String? = null,
    val energyConsumption: String? = null,
    val heatingType: String? = null,
    val garage: Boolean? = null,
    val parkingSpaces: Int? = null,
    val cellar: Boolean? = null,
    val attic: Boolean? = null,
    val balcony: Boolean? = null,
    val terrace: Boolean? = null,
    val garden: Boolean? = null,
    val airConditioning: Boolean? = null,
    val description: String? = null,
    val listingUrl: String? = null,
    val listingId: String? = null,
    val sourceSite: String? = null,
    val referenceNumber: String? = null,
    val agentName: String? = null,
    val agentPhone: String? = null,
    val agentEmail: String? = null,
    val viewingAt: Long? = null,
    val mainPhotoId: Long? = null,
    val notes: String? = null,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val personalRating: Int? = null,
    val ownOffer: Long? = null,
    val purchaseCostPercent: Double? = null,
    val contingencyPercent: Double? = null,
    val featuresConfirmed: Boolean = false,
    /** Comma separated field names that the user corrected by hand; listing refresh never overwrites these silently. */
    val manualFields: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
)

/** Extra features (key/value) such as HILLSIDE, SEPTIC_TANK, GAS, LIFT, DETACHED, orientation etc. */
@Serializable
@Entity(
    tableName = "property_feature",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index(value = ["propertyId", "key"], unique = true)]
)
data class PropertyFeature(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val key: String,
    val value: String?,
    /** "listing", "user", "ocr", "text" */
    val source: String = "user",
)

/** Listing (advertentie) state for refresh/history. */
@Serializable
@Entity(
    tableName = "property_listing",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId"), Index(value = ["site", "externalId"])]
)
data class PropertyListing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val site: String,
    val url: String,
    val externalId: String?,
    val state: ListingState = ListingState.UNKNOWN,
    val lastCheckedAt: Long? = null,
    val lastDescription: String? = null,
    val descriptionChangedAt: Long? = null,
    val lastPhotoUrls: String = "",
    val photosAdded: Int = 0,
    val photosRemoved: Int = 0,
    val lastMessage: String? = null,
)

@Serializable
@Entity(
    tableName = "price_history",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId")]
)
data class PropertyPriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val price: Long,
    val recordedAt: Long = System.currentTimeMillis(),
    val source: String = "user",
)

@Serializable
@Entity(
    tableName = "floor",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId"), Index("uuid", unique = true)]
)
data class Floor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val name: String,
    val level: Int = 0,
)

@Serializable
@Entity(
    tableName = "room",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(Floor::class, ["id"], ["floorId"], onDelete = SET_NULL),
    ],
    indices = [Index("propertyId"), Index("floorId"), Index("uuid", unique = true)]
)
data class Room(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val floorId: Long? = null,
    val name: String,
    val type: RoomType = RoomType.OTHER,
    val area: Double? = null,
    val dimensions: String? = null,
    val generalCondition: String? = null,
    val heating: String? = null,
    val windows: String? = null,
    val electricity: String? = null,
    val moisture: String? = null,
    val cracks: String? = null,
    val flooring: String? = null,
    val ceilings: String? = null,
    val notes: String? = null,
    val repairCost: Long? = null,
    val sortOrder: Int = 0,
)

/** Category of the checkpoint library (seeded from code, names via string resources). */
@Serializable
@Entity(tableName = "inspection_category")
data class InspectionCategory(
    @PrimaryKey val code: String,
    val sortOrder: Int,
)

/**
 * Checkpoint template. Built-in templates come from the CheckpointLibrary (texts in string resources);
 * user-created templates (e.g. from a pre-marked listing photo) carry their own title/description.
 */
@Serializable
@Entity(
    tableName = "inspection_template",
    foreignKeys = [ForeignKey(InspectionCategory::class, ["code"], ["categoryCode"], onDelete = CASCADE)],
    indices = [Index("categoryCode")]
)
data class InspectionTemplate(
    @PrimaryKey val code: String,
    val categoryCode: String,
    val builtIn: Boolean = true,
    val quick: Boolean = false,
    val costMin: Long? = null,
    val costMax: Long? = null,
    val sortOrder: Int = 0,
)

/** A checkpoint instance for one property (optionally in one room). */
@Serializable
@Entity(
    tableName = "inspection_item",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(Room::class, ["id"], ["roomId"], onDelete = CASCADE),
    ],
    indices = [Index("propertyId"), Index("roomId"), Index("templateCode"), Index("uuid", unique = true),
        Index(value = ["propertyId", "templateCode", "roomId"], unique = true)]
)
data class InspectionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val templateCode: String,
    val categoryCode: String,
    val roomId: Long? = null,
    val customTitle: String? = null,
    val customDescription: String? = null,
    val sourcePhotoId: Long? = null,
    val quick: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
@Entity(
    tableName = "inspection_result",
    foreignKeys = [ForeignKey(InspectionItem::class, ["id"], ["itemId"], onDelete = CASCADE)],
    indices = [Index(value = ["itemId"], unique = true)]
)
data class InspectionResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val status: CheckStatus = CheckStatus.NOT_CHECKED,
    val specialistNeeded: Boolean = false,
    val notes: String? = null,
    val costMin: Long? = null,
    val costMax: Long? = null,
    val costOwn: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "inspection_session",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId")]
)
data class InspectionSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val mode: InspectionMode,
    val roomId: Long? = null,
    val categoryCode: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val lastItemId: Long? = null,
)

@Serializable
@Entity(
    tableName = "repair_project",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId"), Index("uuid", unique = true)]
)
data class RepairProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val name: String,
    val costCategory: String,
    val urgency: CostUrgency = CostUrgency.WITHIN_YEAR,
    val costMin: Long? = null,
    val costMax: Long? = null,
    val costOwn: Long? = null,
    val quoteAmount: Long? = null,
    val quoteReceived: Boolean = false,
    val notes: String? = null,
)

@Serializable
@Entity(
    tableName = "defect",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(Room::class, ["id"], ["roomId"], onDelete = SET_NULL),
        ForeignKey(InspectionItem::class, ["id"], ["itemId"], onDelete = SET_NULL),
        ForeignKey(RepairProject::class, ["id"], ["repairProjectId"], onDelete = SET_NULL),
    ],
    indices = [Index("propertyId"), Index("roomId"), Index(value = ["itemId"], unique = true), Index("repairProjectId"), Index("uuid", unique = true)]
)
data class Defect(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val roomId: Long? = null,
    val itemId: Long? = null,
    val title: String,
    val description: String? = null,
    val categoryCode: String? = null,
    val severity: Severity = Severity.MINOR,
    val priority: Priority = Priority.WITHIN_YEAR,
    val cause: String? = null,
    val solution: String? = null,
    val notes: String? = null,
    val costMin: Long? = null,
    val costMax: Long? = null,
    val costOwn: Long? = null,
    val specialistNeeded: Boolean = false,
    val quoteReceived: Boolean = false,
    val status: DefectStatus = DefectStatus.OPEN,
    val repairProjectId: Long? = null,
    val isMoisture: Boolean = false,
    val isStructural: Boolean = false,
    val isSafety: Boolean = false,
    val isLegal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "moisture_measurement",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(Room::class, ["id"], ["roomId"], onDelete = SET_NULL),
    ],
    indices = [Index("propertyId"), Index("roomId"), Index("uuid", unique = true)]
)
data class MoistureMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val roomId: Long? = null,
    val floorName: String? = null,
    val wall: String,
    val measuredAt: Long = System.currentTimeMillis(),
    val meterType: String? = null,
    val method: String? = null,
    val unit: String? = null,
    val material: String? = null,
    val referenceValue: Double? = null,
    val temperature: Double? = null,
    val relativeHumidity: Double? = null,
    val notes: String? = null,
)

@Serializable
@Entity(
    tableName = "moisture_point",
    foreignKeys = [ForeignKey(MoistureMeasurement::class, ["id"], ["measurementId"], onDelete = CASCADE)],
    indices = [Index(value = ["measurementId", "heightCm"], unique = true)]
)
data class MoistureMeasurementPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measurementId: Long,
    val heightCm: Int,
    val value: Double?,
)

@Serializable
@Entity(
    tableName = "crack_measurement",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(Room::class, ["id"], ["roomId"], onDelete = SET_NULL),
        ForeignKey(CrackMeasurement::class, ["id"], ["previousId"], onDelete = SET_NULL),
    ],
    indices = [Index("propertyId"), Index("roomId"), Index("previousId"), Index("uuid", unique = true)]
)
data class CrackMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val roomId: Long? = null,
    val previousId: Long? = null,
    val location: String,
    val direction: CrackDirection = CrackDirection.VERTICAL,
    val lengthCm: Double? = null,
    val widthMm: Double? = null,
    val inside: Boolean = true,
    val measuredAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val changeNotes: String? = null,
)

@Serializable
@Entity(
    tableName = "cost_estimate",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(RepairProject::class, ["id"], ["repairProjectId"], onDelete = SET_NULL),
    ],
    indices = [Index("propertyId"), Index("repairProjectId"), Index("uuid", unique = true)]
)
data class CostEstimate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val repairProjectId: Long? = null,
    val costCategory: String,
    val description: String,
    val type: CostType = CostType.FIXED,
    val quantity: Double? = null,
    val unitPriceMin: Long? = null,
    val unitPriceMax: Long? = null,
    val materialCost: Long? = null,
    val laborCost: Long? = null,
    val costMin: Long? = null,
    val costMax: Long? = null,
    val costOwn: Long? = null,
    val quoteAmount: Long? = null,
    val quoteReceived: Boolean = false,
    val urgency: CostUrgency = CostUrgency.WITHIN_YEAR,
)

@Serializable
@Entity(
    tableName = "document",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index(value = ["propertyId", "typeCode"], unique = true)]
)
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val typeCode: String,
    val status: DocumentStatus = DocumentStatus.NOT_REQUESTED,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "document_file",
    foreignKeys = [ForeignKey(Document::class, ["id"], ["documentId"], onDelete = CASCADE)],
    indices = [Index("documentId")]
)
data class DocumentFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val filePath: String,
    val displayName: String,
    val mimeType: String,
    val sha256: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

/** A stored photo. Files live in app-private permanent storage (filesDir/photos), never only in cache. */
@Serializable
@Entity(
    tableName = "photo",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(Room::class, ["id"], ["roomId"], onDelete = SET_NULL),
        ForeignKey(Defect::class, ["id"], ["defectId"], onDelete = SET_NULL),
        ForeignKey(MoistureMeasurement::class, ["id"], ["moistureId"], onDelete = SET_NULL),
        ForeignKey(CrackMeasurement::class, ["id"], ["crackId"], onDelete = SET_NULL),
        ForeignKey(Document::class, ["id"], ["documentId"], onDelete = SET_NULL),
    ],
    indices = [Index("propertyId"), Index("roomId"), Index("defectId"), Index("moistureId"), Index("crackId"),
        Index("documentId"), Index(value = ["propertyId", "sha256"]), Index("uuid", unique = true)]
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val kind: PhotoKind,
    val filePath: String,
    val thumbPath: String? = null,
    val editedPath: String? = null,
    val originalUrl: String? = null,
    val sha256: String? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val caption: String? = null,
    val notes: String? = null,
    val takenAt: Long = System.currentTimeMillis(),
    val important: Boolean = false,
    val includeInReport: Boolean = true,
    val roomId: Long? = null,
    val defectId: Long? = null,
    val moistureId: Long? = null,
    val crackId: Long? = null,
    val documentId: Long? = null,
    val sortOrder: Int = 0,
)

/** Listing-gallery metadata of a photo (original order, CDN URL, removed from the ad). */
@Serializable
@Entity(
    tableName = "property_photo",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(Photo::class, ["id"], ["photoId"], onDelete = CASCADE),
    ],
    indices = [Index("propertyId"), Index(value = ["photoId"], unique = true)]
)
data class PropertyPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val photoId: Long,
    val sourceUrl: String,
    val position: Int,
    val tag: String? = null,
    val isFloorplan: Boolean = false,
    val removedFromListing: Boolean = false,
)

/** Vector annotations (arrows, circles, ...) as JSON; the original photo file is never changed. */
@Serializable
@Entity(
    tableName = "photo_annotation",
    foreignKeys = [ForeignKey(Photo::class, ["id"], ["photoId"], onDelete = CASCADE)],
    indices = [Index(value = ["photoId"], unique = true)]
)
data class PhotoAnnotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoId: Long,
    val shapesJson: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Many-to-many: one photo can belong to several checkpoints. */
@Serializable
@Entity(
    tableName = "photo_inspection_link",
    primaryKeys = ["photoId", "itemId"],
    foreignKeys = [
        ForeignKey(Photo::class, ["id"], ["photoId"], onDelete = CASCADE),
        ForeignKey(InspectionItem::class, ["id"], ["itemId"], onDelete = CASCADE),
    ],
    indices = [Index("itemId")]
)
data class PhotoInspectionLink(
    val photoId: Long,
    val itemId: Long,
)

@Serializable
@Entity(tableName = "agent_question", indices = [Index("code", unique = true)])
data class AgentQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Built-in questions have a code (texts in string resources); custom questions carry their own texts. */
    val code: String,
    val builtIn: Boolean = true,
    val customNl: String? = null,
    val customIt: String? = null,
    val sortOrder: Int = 0,
)

@Serializable
@Entity(
    tableName = "agent_answer",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(AgentQuestion::class, ["id"], ["questionId"], onDelete = CASCADE),
    ],
    indices = [Index(value = ["propertyId", "questionId"], unique = true), Index("questionId")]
)
data class AgentAnswer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val questionId: Long,
    val answer: String? = null,
    val notes: String? = null,
    val asked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Personal housing wishes (global). HARD = requirement, SOFT = preference. */
@Serializable
@Entity(tableName = "property_preference")
data class PropertyPreference(
    @PrimaryKey val key: String,
    val type: PreferenceType = PreferenceType.OFF,
    val numberValue: Double? = null,
)

@Serializable
@Entity(
    tableName = "appointment",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId"), Index("startsAt")]
)
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val startsAt: Long,
    val notes: String? = null,
    val done: Boolean = false,
)

@Serializable
@Entity(
    tableName = "import_history",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = SET_NULL)],
    indices = [Index("propertyId")]
)
data class ImportHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long? = null,
    val source: String,
    val url: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val status: ImportStatus,
    val message: String? = null,
    val fieldsFound: Int = 0,
    val photosFound: Int = 0,
    val photosImported: Int = 0,
)

/** Environment assessment per property (section 24). Distances are only ever entered by the user. */
@Serializable
@Entity(
    tableName = "environment_rating",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index(value = ["propertyId", "itemCode"], unique = true)]
)
data class EnvironmentRating(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val itemCode: String,
    val rating: EnvRating = EnvRating.UNKNOWN,
    val distanceKm: Double? = null,
    val travelMinutes: Int? = null,
    val notes: String? = null,
)

/** User-defined important destinations (e.g. Torre Bormida, Savona, family, hospital). */
@Serializable
@Entity(tableName = "destination")
data class Destination(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val query: String,
    val isFamily: Boolean = false,
    val isSea: Boolean = false,
)

@Serializable
@Entity(
    tableName = "destination_distance",
    foreignKeys = [
        ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE),
        ForeignKey(Destination::class, ["id"], ["destinationId"], onDelete = CASCADE),
    ],
    indices = [Index(value = ["propertyId", "destinationId"], unique = true), Index("destinationId")]
)
data class DestinationDistance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val destinationId: Long,
    val distanceKm: Double? = null,
    val travelMinutes: Int? = null,
    val notes: String? = null,
)

/** Generated PDF reports (file in filesDir/reports). */
@Serializable
@Entity(
    tableName = "report",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId")]
)
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long? = null,
    val type: String,
    val language: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val title: String,
)
