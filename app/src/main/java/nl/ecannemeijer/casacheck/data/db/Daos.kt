package nl.ecannemeijer.casacheck.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Aggregated numbers per property for the list cards and comparison. */
data class PropertyStats(
    val propertyId: Long,
    val totalItems: Int,
    val checkedItems: Int,
    val minor: Int,
    val major: Int,
    val serious: Int,
    val specialist: Int,
)

@Dao
interface PropertyDao {
    @Query("SELECT * FROM property WHERE archived = :archived ORDER BY updatedAt DESC")
    fun observeAll(archived: Boolean = false): Flow<List<Property>>

    @Query("SELECT * FROM property ORDER BY updatedAt DESC")
    fun observeEverything(): Flow<List<Property>>

    @Query("SELECT * FROM property")
    suspend fun all(): List<Property>

    @Query("SELECT * FROM property WHERE id = :id")
    fun observe(id: Long): Flow<Property?>

    @Query("SELECT * FROM property WHERE id = :id")
    suspend fun get(id: Long): Property?

    @Query("SELECT * FROM property WHERE uuid = :uuid")
    suspend fun byUuid(uuid: String): Property?

    @Query("SELECT * FROM property WHERE (listingUrl IS NOT NULL AND listingUrl = :url) OR (sourceSite = :site AND listingId = :listingId AND listingId IS NOT NULL) LIMIT 1")
    suspend fun findByListing(url: String?, site: String?, listingId: String?): Property?

    @Query("SELECT * FROM property WHERE lower(title) = lower(:title) AND ifnull(lower(city),'') = ifnull(lower(:city),'') LIMIT 1")
    suspend fun findByTitleCity(title: String, city: String?): Property?

    @Insert suspend fun insert(p: Property): Long
    @Update suspend fun update(p: Property)
    @Query("DELETE FROM property WHERE id = :id") suspend fun delete(id: Long)

    @Query("UPDATE property SET lastOpenedAt = :t WHERE id = :id") suspend fun touchOpened(id: Long, t: Long = System.currentTimeMillis())
    @Query("UPDATE property SET favorite = :fav, updatedAt = :t WHERE id = :id") suspend fun setFavorite(id: Long, fav: Boolean, t: Long = System.currentTimeMillis())
    @Query("UPDATE property SET archived = :arch, updatedAt = :t WHERE id = :id") suspend fun setArchived(id: Long, arch: Boolean, t: Long = System.currentTimeMillis())
    @Query("UPDATE property SET mainPhotoId = :photoId WHERE id = :id") suspend fun setMainPhoto(id: Long, photoId: Long?)

    @Query("SELECT * FROM property WHERE lastOpenedAt IS NOT NULL ORDER BY lastOpenedAt DESC LIMIT 1")
    fun observeLastOpened(): Flow<Property?>

    @Query("""
        SELECT i.propertyId AS propertyId,
          COUNT(i.id) AS totalItems,
          SUM(CASE WHEN r.status IS NOT NULL AND r.status != 'NOT_CHECKED' THEN 1 ELSE 0 END) AS checkedItems,
          SUM(CASE WHEN r.status = 'MINOR' THEN 1 ELSE 0 END) AS minor,
          SUM(CASE WHEN r.status = 'MAJOR' THEN 1 ELSE 0 END) AS major,
          SUM(CASE WHEN r.status = 'SERIOUS' THEN 1 ELSE 0 END) AS serious,
          SUM(CASE WHEN r.specialistNeeded = 1 THEN 1 ELSE 0 END) AS specialist
        FROM inspection_item i LEFT JOIN inspection_result r ON r.itemId = i.id
        GROUP BY i.propertyId
    """)
    fun observeStats(): Flow<List<PropertyStats>>
}

