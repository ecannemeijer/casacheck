package nl.ecannemeijer.casacheck.data.repo

import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.ecannemeijer.casacheck.data.db.AgentAnswer
import nl.ecannemeijer.casacheck.data.db.AgentQuestion
import nl.ecannemeijer.casacheck.data.db.Appointment
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.db.CostEstimate
import nl.ecannemeijer.casacheck.data.db.CrackMeasurement
import nl.ecannemeijer.casacheck.data.db.Defect
import nl.ecannemeijer.casacheck.data.db.Destination
import nl.ecannemeijer.casacheck.data.db.DestinationDistance
import nl.ecannemeijer.casacheck.data.db.Document
import nl.ecannemeijer.casacheck.data.db.DocumentFile
import nl.ecannemeijer.casacheck.data.db.DocumentStatus
import nl.ecannemeijer.casacheck.data.db.EnvRating
import nl.ecannemeijer.casacheck.data.db.EnvironmentRating
import nl.ecannemeijer.casacheck.data.db.Floor
import nl.ecannemeijer.casacheck.data.db.MoistureMeasurement
import nl.ecannemeijer.casacheck.data.db.MoistureMeasurementPoint
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.data.db.PhotoAnnotation
import nl.ecannemeijer.casacheck.data.db.PhotoInspectionLink
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.data.db.PreferenceType
import nl.ecannemeijer.casacheck.data.db.PropertyPreference
import nl.ecannemeijer.casacheck.data.db.RepairProject
import nl.ecannemeijer.casacheck.data.db.Room
import nl.ecannemeijer.casacheck.data.files.FileStore
import nl.ecannemeijer.casacheck.data.settings.SettingsStore
import nl.ecannemeijer.casacheck.domain.content.ContentLibrary
import nl.ecannemeijer.casacheck.domain.cost.CostCalculator
import nl.ecannemeijer.casacheck.domain.score.PropertySnapshot
import java.io.File

class RoomRepository(private val db: CasaDatabase, private val inspections: InspectionRepository) {
    private val dao = db.roomDao()
    fun observeFloors(pid: Long) = dao.observeFloors(pid)
    fun observeRooms(pid: Long) = dao.observeRooms(pid)
    fun observeRoom(id: Long) = dao.observeRoom(id)
    suspend fun rooms(pid: Long) = dao.rooms(pid)
    suspend fun addFloor(f: Floor) = dao.insertFloor(f)
    suspend fun updateFloor(f: Floor) = dao.updateFloor(f)
    suspend fun deleteFloor(f: Floor) = dao.deleteFloor(f)
    /** Adding a room generates its room-type checklist. */
    suspend fun addRoom(r: Room): Long { val id = dao.insertRoom(r); inspections.regenerate(r.propertyId); return id }
    suspend fun updateRoom(r: Room) { val old = dao.room(r.id); dao.updateRoom(r); if (old?.type != r.type) inspections.regenerate(r.propertyId) }
    suspend fun deleteRoom(r: Room) { dao.deleteRoom(r.id); inspections.regenerate(r.propertyId) }
}

class DefectRepository(private val db: CasaDatabase) {
    private val dao = db.defectDao()
    fun observe(pid: Long) = dao.observe(pid)
    fun observeOne(id: Long) = dao.observeOne(id)
    fun observeProjects(pid: Long) = dao.observeProjects(pid)
    fun observeCosts(pid: Long) = dao.observeCosts(pid)
    suspend fun get(id: Long) = dao.get(id)
    /** Returns an existing defect with the same title in the same room, to prevent duplicates. */
    suspend fun findSimilar(pid: Long, title: String, roomId: Long?) = dao.findSimilar(pid, title.trim(), roomId)
    suspend fun save(d: Defect): Long = if (d.id == 0L) dao.insert(d) else { dao.update(d); d.id }
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun saveProject(p: RepairProject): Long = if (p.id == 0L) dao.insertProject(p) else { dao.updateProject(p); p.id }
    suspend fun deleteProject(id: Long) = dao.deleteProject(id)
    suspend fun saveCost(c: CostEstimate): Long = if (c.id == 0L) dao.insertCost(c) else { dao.updateCost(c); c.id }
    suspend fun deleteCost(id: Long) = dao.deleteCost(id)
    /** Links several defects to one repair project so their costs are counted once. */
    suspend fun linkToProject(defectIds: List<Long>, projectId: Long?) = db.withTransaction {
        defectIds.forEach { id -> dao.get(id)?.let { dao.update(it.copy(repairProjectId = projectId)) } }
    }
}

