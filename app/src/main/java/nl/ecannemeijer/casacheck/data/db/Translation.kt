package nl.ecannemeijer.casacheck.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Cached machine translation of a listing text field (e.g. "title", "description").
 * [sourceHash] identifies the original text, so a changed listing text is translated again.
 */
@Entity(
    tableName = "translation",
    foreignKeys = [ForeignKey(Property::class, ["id"], ["propertyId"], onDelete = CASCADE)],
    indices = [Index("propertyId"), Index(value = ["propertyId", "field", "targetLang"], unique = true)]
)
data class Translation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val field: String,
    val sourceHash: String,
    val sourceLang: String,
    val targetLang: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translation WHERE propertyId = :pid") fun observe(pid: Long): Flow<List<Translation>>
    @Query("SELECT * FROM translation WHERE propertyId = :pid AND field = :field AND targetLang = :target") suspend fun get(pid: Long, field: String, target: String): Translation?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(t: Translation): Long
    @Query("DELETE FROM translation WHERE propertyId = :pid") suspend fun deleteFor(pid: Long)
    @Query("SELECT COUNT(*) FROM translation") suspend fun count(): Int
    @Query("DELETE FROM translation WHERE propertyId = :pid AND field LIKE :prefix || '%'") suspend fun deleteFieldPrefix(pid: Long, prefix: String)
}