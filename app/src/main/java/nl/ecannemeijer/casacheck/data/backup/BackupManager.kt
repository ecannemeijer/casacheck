package nl.ecannemeijer.casacheck.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.ecannemeijer.casacheck.BuildConfig
import nl.ecannemeijer.casacheck.data.db.*
import nl.ecannemeijer.casacheck.data.files.FileStore
import nl.ecannemeijer.casacheck.data.settings.AppSettings
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ZIP backup / restore via the Storage Access Framework (the UI passes SAF Uris).
 * Layout: manifest.json, data.json, files/<relative path in filesDir>.
 * Restore validates format, versions, SHA-256 of data and every file, and blocks unsafe paths before touching the database.
 */
class BackupManager(private val context: Context, private val db: CasaDatabase, private val files: FileStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun collect(propertyIds: List<Long>?): BackupData = withContext(Dispatchers.IO) {
        val ids = propertyIds ?: db.propertyDao().all().map { it.id }
        val bundles = ids.mapNotNull { pid ->
            val p = db.propertyDao().get(pid) ?: return@mapNotNull null
            val items = db.inspectionDao().items(pid)
            val moisture = db.measurementDao().moisture(pid)
            PropertyBundle(
                property = p, features = db.featureDao().list(pid), listings = db.listingDao().list(pid), prices = db.listingDao().prices(pid),
                floors = db.roomDao().floors(pid), rooms = db.roomDao().rooms(pid), items = items, results = db.inspectionDao().results(pid),
                sessions = db.inspectionDao().sessions(pid), projects = db.defectDao().projects(pid), defects = db.defectDao().list(pid),
                moisture = moisture, moisturePoints = db.measurementDao().pointsFor(pid), cracks = db.measurementDao().cracks(pid),
                costs = db.defectDao().costs(pid), documents = db.documentDao().list(pid), documentFiles = db.documentDao().files(pid),
                photos = db.photoDao().list(pid), listingPhotos = db.photoDao().listingPhotos(pid), annotations = db.photoDao().annotationsFor(pid),
                photoLinks = db.photoDao().linksForProperty(pid), answers = db.agentDao().answers(pid), appointments = db.appointmentDao().listFor(pid),
                imports = db.listingDao().importsFor(pid), environment = db.environmentDao().list(pid), distances = db.environmentDao().distances(pid),
                recordings = db.recordingDao().list(pid).filter { it.status == nl.ecannemeijer.casacheck.data.db.RecStatus.DONE },
                recordingMarkers = db.recordingDao().markersForProperty(pid), transcriptSegments = db.recordingDao().segmentsForProperty(pid),
            )
        }
        BackupData(bundles, db.agentDao().questions().filter { !it.builtIn }, db.preferenceDao().list(), db.environmentDao().destinations())
    }

    /** Writes the backup. Returns the number of properties. */
    suspend fun export(out: OutputStream, propertyIds: List<Long>?, settings: AppSettings?): Int = withContext(Dispatchers.IO) {
        val data = collect(propertyIds).copy(settings = settings)
        val dataBytes = json.encodeToString(data).toByteArray()
        val fileRefs = data.properties.flatMap { b ->
            b.photos.flatMap { listOfNotNull(it.filePath, it.thumbPath, it.editedPath) } + b.documentFiles.map { it.filePath } + b.recordings.map { it.filePath }
        }.distinct().filter { files.resolve(it)?.exists() == true && ZipSafety.isSafeName(it) }
        val hashes = fileRefs.associate { "files/$it" to FileStore.sha256(files.resolve(it)!!) }
        val manifest = BackupManifest(databaseVersion = CasaDatabase.VERSION, appVersion = BuildConfig.VERSION_NAME,
            createdAt = System.currentTimeMillis(), propertyCount = data.properties.size, dataSha256 = FileStore.sha256(dataBytes), files = hashes)
        ZipOutputStream(out.buffered()).use { z ->
            z.putNextEntry(ZipEntry("manifest.json")); z.write(json.encodeToString(manifest).toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("data.json")); z.write(dataBytes); z.closeEntry()
            for (rel in fileRefs) {
                z.putNextEntry(ZipEntry("files/$rel"))
                files.resolve(rel)!!.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
        data.properties.size
    }

    suspend fun exportToUri(uri: Uri, propertyIds: List<Long>?, settings: AppSettings?): Int =
        context.contentResolver.openOutputStream(uri)?.use { export(it, propertyIds, settings) } ?: error("cannot open output")

    private fun workDir() = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }

    /** Extracts and fully validates a backup. Nothing is written to the database. */
    suspend fun inspect(input: InputStream): Pair<BackupInfo, File> = withContext(Dispatchers.IO) {
        val dir = workDir()
        try {
            ZipSafety.extract(input, dir)
        } catch (e: BackupException) { throw e } catch (e: Exception) { throw BackupException(BackupException.Reason.CORRUPT, e.message ?: "corrupt") }
        val mf = File(dir, "manifest.json"); val df = File(dir, "data.json")
        if (!mf.exists() || !df.exists()) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        val manifest = runCatching { json.decodeFromString<BackupManifest>(mf.readText()) }.getOrElse { throw BackupException(BackupException.Reason.NOT_A_BACKUP) }
        if (manifest.format != BackupManifest.FORMAT) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        if (manifest.exportVersion > BackupManifest.EXPORT_VERSION || manifest.databaseVersion > CasaDatabase.VERSION) throw BackupException(BackupException.Reason.NEWER_VERSION)
        if (FileStore.sha256(df) != manifest.dataSha256) throw BackupException(BackupException.Reason.INTEGRITY, "data.json checksum")
        for ((name, sha) in manifest.files) {
            val f = ZipSafety.safeTarget(dir, name)
            if (!f.exists() || FileStore.sha256(f) != sha) throw BackupException(BackupException.Reason.INTEGRITY, "file $name")
        }
        val data = runCatching { json.decodeFromString<BackupData>(df.readText()) }.getOrElse { throw BackupException(BackupException.Reason.CORRUPT) }
        val dups = data.properties.mapNotNull { b ->
            val local = db.propertyDao().byUuid(b.property.uuid)
                ?: b.property.listingUrl?.let { db.propertyDao().findByListing(it, b.property.sourceSite, b.property.listingId) }
            local?.let { b.property.title to it.title }
        }
        BackupInfo(manifest, data, dups) to dir
    }

    /** Restores a validated backup. Existing data is never deleted; duplicates are merged (only missing records are added) or skipped. */
    suspend fun restore(info: BackupInfo, dir: File, mode: DuplicateMode, applySettings: (AppSettings) -> Unit = {}): RestoreReport = withContext(Dispatchers.IO) {
        var added = 0; var merged = 0; var skipped = 0; var photos = 0
        db.withTransaction {
            // global data
            val questionMap = mutableMapOf<Long, Long>()
            val localQuestions = db.agentDao().questions()
            for (q in localQuestions) questionMap[q.id] = q.id
            for (q in info.data.questions) {
                val existing = localQuestions.firstOrNull { it.code == q.code }
                questionMap[q.id] = existing?.id ?: db.agentDao().insertQuestion(q.copy(id = 0))
            }
            val destMap = mutableMapOf<Long, Long>()
            val localDest = db.environmentDao().destinations()
            for (d in info.data.destinations) {
                destMap[d.id] = localDest.firstOrNull { it.name == d.name && it.query == d.query }?.id ?: db.environmentDao().upsertDestination(d.copy(id = 0))
            }
            val localPrefs = db.preferenceDao().list().map { it.key }.toSet()
            info.data.preferences.filter { it.key !in localPrefs }.forEach { db.preferenceDao().upsert(it) }

            for (b in info.data.properties) {
                val local = db.propertyDao().byUuid(b.property.uuid)
                    ?: b.property.listingUrl?.let { db.propertyDao().findByListing(it, b.property.sourceSite, b.property.listingId) }
                if (local != null && mode == DuplicateMode.SKIP) { skipped++; continue }
                val pid = local?.id ?: db.propertyDao().insert(b.property.copy(id = 0, mainPhotoId = null))
                if (local == null) added++ else merged++
                photos += restoreBundle(b, pid, local != null, dir, questionMap, destMap)
            }
        }
        info.data.settings?.let(applySettings)
        dir.deleteRecursively()
        RestoreReport(added, merged, skipped, photos)
    }

    private fun copyFile(dir: File, rel: String?, propertyUuid: String): String? {
        if (rel == null) return null
        val src = ZipSafety.safeTarget(dir, "files/$rel")
        if (!src.exists()) return null
        // Keep the file name but place it under the (possibly different) local property folder.
        val parts = rel.split('/')
        val target = File(files.root, (listOf(parts.first(), propertyUuid) + parts.drop(2)).joinToString("/"))
        if (!target.canonicalPath.startsWith(files.root.canonicalPath)) return null
        target.parentFile?.mkdirs()
        if (!target.exists()) src.copyTo(target)
        return files.relative(target)
    }

    private suspend fun restoreBundle(b: PropertyBundle, pid: Long, merging: Boolean, dir: File, qMap: Map<Long, Long>, dMap: Map<Long, Long>): Int {
        val pUuid = db.propertyDao().get(pid)!!.uuid
        val localFeatures = db.featureDao().list(pid).map { it.key }.toSet()
        b.features.filter { it.key !in localFeatures }.forEach { db.featureDao().upsert(it.copy(id = 0, propertyId = pid)) }
        if (db.listingDao().list(pid).isEmpty()) b.listings.forEach { db.listingDao().upsert(it.copy(id = 0, propertyId = pid)) }
        val localPrices = db.listingDao().prices(pid).map { it.price to it.recordedAt }.toSet()
        b.prices.filter { (it.price to it.recordedAt) !in localPrices }.forEach { db.listingDao().insertPrice(it.copy(id = 0, propertyId = pid)) }

        val floorMap = mutableMapOf<Long, Long>()
        val localFloors = db.roomDao().floors(pid).associateBy { it.uuid }
        b.floors.forEach { f -> floorMap[f.id] = localFloors[f.uuid]?.id ?: db.roomDao().insertFloor(f.copy(id = 0, propertyId = pid)) }
        val roomMap = mutableMapOf<Long, Long>()
        val localRooms = db.roomDao().rooms(pid).associateBy { it.uuid }
        b.rooms.forEach { r -> roomMap[r.id] = localRooms[r.uuid]?.id ?: db.roomDao().insertRoom(r.copy(id = 0, propertyId = pid, floorId = r.floorId?.let { floorMap[it] })) }

        val itemMap = mutableMapOf<Long, Long>()
        val localItems = db.inspectionDao().items(pid)
        val localByUuid = localItems.associateBy { it.uuid }
        val localByKey = localItems.associateBy { it.templateCode to it.roomId }
        for (i in b.items) {
            val rid = i.roomId?.let { roomMap[it] }
            val existing = localByUuid[i.uuid] ?: localByKey[i.templateCode to rid]
            itemMap[i.id] = existing?.id ?: db.inspectionDao().insertItem(i.copy(id = 0, propertyId = pid, roomId = rid, sourcePhotoId = null))
        }
        for (r in b.results) {
            val iid = itemMap[r.itemId] ?: continue
            val localR = db.inspectionDao().result(iid)
            if (localR == null || (merging && localR.status == CheckStatus.NOT_CHECKED && localR.notes == null)) {
                db.inspectionDao().upsertResult(r.copy(id = localR?.id ?: 0, itemId = iid))
            }
        }
        if (!merging) b.sessions.forEach { db.inspectionDao().insertSession(it.copy(id = 0, propertyId = pid, roomId = it.roomId?.let { r -> roomMap[r] }, lastItemId = it.lastItemId?.let { x -> itemMap[x] })) }

        val projMap = mutableMapOf<Long, Long>()
        val localProj = db.defectDao().projects(pid).associateBy { it.uuid }
        b.projects.forEach { p -> projMap[p.id] = localProj[p.uuid]?.id ?: db.defectDao().insertProject(p.copy(id = 0, propertyId = pid)) }
        val defMap = mutableMapOf<Long, Long>()
        val localDef = db.defectDao().list(pid)
        for (d in b.defects) {
            val iid = d.itemId?.let { itemMap[it] }
            val existing = localDef.firstOrNull { it.uuid == d.uuid } ?: iid?.let { db.defectDao().byItem(it) }
            defMap[d.id] = existing?.id ?: db.defectDao().insert(d.copy(id = 0, propertyId = pid, roomId = d.roomId?.let { roomMap[it] }, itemId = iid,
                repairProjectId = d.repairProjectId?.let { projMap[it] }))
        }
        val moistMap = mutableMapOf<Long, Long>()
        val localMoist = db.measurementDao().moisture(pid).associateBy { it.uuid }
        for (m in b.moisture) {
            val ex = localMoist[m.uuid]
            moistMap[m.id] = ex?.id ?: db.measurementDao().insertMoisture(m.copy(id = 0, propertyId = pid, roomId = m.roomId?.let { roomMap[it] })).also { newId ->
                db.measurementDao().insertPoints(b.moisturePoints.filter { it.measurementId == m.id }.map { it.copy(id = 0, measurementId = newId) })
            }
        }
        val crackMap = mutableMapOf<Long, Long>()
        val localCracks = db.measurementDao().cracks(pid).associateBy { it.uuid }
        for (c in b.cracks.sortedBy { it.measuredAt }) {
            crackMap[c.id] = localCracks[c.uuid]?.id ?: db.measurementDao().insertCrack(c.copy(id = 0, propertyId = pid, roomId = c.roomId?.let { roomMap[it] }, previousId = c.previousId?.let { crackMap[it] }))
        }
        val localCosts = db.defectDao().costs(pid).map { it.uuid }.toSet()
        b.costs.filter { it.uuid !in localCosts }.forEach { db.defectDao().insertCost(it.copy(id = 0, propertyId = pid, repairProjectId = it.repairProjectId?.let { p -> projMap[p] })) }

        val docMap = mutableMapOf<Long, Long>()
        for (d in b.documents) {
            val ex = db.documentDao().get(pid, d.typeCode)
            docMap[d.id] = if (ex == null) db.documentDao().upsert(d.copy(id = 0, propertyId = pid))
            else { if (ex.status == DocumentStatus.NOT_REQUESTED && d.status != DocumentStatus.NOT_REQUESTED) db.documentDao().upsert(ex.copy(status = d.status, notes = ex.notes ?: d.notes)); ex.id }
        }
        val localDocShas = db.documentDao().files(pid).mapNotNull { it.sha256 }.toSet()
        for (f in b.documentFiles) {
            val did = docMap[f.documentId] ?: continue
            if (f.sha256 != null && f.sha256 in localDocShas) continue
            val path = copyFile(dir, f.filePath, pUuid) ?: continue
            db.documentDao().insertFile(f.copy(id = 0, documentId = did, filePath = path))
        }

        val photoMap = mutableMapOf<Long, Long>()
        var n = 0
        val localPhotos = db.photoDao().list(pid)
        for (ph in b.photos) {
            val ex = localPhotos.firstOrNull { it.uuid == ph.uuid } ?: ph.sha256?.let { db.photoDao().bySha(pid, it) }
            if (ex != null) { photoMap[ph.id] = ex.id; continue }
            val path = copyFile(dir, ph.filePath, pUuid) ?: continue
            photoMap[ph.id] = db.photoDao().insert(ph.copy(id = 0, propertyId = pid, filePath = path, thumbPath = copyFile(dir, ph.thumbPath, pUuid),
                editedPath = copyFile(dir, ph.editedPath, pUuid), roomId = ph.roomId?.let { roomMap[it] }, defectId = ph.defectId?.let { defMap[it] },
                moistureId = ph.moistureId?.let { moistMap[it] }, crackId = ph.crackId?.let { crackMap[it] }, documentId = ph.documentId?.let { docMap[it] }))
            n++
        }
        b.listingPhotos.forEach { lp -> photoMap[lp.photoId]?.let { db.photoDao().insertListingPhoto(lp.copy(id = 0, propertyId = pid, photoId = it)) } }
        b.annotations.forEach { a -> photoMap[a.photoId]?.let { if (db.photoDao().annotation(it) == null) db.photoDao().upsertAnnotation(a.copy(id = 0, photoId = it)) } }
        b.photoLinks.forEach { l -> val p = photoMap[l.photoId]; val i = itemMap[l.itemId]; if (p != null && i != null) db.photoDao().link(PhotoInspectionLink(p, i)) }
        if (db.propertyDao().get(pid)?.mainPhotoId == null) b.property.mainPhotoId?.let { photoMap[it] }?.let { db.propertyDao().setMainPhoto(pid, it) }
        // custom item source photos
        for (i in b.items.filter { it.sourcePhotoId != null }) {
            val iid = itemMap[i.id] ?: continue; val phid = photoMap[i.sourcePhotoId!!] ?: continue
            db.inspectionDao().item(iid)?.takeIf { it.sourcePhotoId == null }?.let { db.inspectionDao().updateItem(it.copy(sourcePhotoId = phid)) }
        }

        for (a in b.answers) {
            val qid = qMap[a.questionId] ?: continue
            if (db.agentDao().answer(pid, qid) == null) db.agentDao().upsertAnswer(a.copy(id = 0, propertyId = pid, questionId = qid))
        }
        val localAppts = db.appointmentDao().listFor(pid).map { it.startsAt }.toSet()
        b.appointments.filter { it.startsAt !in localAppts }.forEach { db.appointmentDao().upsert(it.copy(id = 0, propertyId = pid)) }
        if (!merging) b.imports.forEach { db.listingDao().insertImport(it.copy(id = 0, propertyId = pid)) }
        val localEnv = db.environmentDao().list(pid).map { it.itemCode }.toSet()
        b.environment.filter { it.itemCode !in localEnv }.forEach { db.environmentDao().upsert(it.copy(id = 0, propertyId = pid)) }
        val localDist = db.environmentDao().distances(pid).map { it.destinationId }.toSet()
        b.distances.forEach { d -> dMap[d.destinationId]?.takeIf { it !in localDist }?.let { db.environmentDao().upsertDistance(d.copy(id = 0, propertyId = pid, destinationId = it)) } }
        // Viewing recordings (audio file + markers + transcript); skipped when the same recording already exists here.
        val localRecs = db.recordingDao().list(pid).map { it.uuid }.toSet()
        for (r in b.recordings) {
            if (r.uuid in localRecs) continue
            val path = copyFile(dir, r.filePath, pUuid) ?: continue
            val rid = db.recordingDao().insert(r.copy(id = 0, propertyId = pid, filePath = path,
                transcriptStatus = if (r.transcriptStatus == nl.ecannemeijer.casacheck.data.db.TxStatus.DONE) r.transcriptStatus else nl.ecannemeijer.casacheck.data.db.TxStatus.NONE))
            b.recordingMarkers.filter { it.recordingId == r.id }.forEach { m -> db.recordingDao().insertMarker(m.copy(id = 0, recordingId = rid, itemId = m.itemId?.let { itemMap[it] })) }
            db.recordingDao().insertSegments(b.transcriptSegments.filter { it.recordingId == r.id }.map { it.copy(id = 0, recordingId = rid) })
        }
        return n
    }
}