class PhotoRepository(private val db: CasaDatabase, private val files: FileStore) {
    private val dao = db.photoDao()
    fun observe(pid: Long) = dao.observe(pid)
    fun observeOne(id: Long) = dao.observeOne(id)
    fun observeForItem(itemId: Long) = dao.observeForItem(itemId)
    fun observeForRoom(roomId: Long) = dao.observeForRoom(roomId)
    fun observeListingPhotos(pid: Long) = dao.observeListingPhotos(pid)
    suspend fun get(id: Long) = dao.get(id)
    suspend fun list(pid: Long) = dao.list(pid)

    private suspend fun uuidOf(pid: Long) = db.propertyDao().get(pid)?.uuid ?: error("property not found")

    /** Stores a photo permanently; duplicates (same SHA-256 within the property) return the existing photo id. */
    suspend fun addFromFile(pid: Long, file: File, kind: PhotoKind, itemId: Long? = null, roomId: Long? = null, defectId: Long? = null,
                            moistureId: Long? = null, crackId: Long? = null, documentId: Long? = null): Long = withContext(Dispatchers.IO) {
        val stored = file.inputStream().use { files.storeImage(uuidOf(pid), it) }
        file.delete()
        insertStored(pid, stored, kind, itemId, roomId, defectId, moistureId, crackId, documentId)
    }

    suspend fun addFromUri(pid: Long, uri: Uri, kind: PhotoKind, itemId: Long? = null, roomId: Long? = null, defectId: Long? = null,
                           moistureId: Long? = null, crackId: Long? = null, documentId: Long? = null): Long = withContext(Dispatchers.IO) {
        val stored = files.storeImageFromUri(uuidOf(pid), uri)
        insertStored(pid, stored, kind, itemId, roomId, defectId, moistureId, crackId, documentId)
    }

    private suspend fun insertStored(pid: Long, s: nl.ecannemeijer.casacheck.data.files.StoredImage, kind: PhotoKind, itemId: Long?, roomId: Long?,
                                     defectId: Long?, moistureId: Long?, crackId: Long?, documentId: Long?): Long = db.withTransaction {
        val dup = dao.bySha(pid, s.sha256)
        val id = if (dup != null) { files.delete(s.path); files.delete(s.thumbPath); dup.id } else dao.insert(Photo(
            propertyId = pid, kind = kind, filePath = s.path, thumbPath = s.thumbPath, sha256 = s.sha256, mimeType = s.mime,
            width = s.width, height = s.height, roomId = roomId, defectId = defectId, moistureId = moistureId, crackId = crackId, documentId = documentId,
        ))
        if (itemId != null) dao.link(PhotoInspectionLink(id, itemId))
        id
    }

    suspend fun update(p: Photo) = dao.update(p)
    suspend fun link(photoId: Long, itemId: Long) = dao.link(PhotoInspectionLink(photoId, itemId))
    suspend fun unlink(photoId: Long, itemId: Long) = dao.unlink(photoId, itemId)
    suspend fun links(photoId: Long) = dao.linksForPhoto(photoId)

    /**
     * Deletes photos everywhere: the row (checkpoint links, annotation and listing-gallery entry cascade), the main-photo
     * reference of the property, and the original/thumbnail/annotated files (unless another row still uses the same file).
     */
    suspend fun delete(photo: Photo) = deleteAll(listOf(photo))

    suspend fun deleteAll(photos: List<Photo>): Unit = withContext(Dispatchers.IO) {
        if (photos.isEmpty()) return@withContext
        val unused = db.withTransaction {
            photos.forEach { ph ->
                dao.delete(ph.id)
                db.propertyDao().get(ph.propertyId)?.takeIf { it.mainPhotoId == ph.id }?.let { db.propertyDao().setMainPhoto(it.id, null) }
            }
            photos.flatMap { listOf(it.filePath, it.thumbPath, it.editedPath) }.filterNotNull().distinct().filter { dao.countUsingFile(it) == 0 }
        }
        unused.forEach { files.delete(it) }
    }

