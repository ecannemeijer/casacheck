package nl.ecannemeijer.casacheck.data.repo

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import nl.ecannemeijer.casacheck.data.files.FileCleanup
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.db.Floor
import nl.ecannemeijer.casacheck.data.db.ImportHistory
import nl.ecannemeijer.casacheck.data.db.ImportStatus
import nl.ecannemeijer.casacheck.data.db.ListingState
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.data.db.Property
import nl.ecannemeijer.casacheck.data.db.PropertyFeature
import nl.ecannemeijer.casacheck.data.db.PropertyListing
import nl.ecannemeijer.casacheck.data.db.PropertyPriceHistory
import nl.ecannemeijer.casacheck.data.db.PropertyStats
import nl.ecannemeijer.casacheck.data.db.newUuid
import nl.ecannemeijer.casacheck.data.files.FileStore
import nl.ecannemeijer.casacheck.data.importer.ListingData
import nl.ecannemeijer.casacheck.data.settings.SettingsStore
import nl.ecannemeijer.casacheck.domain.cost.CostCalculator
import nl.ecannemeijer.casacheck.domain.cost.CostSummary

/** One card of the property list. */
data class PropertyCard(
    val property: Property,
    val thumbPath: String?,
    val stats: PropertyStats?,
    val costs: CostSummary,
) {
    val progress: Int get() = stats?.let { if (it.totalItems == 0) 0 else it.checkedItems * 100 / it.totalItems } ?: 0
}

