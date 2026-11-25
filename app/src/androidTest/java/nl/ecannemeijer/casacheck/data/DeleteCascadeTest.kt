package nl.ecannemeijer.casacheck.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.CrackMeasurement
import nl.ecannemeijer.casacheck.data.db.Defect
import nl.ecannemeijer.casacheck.data.db.Document
import nl.ecannemeijer.casacheck.data.db.DocumentFile
import nl.ecannemeijer.casacheck.data.db.Floor
import nl.ecannemeijer.casacheck.data.db.InspectionItem
import nl.ecannemeijer.casacheck.data.db.InspectionResult
import nl.ecannemeijer.casacheck.data.db.MoistureMeasurement
import nl.ecannemeijer.casacheck.data.db.MoistureMeasurementPoint
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.data.db.PhotoAnnotation
import nl.ecannemeijer.casacheck.data.db.PhotoInspectionLink
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.data.db.Property
import nl.ecannemeijer.casacheck.data.db.PropertyFeature
import nl.ecannemeijer.casacheck.data.db.Report
import nl.ecannemeijer.casacheck.data.db.Room
import nl.ecannemeijer.casacheck.data.files.FileStore
import nl.ecannemeijer.casacheck.data.repo.InspectionRepository
import nl.ecannemeijer.casacheck.data.repo.PhotoRepository
import nl.ecannemeijer.casacheck.data.repo.PropertyRepository
import nl.ecannemeijer.casacheck.data.settings.SettingsStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real Room (in-memory) + real app files dir: deleting a property or a photo removes every row and every file. */
@RunWith(AndroidJUnit4::class)
class DeleteCascadeTest {
    private lateinit var ctx: Context
    private lateinit var db: CasaDatabase
    private lateinit var files: FileStore
    private lateinit var props: PropertyRepository
    private lateinit var photos: PhotoRepository