@Dao
interface FeatureDao {
    @Query("SELECT * FROM property_feature WHERE propertyId = :pid") fun observe(pid: Long): Flow<List<PropertyFeature>>
    @Query("SELECT * FROM property_feature WHERE propertyId = :pid") suspend fun list(pid: Long): List<PropertyFeature>
    @Upsert suspend fun upsert(f: PropertyFeature)
    @Query("DELETE FROM property_feature WHERE propertyId = :pid AND `key` = :key") suspend fun delete(pid: Long, key: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(f: PropertyFeature)
}

@Dao
interface ListingDao {
    @Query("SELECT * FROM property_listing WHERE propertyId = :pid LIMIT 1") fun observe(pid: Long): Flow<PropertyListing?>
    @Query("SELECT * FROM property_listing WHERE propertyId = :pid LIMIT 1") suspend fun get(pid: Long): PropertyListing?
    @Query("SELECT * FROM property_listing WHERE propertyId = :pid") suspend fun list(pid: Long): List<PropertyListing>
    @Upsert suspend fun upsert(l: PropertyListing): Long
    @Query("SELECT * FROM price_history WHERE propertyId = :pid ORDER BY recordedAt") fun observePrices(pid: Long): Flow<List<PropertyPriceHistory>>
    @Query("SELECT * FROM price_history WHERE propertyId = :pid ORDER BY recordedAt") suspend fun prices(pid: Long): List<PropertyPriceHistory>
    @Insert suspend fun insertPrice(p: PropertyPriceHistory): Long
    @Insert suspend fun insertImport(h: ImportHistory): Long
    @Query("DELETE FROM import_history WHERE propertyId = :pid") suspend fun deleteImports(pid: Long)
    @Query("SELECT * FROM import_history ORDER BY startedAt DESC LIMIT 50") fun observeImports(): Flow<List<ImportHistory>>
    @Query("SELECT * FROM import_history WHERE propertyId = :pid ORDER BY startedAt DESC") suspend fun importsFor(pid: Long): List<ImportHistory>
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM floor WHERE propertyId = :pid ORDER BY level") fun observeFloors(pid: Long): Flow<List<Floor>>
    @Query("SELECT * FROM floor WHERE propertyId = :pid ORDER BY level") suspend fun floors(pid: Long): List<Floor>
    @Insert suspend fun insertFloor(f: Floor): Long
    @Update suspend fun updateFloor(f: Floor)
    @Delete suspend fun deleteFloor(f: Floor)
    @Query("SELECT * FROM room WHERE propertyId = :pid ORDER BY sortOrder, id") fun observeRooms(pid: Long): Flow<List<Room>>
    @Query("SELECT * FROM room WHERE propertyId = :pid ORDER BY sortOrder, id") suspend fun rooms(pid: Long): List<Room>
    @Query("SELECT * FROM room WHERE id = :id") fun observeRoom(id: Long): Flow<Room?>
    @Query("SELECT * FROM room WHERE id = :id") suspend fun room(id: Long): Room?
    @Insert suspend fun insertRoom(r: Room): Long
    @Update suspend fun updateRoom(r: Room)
    @Query("DELETE FROM room WHERE id = :id") suspend fun deleteRoom(id: Long)
}

/** A checkpoint instance joined with its result. */
data class ItemWithResult(
    val id: Long,
    val propertyId: Long,
    val templateCode: String,
    val categoryCode: String,
    val roomId: Long?,
    val customTitle: String?,
    val customDescription: String?,
    val sourcePhotoId: Long?,
    val quick: Boolean,
    val sortOrder: Int,
    val status: CheckStatus?,
    val specialistNeeded: Boolean?,
    val notes: String?,
    val costMin: Long?,
    val costMax: Long?,
    val costOwn: Long?,
    val photoCount: Int,
) {
    val effectiveStatus get() = status ?: CheckStatus.NOT_CHECKED
}

@Dao
interface InspectionDao {
    @Upsert suspend fun upsertCategories(c: List<InspectionCategory>)
    @Upsert suspend fun upsertTemplates(t: List<InspectionTemplate>)
    @Query("SELECT * FROM inspection_template") suspend fun templates(): List<InspectionTemplate>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTemplate(t: InspectionTemplate)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertItems(items: List<InspectionItem>): List<Long>
    @Insert suspend fun insertItem(item: InspectionItem): Long
    @Update suspend fun updateItem(item: InspectionItem)
    @Query("SELECT * FROM inspection_item WHERE propertyId = :pid") suspend fun items(pid: Long): List<InspectionItem>
    @Query("SELECT * FROM inspection_item WHERE id = :id") suspend fun item(id: Long): InspectionItem?
    @Query("DELETE FROM inspection_item WHERE id IN (:ids)") suspend fun deleteItems(ids: List<Long>)