    suspend fun annotation(photoId: Long) = dao.annotation(photoId)

    /** Annotations are stored as JSON next to the untouched original; [renderedJpeg] is a separate edited copy. */
    suspend fun saveAnnotation(photo: Photo, shapesJson: String, renderedJpeg: ByteArray?) = withContext(Dispatchers.IO) {
        dao.upsertAnnotation(PhotoAnnotation(photoId = photo.id, shapesJson = shapesJson))
        val edited = if (renderedJpeg != null) {
            val f = File(files.editedDir(db.propertyDao().get(photo.propertyId)!!.uuid), "${photo.uuid}.jpg")
            f.writeBytes(renderedJpeg); files.relative(f)
        } else null
        dao.update(photo.copy(editedPath = edited))
    }

    suspend fun clearAnnotation(photo: Photo) = withContext(Dispatchers.IO) {
        dao.deleteAnnotation(photo.id); files.delete(photo.editedPath); dao.update(photo.copy(editedPath = null))
    }

    fun fileOf(rel: String?) = files.resolve(rel)
}

class MeasurementRepository(private val db: CasaDatabase) {
    private val dao = db.measurementDao()
    fun observeMoisture(pid: Long) = dao.observeMoisture(pid)
    fun observePoints(pid: Long) = dao.observePoints(pid)
    fun observeCracks(pid: Long) = dao.observeCracks(pid)
    suspend fun moisture(id: Long) = dao.moistureById(id)
    suspend fun points(mid: Long) = dao.points(mid)

    /** Saves a measurement and its points (null values are allowed: "not measured"). */
    suspend fun saveMoisture(m: MoistureMeasurement, points: List<Pair<Int, Double?>>): Long = db.withTransaction {
        val id = if (m.id == 0L) dao.insertMoisture(m) else { dao.updateMoisture(m); m.id }
        dao.clearPoints(id)
        dao.insertPoints(points.distinctBy { it.first }.map { MoistureMeasurementPoint(measurementId = id, heightCm = it.first, value = it.second) })
        id
    }
    suspend fun deleteMoisture(id: Long) = dao.deleteMoisture(id)
    suspend fun saveCrack(c: CrackMeasurement): Long = if (c.id == 0L) dao.insertCrack(c) else { dao.updateCrack(c); c.id }
    suspend fun deleteCrack(id: Long) = dao.deleteCrack(id)
}

class DocumentRepository(private val db: CasaDatabase, private val files: FileStore) {
    private val dao = db.documentDao()
    fun observe(pid: Long) = dao.observe(pid)
    fun observeFiles(pid: Long) = dao.observeFiles(pid)
    suspend fun setStatus(pid: Long, code: String, status: DocumentStatus) {
        val d = dao.get(pid, code) ?: Document(propertyId = pid, typeCode = code)
        dao.upsert(d.copy(status = status, updatedAt = System.currentTimeMillis()))
    }
    suspend fun setNotes(pid: Long, code: String, notes: String?) {
        val d = dao.get(pid, code) ?: Document(propertyId = pid, typeCode = code)
        dao.upsert(d.copy(notes = notes, updatedAt = System.currentTimeMillis()))
    }
    private suspend fun ensure(pid: Long, code: String): Long = dao.get(pid, code)?.id ?: dao.upsert(Document(propertyId = pid, typeCode = code))
    suspend fun addFile(pid: Long, code: String, uri: Uri, name: String, mime: String) = withContext(Dispatchers.IO) {
        val docId = ensure(pid, code)
        val (path, sha) = files.storeDocument(db.propertyDao().get(pid)!!.uuid, uri, name)
        dao.insertFile(DocumentFile(documentId = docId, filePath = path, displayName = name, mimeType = mime, sha256 = sha))
        dao.get(pid, code)?.takeIf { it.status == DocumentStatus.NOT_REQUESTED || it.status == DocumentStatus.REQUESTED }
            ?.let { dao.upsert(it.copy(status = DocumentStatus.RECEIVED)) }
    }
    suspend fun deleteFile(f: DocumentFile) { dao.deleteFile(f.id); files.delete(f.filePath) }
    suspend fun documentId(pid: Long, code: String) = ensure(pid, code)
}

