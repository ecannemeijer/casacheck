package nl.ecannemeijer.casacheck.data.repo

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.Defect
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.InspectionCategory
import nl.ecannemeijer.casacheck.data.db.InspectionItem
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.data.db.InspectionResult
import nl.ecannemeijer.casacheck.data.db.InspectionSession
import nl.ecannemeijer.casacheck.data.db.InspectionTemplate
import nl.ecannemeijer.casacheck.data.db.ItemWithResult
import nl.ecannemeijer.casacheck.data.db.Priority
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointDef
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.checklist.ChecklistGenerator
import nl.ecannemeijer.casacheck.domain.content.costCategoryFor

class InspectionRepository(private val context: Context, private val db: CasaDatabase) {
    private val dao = db.inspectionDao()
    val library: Map<String, CheckpointDef> get() = CheckpointLibrary.byCode

    /** Seeds categories and templates (idempotent; called at start-up). */
    suspend fun seedLibrary() {
        dao.upsertCategories(CheckpointLibrary.categories.map { InspectionCategory(it.code, it.order) })
        dao.upsertTemplates(CheckpointLibrary.all.map {
            InspectionTemplate(it.code, it.category, builtIn = true, quick = it.quick,
                costMin = it.costMinEur.takeIf { c -> c > 0 }, costMax = it.costMaxEur.takeIf { c -> c > 0 }, sortOrder = it.order)
        })
    }

    /** Adds checkpoints that newly apply (features/rooms changed). Existing items and results are never removed when checked. */
    suspend fun regenerate(propertyId: Long) = db.withTransaction {
        val p = db.propertyDao().get(propertyId) ?: return@withTransaction
        val features = db.featureDao().list(propertyId).associate { it.key to it.value }
        val rooms = db.roomDao().rooms(propertyId)
        val existing = dao.items(propertyId)
        val checked = dao.results(propertyId).filter { it.status != CheckStatus.NOT_CHECKED || it.notes != null || it.costOwn != null }.map { it.itemId }.toSet()
        val withPhotos = db.photoDao().linksForProperty(propertyId).map { it.itemId }.toSet()
        val obsolete = ChecklistGenerator.obsoletePlaceholders(library, rooms, existing, checked + withPhotos)
        // Items whose condition is now explicitly false and that were never touched are removed as well.
        val notApplicable = existing.filter { item ->
            val def = library[item.templateCode] ?: return@filter false
            !ChecklistGenerator.applies(def, p, features) && item.id !in checked && item.id !in withPhotos
        }.map { it.id }
        val remove = (obsolete + notApplicable).distinct()
        if (remove.isNotEmpty()) dao.deleteItems(remove)
        val remaining = existing.filter { it.id !in remove }
        val new = ChecklistGenerator.newItems(CheckpointLibrary.all, p, features, rooms, remaining)
        if (new.isNotEmpty()) dao.insertItems(new)
    }

    fun observeItems(propertyId: Long): Flow<List<ItemWithResult>> = dao.observeItems(propertyId)
    fun observeItem(itemId: Long) = dao.observeItem(itemId)
    suspend fun itemsWithResults(propertyId: Long) = dao.itemsWithResults(propertyId)

    private suspend fun resultFor(itemId: Long) = dao.result(itemId) ?: InspectionResult(itemId = itemId)

    fun titleOf(item: InspectionItem): String = item.customTitle ?: library[item.templateCode]?.let { context.getString(it.title) } ?: item.templateCode
    fun titleOf(item: ItemWithResult): String = item.customTitle ?: library[item.templateCode]?.let { context.getString(it.title) } ?: item.templateCode

    /**
     * Sets the status. A defect status creates (or updates) exactly one linked defect in the register,
     * so the same problem is never registered twice. Going back to OK / N.A. removes an untouched auto-created defect.
     */
    suspend fun setStatus(itemId: Long, status: CheckStatus) = db.withTransaction {
        val item = dao.item(itemId) ?: return@withTransaction
        val r = resultFor(itemId)
        dao.upsertResult(r.copy(status = status, updatedAt = System.currentTimeMillis()))
        syncDefect(item, r.copy(status = status))
        db.inspectionDao().lastSession(item.propertyId)?.takeIf { it.endedAt == null }?.let { dao.updateSession(it.copy(lastItemId = itemId)) }
    }

    suspend fun setSpecialist(itemId: Long, needed: Boolean) = db.withTransaction {
        val item = dao.item(itemId) ?: return@withTransaction
        val r = resultFor(itemId).copy(specialistNeeded = needed, updatedAt = System.currentTimeMillis())
        dao.upsertResult(r); syncDefect(item, r)
    }