    @Query("""
        SELECT i.id, i.propertyId, i.templateCode, i.categoryCode, i.roomId, i.customTitle, i.customDescription, i.sourcePhotoId,
               i.quick, i.sortOrder, r.status, r.specialistNeeded, r.notes, r.costMin, r.costMax, r.costOwn,
               (SELECT COUNT(*) FROM photo_inspection_link l WHERE l.itemId = i.id) AS photoCount
        FROM inspection_item i LEFT JOIN inspection_result r ON r.itemId = i.id
        WHERE i.propertyId = :pid ORDER BY i.sortOrder, i.id
    """)
    fun observeItems(pid: Long): Flow<List<ItemWithResult>>

    @Query("""
        SELECT i.id, i.propertyId, i.templateCode, i.categoryCode, i.roomId, i.customTitle, i.customDescription, i.sourcePhotoId,
               i.quick, i.sortOrder, r.status, r.specialistNeeded, r.notes, r.costMin, r.costMax, r.costOwn,
               (SELECT COUNT(*) FROM photo_inspection_link l WHERE l.itemId = i.id) AS photoCount
        FROM inspection_item i LEFT JOIN inspection_result r ON r.itemId = i.id
        WHERE i.propertyId = :pid ORDER BY i.sortOrder, i.id
    """)
    suspend fun itemsWithResults(pid: Long): List<ItemWithResult>

    @Query("""
        SELECT i.id, i.propertyId, i.templateCode, i.categoryCode, i.roomId, i.customTitle, i.customDescription, i.sourcePhotoId,
               i.quick, i.sortOrder, r.status, r.specialistNeeded, r.notes, r.costMin, r.costMax, r.costOwn,
               (SELECT COUNT(*) FROM photo_inspection_link l WHERE l.itemId = i.id) AS photoCount
        FROM inspection_item i LEFT JOIN inspection_result r ON r.itemId = i.id WHERE i.id = :id
    """)
    fun observeItem(id: Long): Flow<ItemWithResult?>

    @Query("SELECT * FROM inspection_result WHERE itemId = :itemId") suspend fun result(itemId: Long): InspectionResult?
    @Query("SELECT r.* FROM inspection_result r JOIN inspection_item i ON i.id = r.itemId WHERE i.propertyId = :pid") suspend fun results(pid: Long): List<InspectionResult>
    @Upsert suspend fun upsertResult(r: InspectionResult): Long

    @Insert suspend fun insertSession(s: InspectionSession): Long
    @Update suspend fun updateSession(s: InspectionSession)
    @Query("SELECT * FROM inspection_session WHERE propertyId = :pid ORDER BY startedAt DESC LIMIT 1") suspend fun lastSession(pid: Long): InspectionSession?
    @Query("SELECT * FROM inspection_session WHERE propertyId = :pid ORDER BY startedAt DESC LIMIT 1") fun observeLastSession(pid: Long): Flow<InspectionSession?>
    @Query("SELECT * FROM inspection_session WHERE propertyId = :pid") suspend fun sessions(pid: Long): List<InspectionSession>
    @Query("SELECT * FROM inspection_session WHERE endedAt IS NULL ORDER BY startedAt DESC") fun observeOpenSessions(): Flow<List<InspectionSession>>
}

@Dao
interface DefectDao {
    @Query("SELECT * FROM defect WHERE propertyId = :pid ORDER BY severity DESC, createdAt DESC") fun observe(pid: Long): Flow<List<Defect>>
    @Query("SELECT * FROM defect WHERE propertyId = :pid") suspend fun list(pid: Long): List<Defect>
    @Query("SELECT * FROM defect") fun observeAll(): Flow<List<Defect>>
    @Query("SELECT * FROM defect WHERE id = :id") suspend fun get(id: Long): Defect?
    @Query("SELECT * FROM defect WHERE id = :id") fun observeOne(id: Long): Flow<Defect?>
    @Query("SELECT * FROM defect WHERE itemId = :itemId") suspend fun byItem(itemId: Long): Defect?
    @Query("SELECT * FROM defect WHERE propertyId = :pid AND lower(title) = lower(:title) AND ((roomId IS NULL AND :roomId IS NULL) OR roomId = :roomId) LIMIT 1")
    suspend fun findSimilar(pid: Long, title: String, roomId: Long?): Defect?
    @Insert suspend fun insert(d: Defect): Long
    @Update suspend fun update(d: Defect)
    @Query("DELETE FROM defect WHERE id = :id") suspend fun delete(id: Long)