class PropertyRepository(
    private val db: CasaDatabase,
    private val files: FileStore,
    private val settings: SettingsStore,
    private val inspections: InspectionRepository,
) {
    private val dao = db.propertyDao()

    fun observe(id: Long) = dao.observe(id)
    fun observeFeatures(id: Long) = db.featureDao().observe(id)
    fun observeListing(id: Long) = db.listingDao().observe(id)
    fun observePrices(id: Long) = db.listingDao().observePrices(id)
    fun observeLastOpened(): Flow<Property?> = combine(dao.observeLastOpened(), pending) { p, hidden -> p?.takeIf { it.id !in hidden } }

    /** Properties the user just deleted; hidden everywhere while the "undo" snackbar is shown, then deleted for real. */
    private val pending = MutableStateFlow<Set<Long>>(emptySet())
    val pendingDelete: StateFlow<Set<Long>> = pending.asStateFlow()
    fun hide(id: Long) = pending.update { it + id }
    fun unhide(id: Long) = pending.update { it - id }
    suspend fun get(id: Long) = dao.get(id)
    suspend fun all() = dao.all()

    /** All property cards with progress, defect counts and cost totals (one combined flow, no N+1 queries). */
    fun observeCards(): Flow<List<PropertyCard>> = combine(observeAllCards(), pending) { cards, hidden -> if (hidden.isEmpty()) cards else cards.filter { it.property.id !in hidden } }

    private fun observeAllCards(): Flow<List<PropertyCard>> = combine(
        dao.observeEverything(), dao.observeStats(), db.defectDao().observeAll(), db.defectDao().observeAllProjects(),
        combine(db.defectDao().observeAllCosts(), settings.settings, photosFlow()) { c, s, p -> Triple(c, s, p) },
    ) { props, stats, defects, projects, (costs, s, photos) ->
        val statMap = stats.associateBy { it.propertyId }
        val photoMap = photos.associateBy { it.id }
        val firstPhoto = photos.groupBy { it.propertyId }
        props.map { p ->
            val main = p.mainPhotoId?.let { photoMap[it] } ?: firstPhoto[p.id]?.firstOrNull()
            PropertyCard(
                p, main?.thumbPath ?: main?.filePath, statMap[p.id],
                CostCalculator.summarize(p.askingPrice, p.ownOffer, p.purchaseCostPercent ?: s.purchaseCostPercent,
                    p.contingencyPercent ?: s.contingencyPercent, defects.filter { it.propertyId == p.id },
                    projects.filter { it.propertyId == p.id }, costs.filter { it.propertyId == p.id }),
            )
        }
    }

    private fun photosFlow(): Flow<List<Photo>> = db.photoDao().observeMainCandidates()

    suspend fun findExisting(url: String?, site: String?, listingId: String?): Property? = dao.findByListing(url, site, listingId)

    /** Creates a property, its features, initial price history and the generated checklist. */
    suspend fun create(p: Property, features: Map<String, String?> = emptyMap(), source: String = "user"): Long = db.withTransaction {
        val id = dao.insert(p.copy(id = 0, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        features.forEach { (k, v) -> if (v != null) db.featureDao().upsert(PropertyFeature(propertyId = id, key = k, value = v, source = source)) }
        p.askingPrice?.let { db.listingDao().insertPrice(PropertyPriceHistory(propertyId = id, price = it, source = source)) }
        if (p.listingUrl != null && p.sourceSite != null) {
            db.listingDao().upsert(PropertyListing(propertyId = id, site = p.sourceSite, url = p.listingUrl, externalId = p.listingId,
                state = ListingState.ACTIVE, lastCheckedAt = System.currentTimeMillis(), lastDescription = p.description))
        }
        inspections.regenerate(id)
        id
    }

    /** Saves user edits; [changedFields] are remembered as manually corrected so a refresh never overwrites them silently. */
    suspend fun update(p: Property, changedFields: Set<String> = emptySet()) {
        val old = dao.get(p.id)
        val manual = (p.manualFields.split(',').filter { it.isNotBlank() } + changedFields).toSortedSet().joinToString(",")
        dao.update(p.copy(manualFields = manual, updatedAt = System.currentTimeMillis()))
        if (old != null && old.askingPrice != p.askingPrice && p.askingPrice != null) {
            db.listingDao().insertPrice(PropertyPriceHistory(propertyId = p.id, price = p.askingPrice, source = "user"))
        }
        if (old != null && (old.cellar != p.cellar || old.garage != p.garage || old.attic != p.attic || old.garden != p.garden ||
                old.terrace != p.terrace || old.balcony != p.balcony || old.propertyType != p.propertyType)) inspections.regenerate(p.id)
    }

    suspend fun setFeature(propertyId: Long, key: String, value: String?) {
        if (value == null) db.featureDao().delete(propertyId, key)
        else db.featureDao().upsert(PropertyFeature(propertyId = propertyId, key = key, value = value, source = "user"))
        inspections.regenerate(propertyId)
    }

    suspend fun confirmFeatures(propertyId: Long) {
        dao.get(propertyId)?.let { dao.update(it.copy(featuresConfirmed = true, updatedAt = System.currentTimeMillis())) }
        inspections.regenerate(propertyId)
    }

    suspend fun setFavorite(id: Long, fav: Boolean) = dao.setFavorite(id, fav)
    suspend fun setArchived(id: Long, a: Boolean) = dao.setArchived(id, a)
    suspend fun setMainPhoto(id: Long, photoId: Long?) = dao.setMainPhoto(id, photoId)
    suspend fun touch(id: Long) = dao.touchOpened(id)

    /**
     * Deletes one property for good. Foreign keys (ON DELETE CASCADE) remove all its rows: features, listing, prices, floors,
     * rooms, checklist items and results, sessions, defects, repair projects, costs, moisture/crack measurements, documents,
     * photos (+ links/annotations), agent answers, appointments, environment ratings, distances and report rows.
     * Afterwards its files are removed from disk: the per-property photo/thumb/doc/edited folders and the generated PDFs.
     * Returns the number of files/folders removed.
     */
    suspend fun delete(id: Long): Int = withContext(Dispatchers.IO) {
        val p = dao.get(id) ?: run { unhide(id); return@withContext 0 }
        val plan = db.withTransaction {
            val photos = db.photoDao().list(id)
            val docs = db.documentDao().files(id)
            val reports = db.reportDao().forProperty(id)
            dao.delete(id)
            FileCleanup.planForProperty(p.uuid,
                photos.flatMap { listOf(it.filePath, it.thumbPath, it.editedPath) } + docs.map { it.filePath } + reports.map { it.filePath })
        }
        val removed = FileCleanup.execute(files.root, plan)
        unhide(id)
        removed
    }

    /** Copies the base data, features, floors and rooms; the checklist is generated fresh (results are not copied). */
    suspend fun duplicate(id: Long, titleSuffix: String): Long = db.withTransaction {
        val p = dao.get(id) ?: error("not found")
        val newId = dao.insert(p.copy(id = 0, uuid = newUuid(), title = p.title + titleSuffix, mainPhotoId = null, favorite = false,
            archived = false, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), lastOpenedAt = null,
            listingUrl = null, listingId = null))
        db.featureDao().list(id).forEach { db.featureDao().upsert(it.copy(id = 0, propertyId = newId)) }
        val floorMap = db.roomDao().floors(id).associate { f -> f.id to db.roomDao().insertFloor(f.copy(id = 0, uuid = newUuid(), propertyId = newId)) }
        db.roomDao().rooms(id).forEach { r -> db.roomDao().insertRoom(r.copy(id = 0, uuid = newUuid(), propertyId = newId, floorId = r.floorId?.let { floorMap[it] })) }
        inspections.regenerate(newId)
        newId
    }

    /** Applies imported listing data to a new property (only found values are set). */
    fun fromListing(d: ListingData): Property = Property(
        title = d.title ?: "", address = d.address, city = d.city, province = d.province, region = d.region, country = d.country,
        latitude = d.latitude, longitude = d.longitude, askingPrice = d.price, originalPrice = d.price, pricePerSqm = d.pricePerSqm,
        buildYear = d.buildYear, propertyType = d.propertyType, livingArea = d.livingArea, plotArea = d.plotArea, rooms = d.rooms,
        bedrooms = d.bedrooms, bathrooms = d.bathrooms, floors = d.floors, condition = d.condition, energyLabel = d.energyLabel,
        energyConsumption = d.energyConsumption, heatingType = d.heatingType, garage = d.garage, parkingSpaces = d.parkingSpaces,
        cellar = d.cellar, attic = d.attic, balcony = d.balcony, terrace = d.terrace, garden = d.garden, airConditioning = d.airConditioning,
        description = d.description, listingUrl = d.url, listingId = d.externalId, sourceSite = d.site.takeIf { it != "text" && it != "screenshot" },
        referenceNumber = d.referenceNumber, agentName = d.agentName, agentPhone = d.agentPhone, agentEmail = d.agentEmail,
    )

    suspend fun logImport(h: ImportHistory) = db.listingDao().insertImport(h)
    fun observeImports() = db.listingDao().observeImports()

    // ---------- Listing refresh (section 28) ----------

    /** A proposed change from a refresh that touches a manually corrected field; needs user confirmation. */
    data class PendingChange(val field: String, val oldValue: String?, val newValue: String?)

    data class RefreshResult(
        val priceChanged: Boolean,
        val oldPrice: Long?,
        val newPrice: Long?,
        val descriptionChanged: Boolean,
        val newPhotoUrls: List<String>,
        val removedPhotoUrls: List<String>,
        val pending: List<PendingChange>,
        val appliedFields: List<String>,
    )

    /**
     * Merges refreshed listing data. Never touches inspection photos, results, notes, own costs or ratings.
     * Fields that were corrected by hand are returned as [RefreshResult.pending] instead of being overwritten.
     * Empty fields are filled; other listing fields are updated only when not manually corrected.
     */
    suspend fun applyRefresh(propertyId: Long, d: ListingData): RefreshResult = db.withTransaction {
        val p = dao.get(propertyId) ?: error("not found")
        val manual = p.manualFields.split(',').filter { it.isNotBlank() }.toSet()
        val pending = mutableListOf<PendingChange>()
        val applied = mutableListOf<String>()
        var np = p
        fun <T> field(name: String, old: T?, new: T?, set: (Property, T?) -> Property) {
            if (new == null || new == old) return
            if (old != null && name in manual) { pending += PendingChange(name, old.toString(), new.toString()); return }
            np = set(np, new); applied += name
        }
        field("title", p.title.ifBlank { null }, d.title) { x, v -> x.copy(title = v ?: x.title) }
        field("livingArea", p.livingArea, d.livingArea) { x, v -> x.copy(livingArea = v) }
        field("plotArea", p.plotArea, d.plotArea) { x, v -> x.copy(plotArea = v) }
        field("energyLabel", p.energyLabel, d.energyLabel) { x, v -> x.copy(energyLabel = v) }
        field("pricePerSqm", p.pricePerSqm, d.pricePerSqm) { x, v -> x.copy(pricePerSqm = v) }
        field("agentName", p.agentName, d.agentName) { x, v -> x.copy(agentName = v) }
        field("agentPhone", p.agentPhone, d.agentPhone) { x, v -> x.copy(agentPhone = v) }
        field("description", p.description, d.description) { x, v -> x.copy(description = v) }
        val priceChanged = d.price != null && d.price != p.askingPrice
        if (priceChanged) {
            if ("askingPrice" in manual) pending += PendingChange("askingPrice", p.askingPrice?.toString(), d.price.toString())
            else { np = np.copy(askingPrice = d.price, originalPrice = p.originalPrice ?: p.askingPrice); applied += "askingPrice" }
            db.listingDao().insertPrice(PropertyPriceHistory(propertyId = propertyId, price = d.price!!, source = "listing"))
        }
        dao.update(np.copy(updatedAt = System.currentTimeMillis()))
        val listing = db.listingDao().get(propertyId)
        val oldUrls = listing?.lastPhotoUrls?.split('\n')?.filter { it.isNotBlank() }?.toSet()
            ?: db.photoDao().listingPhotos(propertyId).map { it.sourceUrl }.toSet()
        // A refresh without photo information (e.g. from pasted text) never marks photos as removed.
        val hasPhotos = d.photos.isNotEmpty()
        val newUrls = if (hasPhotos) d.photos.map { it.url } else oldUrls.toList()
        val added = newUrls.filter { it !in oldUrls }
        val removed = oldUrls.filter { it !in newUrls.toSet() }
        // Mark removed photos (the local files are kept).
        if (hasPhotos) db.photoDao().listingPhotos(propertyId).forEach { lp ->
            val isRemoved = lp.sourceUrl in removed
            if (lp.removedFromListing != isRemoved) db.photoDao().updateListingPhoto(lp.copy(removedFromListing = isRemoved))
        }
        val descChanged = d.description != null && listing?.lastDescription != null && d.description != listing.lastDescription
        db.listingDao().upsert((listing ?: PropertyListing(propertyId = propertyId, site = d.site, url = d.url ?: p.listingUrl ?: "", externalId = d.externalId)).copy(
            state = ListingState.ACTIVE, lastCheckedAt = System.currentTimeMillis(), lastDescription = d.description ?: listing?.lastDescription,
            descriptionChangedAt = descriptionChanged_at(descChanged, listing?.descriptionChangedAt), lastPhotoUrls = newUrls.joinToString("\n"),
            photosAdded = added.size, photosRemoved = removed.size, lastMessage = null,
        ))
        RefreshResult(priceChanged, p.askingPrice, d.price, descChanged, added, removed, pending, applied)
    }

    private fun descriptionChanged_at(changed: Boolean, old: Long?): Long? = if (changed) System.currentTimeMillis() else old

    suspend fun markListingState(propertyId: Long, state: ListingState, message: String?) {
        val l = db.listingDao().get(propertyId) ?: return
        db.listingDao().upsert(l.copy(state = state, lastCheckedAt = System.currentTimeMillis(), lastMessage = message))
    }

    suspend fun acceptPending(propertyId: Long, change: PendingChange) {
        val p = dao.get(propertyId) ?: return
        val v = change.newValue
        val np = when (change.field) {
            "title" -> p.copy(title = v ?: p.title)
            "livingArea" -> p.copy(livingArea = v?.toDoubleOrNull())
            "plotArea" -> p.copy(plotArea = v?.toDoubleOrNull())
            "energyLabel" -> p.copy(energyLabel = v)
            "pricePerSqm" -> p.copy(pricePerSqm = v?.toLongOrNull())
            "agentName" -> p.copy(agentName = v)
            "agentPhone" -> p.copy(agentPhone = v)
            "description" -> p.copy(description = v)
            "askingPrice" -> p.copy(askingPrice = v?.toLongOrNull())
            else -> p
        }
        dao.update(np.copy(updatedAt = System.currentTimeMillis()))
    }

    fun importStatusFor(partial: Boolean) = if (partial) ImportStatus.PARTIAL else ImportStatus.SUCCESS

    // floors convenience used by the add flow
    suspend fun addFloor(propertyId: Long, name: String, level: Int) = db.roomDao().insertFloor(Floor(propertyId = propertyId, name = name, level = level))
}