    suspend fun setNotes(itemId: Long, notes: String?) {
        val r = resultFor(itemId)
        dao.upsertResult(r.copy(notes = notes?.ifBlank { null }, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setOwnCost(itemId: Long, min: Long?, max: Long?, own: Long?) = db.withTransaction {
        val item = dao.item(itemId) ?: return@withTransaction
        val r = resultFor(itemId).copy(costMin = min, costMax = max, costOwn = own, updatedAt = System.currentTimeMillis())
        dao.upsertResult(r); syncDefect(item, r)
    }

    private fun severityOf(s: CheckStatus) = when (s) { CheckStatus.MINOR -> Severity.MINOR; CheckStatus.MAJOR -> Severity.MAJOR; else -> Severity.SERIOUS }

    private suspend fun syncDefect(item: InspectionItem, r: InspectionResult) {
        val defectDao = db.defectDao()
        val existing = defectDao.byItem(item.id)
        val def = library[item.templateCode]
        if (r.status.isDefect) {
            val min = r.costMin ?: def?.costMinEur?.takeIf { it > 0 }
            val max = r.costMax ?: def?.costMaxEur?.takeIf { it > 0 }
            if (existing == null) {
                defectDao.insert(Defect(
                    propertyId = item.propertyId, roomId = item.roomId, itemId = item.id, title = titleOf(item),
                    categoryCode = costCategoryFor(item.categoryCode), severity = severityOf(r.status),
                    priority = if (r.status == CheckStatus.SERIOUS) Priority.IMMEDIATE else Priority.WITHIN_YEAR,
                    costMin = min, costMax = max, costOwn = r.costOwn, specialistNeeded = r.specialistNeeded,
                    isMoisture = def?.isMoisture ?: false, isStructural = def?.isStructural ?: false,
                    isSafety = def?.isSafety ?: false, isLegal = def?.isLegal ?: false,
                    cause = def?.causes?.takeIf { it != 0 }?.let { context.getString(it) },
                    solution = def?.repair?.takeIf { it != 0 }?.let { context.getString(it) },
                ))
            } else {
                defectDao.update(existing.copy(severity = severityOf(r.status), specialistNeeded = r.specialistNeeded,
                    costMin = r.costMin ?: existing.costMin, costMax = r.costMax ?: existing.costMax, costOwn = r.costOwn ?: existing.costOwn,
                    status = if (existing.status == DefectStatus.RESOLVED) DefectStatus.OPEN else existing.status))
            }
        } else if (existing != null && r.status != CheckStatus.UNSURE && r.status != CheckStatus.NOT_CHECKED) {
            val untouched = existing.notes == null && existing.repairProjectId == null && db.photoDao().forDefect(existing.id).isEmpty() && !existing.quoteReceived
            if (untouched) defectDao.delete(existing.id) else defectDao.update(existing.copy(status = DefectStatus.RESOLVED))
        }
    }

    /** Creates a "pre-marked" checkpoint from a listing photo (section 18). It appears in the wizard automatically. */
    suspend fun createPremarked(propertyId: Long, title: String, description: String?, photoId: Long?, roomId: Long?): Long = db.withTransaction {
        val id = dao.insertItem(InspectionItem(propertyId = propertyId, templateCode = "custom_" + java.util.UUID.randomUUID(),
            categoryCode = "PREMARKED", roomId = roomId, customTitle = title, customDescription = description, sourcePhotoId = photoId,
            quick = true, sortOrder = -1000))
        if (photoId != null) db.photoDao().link(nl.ecannemeijer.casacheck.data.db.PhotoInspectionLink(photoId, id))
        id
    }

    suspend fun deleteCustomItem(itemId: Long) {
        val item = dao.item(itemId) ?: return
        if (item.templateCode.startsWith("custom_")) dao.deleteItems(listOf(itemId))
    }

    // Sessions
    suspend fun startSession(propertyId: Long, mode: InspectionMode, roomId: Long?, category: String?): Long {
        dao.lastSession(propertyId)?.takeIf { it.endedAt == null }?.let { dao.updateSession(it.copy(endedAt = System.currentTimeMillis())) }
        return dao.insertSession(InspectionSession(propertyId = propertyId, mode = mode, roomId = roomId, categoryCode = category))
    }
    suspend fun endSession(propertyId: Long) {
        dao.lastSession(propertyId)?.takeIf { it.endedAt == null }?.let { dao.updateSession(it.copy(endedAt = System.currentTimeMillis())) }
    }
    suspend fun lastSession(propertyId: Long) = dao.lastSession(propertyId)
    fun observeLastSession(propertyId: Long) = dao.observeLastSession(propertyId)
    fun observeOpenSessions() = dao.observeOpenSessions()

    /** Items for a wizard run, in a sensible order. */
    fun filterForMode(items: List<ItemWithResult>, mode: InspectionMode, roomId: Long?, category: String?): List<ItemWithResult> = when (mode) {
        InspectionMode.QUICK -> items.filter { it.quick || it.categoryCode == "PREMARKED" }
        InspectionMode.FULL -> items
        InspectionMode.BY_ROOM -> items.filter { it.roomId == roomId }
        InspectionMode.BY_CATEGORY -> items.filter { it.categoryCode == category }
        InspectionMode.SUSPECT_ONLY -> items.filter {
            it.categoryCode == "PREMARKED" || it.effectiveStatus == CheckStatus.UNSURE || it.effectiveStatus.isDefect || it.specialistNeeded == true
        }
    }.sortedWith(compareBy<ItemWithResult> { if (it.categoryCode == "PREMARKED") 0 else 1 }.thenBy { it.sortOrder }.thenBy { it.id })
}