    @Query("SELECT * FROM repair_project WHERE propertyId = :pid ORDER BY name") fun observeProjects(pid: Long): Flow<List<RepairProject>>
    @Query("SELECT * FROM repair_project WHERE propertyId = :pid") suspend fun projects(pid: Long): List<RepairProject>
    @Query("SELECT * FROM repair_project") fun observeAllProjects(): Flow<List<RepairProject>>
    @Insert suspend fun insertProject(p: RepairProject): Long
    @Update suspend fun updateProject(p: RepairProject)
    @Query("DELETE FROM repair_project WHERE id = :id") suspend fun deleteProject(id: Long)

    @Query("SELECT * FROM cost_estimate WHERE propertyId = :pid") fun observeCosts(pid: Long): Flow<List<CostEstimate>>
    @Query("SELECT * FROM cost_estimate WHERE propertyId = :pid") suspend fun costs(pid: Long): List<CostEstimate>
    @Query("SELECT * FROM cost_estimate") fun observeAllCosts(): Flow<List<CostEstimate>>
    @Insert suspend fun insertCost(c: CostEstimate): Long
    @Update suspend fun updateCost(c: CostEstimate)
    @Query("DELETE FROM cost_estimate WHERE id = :id") suspend fun deleteCost(id: Long)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM moisture_measurement WHERE propertyId = :pid ORDER BY measuredAt DESC") fun observeMoisture(pid: Long): Flow<List<MoistureMeasurement>>
    @Query("SELECT * FROM moisture_measurement WHERE propertyId = :pid ORDER BY measuredAt DESC") suspend fun moisture(pid: Long): List<MoistureMeasurement>
    @Query("SELECT * FROM moisture_measurement WHERE id = :id") suspend fun moistureById(id: Long): MoistureMeasurement?
    @Insert suspend fun insertMoisture(m: MoistureMeasurement): Long
    @Update suspend fun updateMoisture(m: MoistureMeasurement)
    @Query("DELETE FROM moisture_measurement WHERE id = :id") suspend fun deleteMoisture(id: Long)
    @Query("SELECT * FROM moisture_point WHERE measurementId = :mid ORDER BY heightCm") suspend fun points(mid: Long): List<MoistureMeasurementPoint>
    @Query("SELECT p.* FROM moisture_point p JOIN moisture_measurement m ON m.id = p.measurementId WHERE m.propertyId = :pid") fun observePoints(pid: Long): Flow<List<MoistureMeasurementPoint>>
    @Query("SELECT p.* FROM moisture_point p JOIN moisture_measurement m ON m.id = p.measurementId WHERE m.propertyId = :pid") suspend fun pointsFor(pid: Long): List<MoistureMeasurementPoint>
    @Query("DELETE FROM moisture_point WHERE measurementId = :mid") suspend fun clearPoints(mid: Long)
    @Insert suspend fun insertPoints(p: List<MoistureMeasurementPoint>)