    @Before fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        db = CasaDatabase.build(ctx, null)
        files = FileStore(ctx)
        props = PropertyRepository(db, files, SettingsStore(ctx), InspectionRepository(ctx, db))
        photos = PhotoRepository(db, files)
    }

    @After fun tearDown() { db.close() }

    private fun count(sql: String): Int = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $sql").use { it.moveToFirst(); it.getInt(0) }

    private fun file(dir: File, name: String) = File(dir, name).apply { writeText("test") }

    data class Seed(val pid: Long, val uuid: String, val itemId: Long, val photoIds: List<Long>, val docId: Long, val moistId: Long, val files: List<File>)

    private suspend fun seed(title: String): Seed {
        val pid = db.propertyDao().insert(Property(title = title))
        val uuid = db.propertyDao().get(pid)!!.uuid
        db.featureDao().upsert(PropertyFeature(propertyId = pid, key = "garden", value = "true", source = "user"))
        val floor = db.roomDao().insertFloor(Floor(propertyId = pid, name = "PT"))
        val room = db.roomDao().insertRoom(Room(propertyId = pid, floorId = floor, name = "Cucina"))
        val item = db.inspectionDao().insertItem(InspectionItem(propertyId = pid, templateCode = "custom_t", categoryCode = "walls", roomId = room))
        db.inspectionDao().upsertResult(InspectionResult(itemId = item, status = CheckStatus.MAJOR))
        val defect = db.defectDao().insert(Defect(propertyId = pid, roomId = room, itemId = item, title = "Scheur"))
        val moist = db.measurementDao().insertMoisture(MoistureMeasurement(propertyId = pid, roomId = room, wall = "N"))
        db.measurementDao().insertPoints(listOf(MoistureMeasurementPoint(measurementId = moist, heightCm = 10, value = 30.0)))
        val crack = db.measurementDao().insertCrack(CrackMeasurement(propertyId = pid, roomId = room, location = "muur"))
        val doc = db.documentDao().upsert(Document(propertyId = pid, typeCode = "ape"))

        val created = mutableListOf<File>()
        val ids = (1..2).map { n ->
            val orig = file(files.photosDir(uuid), "p$n.jpg"); val thumb = file(files.thumbsDir(uuid), "p$n.jpg"); val ed = file(files.editedDir(uuid), "p$n.jpg")
            created += listOf(orig, thumb, ed)
            val id = db.photoDao().insert(Photo(propertyId = pid, kind = PhotoKind.DEFECT, filePath = files.relative(orig), thumbPath = files.relative(thumb),
                editedPath = files.relative(ed), roomId = room, defectId = defect, moistureId = moist, crackId = crack))
            db.photoDao().link(PhotoInspectionLink(id, item))
            db.photoDao().upsertAnnotation(PhotoAnnotation(photoId = id, shapesJson = "[]"))
            id
        }
        db.propertyDao().setMainPhoto(pid, ids[0])
        val docFile = file(files.docsDir(uuid), "ape.pdf"); created += docFile
        db.documentDao().insertFile(DocumentFile(documentId = doc, filePath = files.relative(docFile), displayName = "ape.pdf", mimeType = "application/pdf"))
        val pdf = file(files.reportsDir(), "test_${uuid}.pdf"); created += pdf
        db.reportDao().insert(Report(propertyId = pid, type = "full", language = "nl", filePath = files.relative(pdf), title = title))
        return Seed(pid, uuid, item, ids, doc, moist, created)
    }

    private fun rowsOf(s: Seed): Int = listOf(
        "property WHERE id = ${s.pid}", "property_feature WHERE propertyId = ${s.pid}", "floor WHERE propertyId = ${s.pid}",
        "room WHERE propertyId = ${s.pid}", "inspection_item WHERE propertyId = ${s.pid}", "inspection_result WHERE itemId = ${s.itemId}",
        "defect WHERE propertyId = ${s.pid}", "moisture_measurement WHERE propertyId = ${s.pid}", "moisture_point WHERE measurementId = ${s.moistId}",
        "crack_measurement WHERE propertyId = ${s.pid}", "document WHERE propertyId = ${s.pid}", "document_file WHERE documentId = ${s.docId}",
        "photo WHERE propertyId = ${s.pid}", "photo_inspection_link WHERE itemId = ${s.itemId}",
        "photo_annotation WHERE photoId IN (${s.photoIds.joinToString()})", "report WHERE propertyId = ${s.pid}",
    ).sumOf { count(it) }

    @Test fun deletingPropertyRemovesAllRowsAndFilesButNotOtherProperty() = runBlocking<Unit> {
        val a = seed("A"); val b = seed("B")
        assertThat(rowsOf(a)).isEqualTo(19)
        assertThat(a.files.all { it.exists() }).isTrue()

        props.delete(a.pid)

        assertThat(rowsOf(a)).isEqualTo(0)
        assertThat(a.files.none { it.exists() }).isTrue()
        listOf("photos", "thumbs", "docs", "edited").forEach { assertThat(File(files.root, "$it/${a.uuid}").exists()).isFalse() }
        assertThat(rowsOf(b)).isEqualTo(19)
        assertThat(b.files.all { it.exists() }).isTrue()
        props.delete(b.pid)
        assertThat(b.files.none { it.exists() }).isTrue()
    }

    @Test fun deletingPhotoRemovesFilesLinksAnnotationAndMainPhoto() = runBlocking<Unit> {
        val s = seed("P")
        val ph = photos.get(s.photoIds[0])!!
        photos.delete(ph)

        assertThat(count("photo WHERE id = ${ph.id}")).isEqualTo(0)
        assertThat(count("photo_inspection_link WHERE photoId = ${ph.id}")).isEqualTo(0)
        assertThat(count("photo_annotation WHERE photoId = ${ph.id}")).isEqualTo(0)
        listOf(ph.filePath, ph.thumbPath, ph.editedPath).forEach { assertThat(files.resolve(it)!!.exists()).isFalse() }
        assertThat(db.propertyDao().get(s.pid)!!.mainPhotoId).isNull()
        // the rest stays: defect, the other photo and its files, the checkpoint
        assertThat(count("defect WHERE propertyId = ${s.pid}")).isEqualTo(1)
        assertThat(count("photo WHERE id = ${s.photoIds[1]}")).isEqualTo(1)
        assertThat(files.resolve(photos.get(s.photoIds[1])!!.filePath)!!.exists()).isTrue()
        assertThat(count("inspection_item WHERE id = ${s.itemId}")).isEqualTo(1)
        props.delete(s.pid)
    }

    @Test fun fileSharedByAnotherPhotoRowIsKept() = runBlocking<Unit> {
        val s = seed("S")
        val a = photos.get(s.photoIds[0])!!
        val twin = db.photoDao().insert(a.copy(id = 0, uuid = java.util.UUID.randomUUID().toString(), thumbPath = null, editedPath = null))
        photos.delete(a)
        assertThat(files.resolve(a.filePath)!!.exists()).isTrue()
        assertThat(files.resolve(a.thumbPath)!!.exists()).isFalse()
        photos.delete(photos.get(twin)!!)
        assertThat(files.resolve(a.filePath)!!.exists()).isFalse()
        props.delete(s.pid)
    }
}