class AgentRepository(private val db: CasaDatabase) {
    private val dao = db.agentDao()
    suspend fun seed() = dao.insertQuestions(ContentLibrary.agentQuestions.mapIndexed { i, q -> AgentQuestion(code = q.code, sortOrder = i) })
    fun observeQuestions() = dao.observeQuestions()
    fun observeAnswers(pid: Long) = dao.observeAnswers(pid)
    suspend fun addCustom(nl: String, it: String?) = dao.insertQuestion(AgentQuestion(code = "custom_" + java.util.UUID.randomUUID(), builtIn = false, customNl = nl, customIt = it, sortOrder = 1000))
    suspend fun deleteCustom(id: Long) = dao.deleteCustom(id)
    suspend fun saveAnswer(pid: Long, qid: Long, answer: String?, notes: String?, asked: Boolean) {
        val a = dao.answer(pid, qid) ?: AgentAnswer(propertyId = pid, questionId = qid)
        dao.upsertAnswer(a.copy(answer = answer, notes = notes, asked = asked, updatedAt = System.currentTimeMillis()))
    }
}

class EnvironmentRepository(private val db: CasaDatabase) {
    private val dao = db.environmentDao()
    fun observe(pid: Long) = dao.observe(pid)
    fun observeDestinations() = dao.observeDestinations()
    fun observeDistances(pid: Long) = dao.observeDistances(pid)
    suspend fun rate(pid: Long, code: String, rating: EnvRating, km: Double?, minutes: Int?, notes: String?) {
        val existing = dao.list(pid).firstOrNull { it.itemCode == code }
        dao.upsert((existing ?: EnvironmentRating(propertyId = pid, itemCode = code)).copy(rating = rating, distanceKm = km, travelMinutes = minutes, notes = notes))
    }
    suspend fun saveDestination(d: Destination) = dao.upsertDestination(d)
    suspend fun deleteDestination(id: Long) = dao.deleteDestination(id)
    suspend fun saveDistance(pid: Long, destId: Long, km: Double?, min: Int?) {
        val existing = dao.distances(pid).firstOrNull { it.destinationId == destId }
        dao.upsertDistance((existing ?: DestinationDistance(propertyId = pid, destinationId = destId)).copy(distanceKm = km, travelMinutes = min))
    }
}

class PreferenceRepository(private val db: CasaDatabase) {
    fun observe() = db.preferenceDao().observe()
    suspend fun list() = db.preferenceDao().list()
    suspend fun set(key: String, type: PreferenceType, value: Double?) = db.preferenceDao().upsert(PropertyPreference(key, type, value))
}

class AppointmentRepository(private val db: CasaDatabase) {
    fun observeUpcoming() = db.appointmentDao().observeUpcoming(System.currentTimeMillis() - 3 * 3600_000L)
    fun observeFor(pid: Long) = db.appointmentDao().observeFor(pid)
    suspend fun save(a: Appointment) = db.appointmentDao().upsert(a)
    suspend fun delete(id: Long) = db.appointmentDao().delete(id)
}

/** Builds complete snapshots for scoring, comparison and reports. */
class SnapshotRepository(private val db: CasaDatabase, private val settings: SettingsStore) {
    suspend fun snapshot(pid: Long): PropertySnapshot? {
        val p = db.propertyDao().get(pid) ?: return null
        val s = settings.current()
        val defects = db.defectDao().list(pid)
        return PropertySnapshot(
            property = p,
            features = db.featureDao().list(pid).associate { it.key to it.value },
            items = db.inspectionDao().itemsWithResults(pid),
            defects = defects,
            costs = CostCalculator.summarize(p.askingPrice, p.ownOffer, p.purchaseCostPercent ?: s.purchaseCostPercent,
                p.contingencyPercent ?: s.contingencyPercent, defects, db.defectDao().projects(pid), db.defectDao().costs(pid)),
            documents = db.documentDao().list(pid),
            environment = db.environmentDao().list(pid),
            destinations = db.environmentDao().destinations(),
            distances = db.environmentDao().distances(pid),
        )
    }
}