    @Query("SELECT * FROM crack_measurement WHERE propertyId = :pid ORDER BY measuredAt DESC") fun observeCracks(pid: Long): Flow<List<CrackMeasurement>>
    @Query("SELECT * FROM crack_measurement WHERE propertyId = :pid ORDER BY measuredAt DESC") suspend fun cracks(pid: Long): List<CrackMeasurement>
    @Insert suspend fun insertCrack(c: CrackMeasurement): Long
    @Update suspend fun updateCrack(c: CrackMeasurement)
    @Query("DELETE FROM crack_measurement WHERE id = :id") suspend fun deleteCrack(id: Long)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo WHERE propertyId = :pid ORDER BY kind, sortOrder, takenAt") fun observe(pid: Long): Flow<List<Photo>>
    @Query("SELECT * FROM photo WHERE propertyId = :pid ORDER BY kind, sortOrder, takenAt") suspend fun list(pid: Long): List<Photo>
    @Query("SELECT * FROM photo") suspend fun all(): List<Photo>
    @Query("SELECT * FROM photo WHERE id = :id") suspend fun get(id: Long): Photo?
    @Query("SELECT * FROM photo WHERE id = :id") fun observeOne(id: Long): Flow<Photo?>
    @Query("SELECT * FROM photo WHERE propertyId = :pid AND sha256 = :sha LIMIT 1") suspend fun bySha(pid: Long, sha: String): Photo?
    @Query("SELECT p.* FROM photo p JOIN photo_inspection_link l ON l.photoId = p.id WHERE l.itemId = :itemId ORDER BY p.takenAt") fun observeForItem(itemId: Long): Flow<List<Photo>>
    @Query("SELECT p.* FROM photo p JOIN photo_inspection_link l ON l.photoId = p.id WHERE l.itemId = :itemId ORDER BY p.takenAt") suspend fun forItem(itemId: Long): List<Photo>
    @Query("SELECT * FROM photo WHERE defectId = :defectId") suspend fun forDefect(defectId: Long): List<Photo>
    @Query("SELECT * FROM photo WHERE roomId = :roomId") fun observeForRoom(roomId: Long): Flow<List<Photo>>
    @Query("SELECT COUNT(*) FROM photo WHERE propertyId = :pid") suspend fun count(pid: Long): Int
    /** Photos that can serve as card image (listing photos first, in gallery order). */
    @Query("SELECT * FROM photo WHERE kind IN ('LISTING','INSPECTION') ORDER BY propertyId, CASE kind WHEN 'LISTING' THEN 0 ELSE 1 END, sortOrder, takenAt")
    fun observeMainCandidates(): Flow<List<Photo>>
    @Query("SELECT * FROM photo WHERE propertyId = :pid AND kind = :kind ORDER BY sortOrder, takenAt") fun observeKind(pid: Long, kind: PhotoKind): Flow<List<Photo>>
    @Query("SELECT * FROM photo WHERE propertyId = :pid AND originalUrl = :url LIMIT 1") suspend fun byOriginalUrl(pid: Long, url: String): Photo?
    @Insert suspend fun insert(p: Photo): Long
    @Update suspend fun update(p: Photo)
    @Query("DELETE FROM photo WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT COUNT(*) FROM photo WHERE filePath = :path OR thumbPath = :path OR editedPath = :path") suspend fun countUsingFile(path: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun link(l: PhotoInspectionLink)
    @Query("DELETE FROM photo_inspection_link WHERE photoId = :photoId AND itemId = :itemId") suspend fun unlink(photoId: Long, itemId: Long)
    @Query("SELECT * FROM photo_inspection_link WHERE photoId = :photoId") suspend fun linksForPhoto(photoId: Long): List<PhotoInspectionLink>
    @Query("SELECT l.* FROM photo_inspection_link l JOIN photo p ON p.id = l.photoId WHERE p.propertyId = :pid") suspend fun linksForProperty(pid: Long): List<PhotoInspectionLink>

    @Query("SELECT * FROM photo_annotation WHERE photoId = :photoId") suspend fun annotation(photoId: Long): PhotoAnnotation?
    @Query("SELECT a.* FROM photo_annotation a JOIN photo p ON p.id = a.photoId WHERE p.propertyId = :pid") suspend fun annotationsFor(pid: Long): List<PhotoAnnotation>
    @Upsert suspend fun upsertAnnotation(a: PhotoAnnotation)
    @Query("DELETE FROM photo_annotation WHERE photoId = :photoId") suspend fun deleteAnnotation(photoId: Long)

    @Query("SELECT * FROM property_photo WHERE propertyId = :pid ORDER BY position") suspend fun listingPhotos(pid: Long): List<PropertyPhoto>
    @Query("SELECT * FROM property_photo WHERE propertyId = :pid ORDER BY position") fun observeListingPhotos(pid: Long): Flow<List<PropertyPhoto>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertListingPhoto(p: PropertyPhoto): Long
    @Update suspend fun updateListingPhoto(p: PropertyPhoto)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM document WHERE propertyId = :pid") fun observe(pid: Long): Flow<List<Document>>
    @Query("SELECT * FROM document WHERE propertyId = :pid") suspend fun list(pid: Long): List<Document>
    @Query("SELECT * FROM document") fun observeAll(): Flow<List<Document>>
    @Query("SELECT * FROM document WHERE propertyId = :pid AND typeCode = :code") suspend fun get(pid: Long, code: String): Document?
    @Upsert suspend fun upsert(d: Document): Long
    @Query("SELECT f.* FROM document_file f JOIN document d ON d.id = f.documentId WHERE d.propertyId = :pid") fun observeFiles(pid: Long): Flow<List<DocumentFile>>
    @Query("SELECT f.* FROM document_file f JOIN document d ON d.id = f.documentId WHERE d.propertyId = :pid") suspend fun files(pid: Long): List<DocumentFile>
    @Insert suspend fun insertFile(f: DocumentFile): Long
    @Query("DELETE FROM document_file WHERE id = :id") suspend fun deleteFile(id: Long)
}

@Dao
interface AgentDao {
    @Query("SELECT * FROM agent_question ORDER BY sortOrder, id") fun observeQuestions(): Flow<List<AgentQuestion>>
    @Query("SELECT * FROM agent_question ORDER BY sortOrder, id") suspend fun questions(): List<AgentQuestion>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertQuestions(q: List<AgentQuestion>)
    @Insert suspend fun insertQuestion(q: AgentQuestion): Long
    @Query("DELETE FROM agent_question WHERE id = :id AND builtIn = 0") suspend fun deleteCustom(id: Long)
    @Query("SELECT * FROM agent_answer WHERE propertyId = :pid") fun observeAnswers(pid: Long): Flow<List<AgentAnswer>>
    @Query("SELECT * FROM agent_answer WHERE propertyId = :pid") suspend fun answers(pid: Long): List<AgentAnswer>
    @Upsert suspend fun upsertAnswer(a: AgentAnswer)
    @Query("SELECT * FROM agent_answer WHERE propertyId = :pid AND questionId = :qid") suspend fun answer(pid: Long, qid: Long): AgentAnswer?
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM property_preference") fun observe(): Flow<List<PropertyPreference>>
    @Query("SELECT * FROM property_preference") suspend fun list(): List<PropertyPreference>
    @Upsert suspend fun upsert(p: PropertyPreference)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointment WHERE done = 0 AND startsAt >= :from ORDER BY startsAt") fun observeUpcoming(from: Long): Flow<List<Appointment>>
    @Query("SELECT * FROM appointment WHERE propertyId = :pid ORDER BY startsAt") fun observeFor(pid: Long): Flow<List<Appointment>>
    @Query("SELECT * FROM appointment WHERE propertyId = :pid ORDER BY startsAt") suspend fun listFor(pid: Long): List<Appointment>
    @Upsert suspend fun upsert(a: Appointment): Long
    @Query("DELETE FROM appointment WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface EnvironmentDao {
    @Query("SELECT * FROM environment_rating WHERE propertyId = :pid") fun observe(pid: Long): Flow<List<EnvironmentRating>>
    @Query("SELECT * FROM environment_rating WHERE propertyId = :pid") suspend fun list(pid: Long): List<EnvironmentRating>
    @Query("SELECT * FROM environment_rating") fun observeAll(): Flow<List<EnvironmentRating>>
    @Upsert suspend fun upsert(r: EnvironmentRating)
    @Query("SELECT * FROM destination ORDER BY name") fun observeDestinations(): Flow<List<Destination>>
    @Query("SELECT * FROM destination") suspend fun destinations(): List<Destination>
    @Upsert suspend fun upsertDestination(d: Destination): Long
    @Query("DELETE FROM destination WHERE id = :id") suspend fun deleteDestination(id: Long)
    @Query("SELECT * FROM destination_distance WHERE propertyId = :pid") fun observeDistances(pid: Long): Flow<List<DestinationDistance>>
    @Query("SELECT * FROM destination_distance WHERE propertyId = :pid") suspend fun distances(pid: Long): List<DestinationDistance>
    @Query("SELECT * FROM destination_distance") fun observeAllDistances(): Flow<List<DestinationDistance>>
    @Upsert suspend fun upsertDistance(d: DestinationDistance)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM report ORDER BY createdAt DESC") fun observeAll(): Flow<List<Report>>
    @Insert suspend fun insert(r: Report): Long
    @Query("DELETE FROM report WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM report WHERE propertyId = :pid") suspend fun forProperty(pid: Long): List<Report>
}
