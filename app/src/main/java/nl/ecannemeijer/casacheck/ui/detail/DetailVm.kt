package nl.ecannemeijer.casacheck.ui.detail

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.data.db.Appointment
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.data.db.Property
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.score.PrefResult
import nl.ecannemeijer.casacheck.domain.score.PreferenceEvaluator
import nl.ecannemeijer.casacheck.domain.score.PropertySnapshot
import nl.ecannemeijer.casacheck.domain.score.ScoreCalculator
import nl.ecannemeijer.casacheck.domain.score.ScoreResult
import nl.ecannemeijer.casacheck.ui.common.BaseVm

data class Analysis(val snapshot: PropertySnapshot, val score: ScoreResult, val prefs: List<PrefResult>)

/** Shared ViewModel for all tabs of one property. Flows are cold until a tab collects them. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DetailVm(val c: AppContainer, val pid: Long) : BaseVm() {
    val property = c.properties.observe(pid).state(null)
    val features = c.properties.observeFeatures(pid).map { l -> l.associate { it.key to it.value } }.state(emptyMap())
    val listing = c.properties.observeListing(pid).state(null)
    val prices = c.properties.observePrices(pid).state(emptyList())
    val items = c.inspections.observeItems(pid).state(emptyList())
    val floors = c.rooms.observeFloors(pid).state(emptyList())
    val rooms = c.rooms.observeRooms(pid).state(emptyList())
    val defects = c.defects.observe(pid).state(emptyList())
    val projects = c.defects.observeProjects(pid).state(emptyList())
    val estimates = c.defects.observeCosts(pid).state(emptyList())
    val photos = c.photos.observe(pid).state(emptyList())
    val listingPhotos = c.photos.observeListingPhotos(pid).state(emptyList())
    val moisture = c.measurements.observeMoisture(pid).state(emptyList())
    val points = c.measurements.observePoints(pid).state(emptyList())
    val cracks = c.measurements.observeCracks(pid).state(emptyList())
    val documents = c.documents.observe(pid).state(emptyList())
    val docFiles = c.documents.observeFiles(pid).state(emptyList())
    val env = c.environment.observe(pid).state(emptyList())
    val destinations = c.environment.observeDestinations().state(emptyList())
    val distances = c.environment.observeDistances(pid).state(emptyList())
    val appointments = c.appointments.observeFor(pid).state(emptyList())
    val lastSession = c.inspections.observeLastSession(pid).state(null)
    val settings = c.settings.settings.state(null)

    /** Recomputed (debounced) whenever relevant data changes. */
    val analysis = combine(
        combine(c.properties.observe(pid), c.inspections.observeItems(pid), c.defects.observe(pid)) { a, b, d -> Triple(a, b, d) },
        combine(c.defects.observeProjects(pid), c.defects.observeCosts(pid), c.documents.observe(pid)) { a, b, d -> Triple(a, b, d) },
        combine(c.environment.observe(pid), c.environment.observeDistances(pid), c.preferences.observe(), c.settings.settings) { a, b, p, s -> listOf(a, b, p, s) },
    ) { a, b, d -> Triple(a, b, d) }.debounce(250).mapLatest { (_, _, x) ->
        val snap = c.snapshots.snapshot(pid) ?: return@mapLatest null
        @Suppress("UNCHECKED_CAST")
        val prefs = x[2] as List<nl.ecannemeijer.casacheck.data.db.PropertyPreference>
        val s = c.settings.current()
        Analysis(snap, ScoreCalculator.calculate(snap, s.weightMap(), CheckpointLibrary.byCode), PreferenceEvaluator.evaluate(snap, prefs))
    }.state(null)

    val refreshing = MutableStateFlow(false)

    init { launch { c.properties.touch(pid) } }

    fun update(field: String, transform: (Property) -> Property) = launch {
        val p = c.properties.get(pid) ?: return@launch
        val n = transform(p)
        if (n != p) {
            c.properties.update(n, setOf(field))
            val t = n.viewingAt
            if (field == "viewingAt" && t != null && appointments.value.none { it.startsAt == t }) c.appointments.save(Appointment(propertyId = pid, startsAt = t))
        }
    }
    fun setFeature(key: String, v: Boolean?) = launch { c.properties.setFeature(pid, key, v?.toString()) }
    fun confirmFeatures() = launch { c.properties.confirmFeatures(pid) }
    fun favorite(f: Boolean) = launch { c.properties.setFavorite(pid, f) }
    fun rating(r: Int?) = update("personalRating") { it.copy(personalRating = r) }
    fun addAppointment(t: Long) = launch {
        c.appointments.save(Appointment(propertyId = pid, startsAt = t))
        c.properties.get(pid)?.let { p -> if (p.viewingAt == null || p.viewingAt < System.currentTimeMillis() || t < p.viewingAt) c.properties.update(p.copy(viewingAt = t)) }
    }
    fun deleteAppointment(id: Long) = launch { c.appointments.delete(id) }
    fun startSession(mode: InspectionMode, room: Long?, cat: String?, then: () -> Unit) = launch { c.inspections.startSession(pid, mode, room, cat); then() }
}
