package nl.ecannemeijer.casacheck.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Recording states (stored as text). */
object RecStatus {
    /** Being recorded right now (raw .aac file, finalized to .m4a on stop or after a crash). */
    const val RECORDING = "RECORDING"
    const val DONE = "DONE"
}

/** Transcript states (stored as text). */
object TxStatus {
    const val NONE = "NONE"; const val QUEUED = "QUEUED"; const val RUNNING = "RUNNING"; const val DONE = "DONE"; const val FAILED = "FAILED"
}

/** An audio recording of a viewing (bezichtiging), per property. File path is relative to filesDir. */
@Serializable
@Entity(
    tableName = "recording",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId"), Index("uuid", unique = true)]
)
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val propertyId: Long,
    val name: String,
    val filePath: String,
    val mime: String = "audio/mp4",
    val durationMs: Long = 0,
    val sizeBytes: Long = 0,
    val status: String = RecStatus.DONE,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val transcriptStatus: String = TxStatus.NONE,
    val transcriptLang: String? = null,
    val transcriptError: String? = null,
    val transcriptModel: String? = null,
)

/** A moment marked during recording ("Markeer moment"), optionally linked to the checkpoint that was open. */
@Serializable
@Entity(
    tableName = "recording_marker",
    foreignKeys = [
        ForeignKey(Recording::class, ["id"], ["recordingId"], onDelete = CASCADE),
        ForeignKey(InspectionItem::class, ["id"], ["itemId"], onDelete = SET_NULL),
    ],
    indices = [Index("recordingId"), Index("itemId")]
)
data class RecordingMarker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val positionMs: Long,
    val label: String? = null,
    val itemId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One timed transcript segment (Whisper output). */
@Serializable
@Entity(
    tableName = "transcript_segment",
    foreignKeys = [ForeignKey(Recording::class, ["id"], ["recordingId"], onDelete = CASCADE)],
    indices = [Index("recordingId")]
)
data class TranscriptSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val idx: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recording WHERE propertyId = :pid ORDER BY createdAt DESC") fun observe(pid: Long): Flow<List<Recording>>
    @Query("SELECT * FROM recording WHERE id = :id") fun observeOne(id: Long): Flow<Recording?>
    @Query("SELECT * FROM recording WHERE id = :id") suspend fun get(id: Long): Recording?
    @Query("SELECT * FROM recording WHERE propertyId = :pid ORDER BY createdAt") suspend fun list(pid: Long): List<Recording>
    @Query("SELECT * FROM recording WHERE status = :status") suspend fun withStatus(status: String): List<Recording>
    @Query("SELECT * FROM recording WHERE transcriptStatus IN ('QUEUED','RUNNING')") suspend fun pendingTranscripts(): List<Recording>
    @Insert suspend fun insert(r: Recording): Long
    @Update suspend fun update(r: Recording)
    @Query("DELETE FROM recording WHERE id = :id") suspend fun delete(id: Long)
    @Query("UPDATE recording SET transcriptStatus = :status, transcriptError = :error WHERE id = :id") suspend fun setTranscriptStatus(id: Long, status: String, error: String?)

    @Query("SELECT * FROM recording_marker WHERE recordingId = :rid ORDER BY positionMs") fun observeMarkers(rid: Long): Flow<List<RecordingMarker>>
    @Query("SELECT * FROM recording_marker WHERE recordingId = :rid ORDER BY positionMs") suspend fun markers(rid: Long): List<RecordingMarker>
    @Query("SELECT m.* FROM recording_marker m JOIN recording r ON r.id = m.recordingId WHERE r.propertyId = :pid") suspend fun markersForProperty(pid: Long): List<RecordingMarker>
    @Insert suspend fun insertMarker(m: RecordingMarker): Long
    @Query("DELETE FROM recording_marker WHERE id = :id") suspend fun deleteMarker(id: Long)

    @Query("SELECT * FROM transcript_segment WHERE recordingId = :rid ORDER BY idx") fun observeSegments(rid: Long): Flow<List<TranscriptSegment>>
    @Query("SELECT * FROM transcript_segment WHERE recordingId = :rid ORDER BY idx") suspend fun segments(rid: Long): List<TranscriptSegment>
    @Query("SELECT s.* FROM transcript_segment s JOIN recording r ON r.id = s.recordingId WHERE r.propertyId = :pid") suspend fun segmentsForProperty(pid: Long): List<TranscriptSegment>
    @Insert suspend fun insertSegments(l: List<TranscriptSegment>)
    @Query("DELETE FROM transcript_segment WHERE recordingId = :rid") suspend fun deleteSegments(rid: Long)
}