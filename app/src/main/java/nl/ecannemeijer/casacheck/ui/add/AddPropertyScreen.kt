package nl.ecannemeijer.casacheck.ui.add

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.MainActivity
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.SharedPayload
import nl.ecannemeijer.casacheck.data.db.Appointment
import nl.ecannemeijer.casacheck.data.db.ImportHistory
import nl.ecannemeijer.casacheck.data.db.ImportStatus
import nl.ecannemeijer.casacheck.data.db.Property
import nl.ecannemeijer.casacheck.data.importer.ImportOutcome
import nl.ecannemeijer.casacheck.data.importer.ImportProgress
import nl.ecannemeijer.casacheck.data.importer.ImportStage
import nl.ecannemeijer.casacheck.data.importer.ListingData
import nl.ecannemeijer.casacheck.data.importer.PhotoDownloadWorker
import nl.ecannemeijer.casacheck.data.importer.PhotoJob
import nl.ecannemeijer.casacheck.data.importer.UrlRecognizer
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.BaseVm
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.PropertyForm
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.WarningCard
import nl.ecannemeijer.casacheck.ui.common.TranslateTextBlock
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.Nav
import java.io.File
import java.util.Locale

enum class AddMode { URL, TEXT, SCREENSHOT, MANUAL }

sealed interface AddState {
    data object Input : AddState
    data class Working(val progress: ImportProgress?, val ocrIndex: Int? = null, val ocrTotal: Int? = null) : AddState
    data class Blocked(val site: String, val code: Int, val reason: String) : AddState
    data class Error(val message: String) : AddState
    data class Preview(val data: ListingData?, val source: String) : AddState
}

class AddPropertyVm(private val c: AppContainer) : BaseVm() {
    val mode = MutableStateFlow(AddMode.URL)
    val url = MutableStateFlow("")
    val text = MutableStateFlow("")
    val state = MutableStateFlow<AddState>(AddState.Input)
    val draft = MutableStateFlow(Property())
    val found = MutableStateFlow<Set<String>?>(null)
    val selectedPhotos = MutableStateFlow<Set<Int>>(emptySet())
    val mainPhoto = MutableStateFlow<Int?>(null)
    val duplicate = MutableStateFlow<Property?>(null)
    val ocrText = MutableStateFlow<String?>(null)
    val screenshots = MutableStateFlow<List<Uri>>(emptyList())
    val keepScreenshots = MutableStateFlow(true)
    private var job: Job? = null
    private var sharedHandled = false

    fun handleShared(p: SharedPayload?) {
        if (p == null || sharedHandled) return
        sharedHandled = true
        when {
            p.images.isNotEmpty() -> { mode.value = AddMode.SCREENSHOT; screenshots.value = p.images; runOcr() }
            p.url != null && UrlRecognizer.isSupported(p.url) -> { mode.value = AddMode.URL; url.value = p.url; importUrl() }
            !p.text.isNullOrBlank() -> {
                val u = UrlRecognizer.extractUrl(p.text)
                if (u != null && UrlRecognizer.isSupported(u)) { mode.value = AddMode.URL; url.value = u; importUrl() }
                else { mode.value = AddMode.TEXT; text.value = p.text }
            }
        }
    }

    fun importUrl() {
        job?.cancel()
        state.value = AddState.Working(null)
        job = viewModelScope.launch {
            val out = c.importer.importUrl(url.value.trim(), Locale.getDefault().toLanguageTag() + ",it;q=0.8,en;q=0.6") { pr -> state.value = AddState.Working(pr) }
            when (out) {
                is ImportOutcome.Success -> preview(out.data, if (out.partial) "partial" else "url")
                is ImportOutcome.Blocked -> {
                    c.properties.logImport(ImportHistory(source = out.site, url = url.value, status = ImportStatus.BLOCKED, message = "HTTP ${out.httpCode}: ${out.reason}"))
                    state.value = AddState.Blocked(out.site, out.httpCode, out.reason)
                }
                is ImportOutcome.NotFound -> state.value = AddState.Error("HTTP ${out.httpCode}")
                is ImportOutcome.Failed -> {
                    c.properties.logImport(ImportHistory(source = "url", url = url.value, status = ImportStatus.FAILED, message = out.message))
                    state.value = AddState.Error(out.message)
                }
                ImportOutcome.Cancelled -> state.value = AddState.Input
            }
        }
    }

    fun cancel() {
        job?.cancel(); job = null
        launch { c.properties.logImport(ImportHistory(source = "url", url = url.value.ifBlank { null }, status = ImportStatus.CANCELLED)) }
        state.value = AddState.Input
    }

    fun parseText() { preview(c.textImport.import(text.value), "text") }

    fun runOcr() {
        val uris = screenshots.value
        if (uris.isEmpty()) return
        job?.cancel()
        state.value = AddState.Working(null, 0, uris.size)
        job = viewModelScope.launch {
            try {
                val dir = File(c.appContext.cacheDir, "ocr").apply { deleteRecursively(); mkdirs() }
                val paths = uris.mapIndexed { i, u ->
                    val f = File(dir, "s$i.img")
                    c.appContext.contentResolver.openInputStream(u)?.use { input -> f.outputStream().use { input.copyTo(it) } }
                    f.absolutePath
                }
                val (data, raw) = c.screenshotImport.import(paths) { i -> state.value = AddState.Working(null, i + 1, uris.size) }
                ocrText.value = raw
                preview(data, "screenshot")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                state.value = AddState.Error(e.message ?: "OCR")
            }
        }
    }

    fun reparseOcr(t: String) { ocrText.value = t; preview(c.textImport.import(t).copy(site = "screenshot"), "screenshot") }

    fun manual() { draft.value = Property(); found.value = null; state.value = AddState.Preview(null, "manual"); duplicate.value = null }

    private fun preview(d: ListingData, source: String) {
        draft.value = c.properties.fromListing(d)
        found.value = d.foundFields()
        val photos = d.photos.filter { !it.isFloorplan }.map { it.position }.toSet() + d.photos.filter { it.isFloorplan }.map { it.position }
        selectedPhotos.value = photos
        mainPhoto.value = d.photos.firstOrNull { !it.isFloorplan }?.position
        state.value = AddState.Preview(d, source)
        launch { duplicate.value = c.properties.findExisting(d.url, d.site, d.externalId) }
    }

    fun change(transform: (Property) -> Property) { draft.value = transform(draft.value) }

    fun save(onDone: (Long) -> Unit) = launch {
        val st = state.value as? AddState.Preview ?: return@launch
        val d = st.data
        val p = draft.value
        val features = d?.extraFeatures.orEmpty()
        val id = c.properties.create(p, features, source = st.source)
        p.viewingAt?.let { c.appointments.save(Appointment(propertyId = id, startsAt = it)) }
        if (d != null) {
            val sel = d.photos.filter { it.position in selectedPhotos.value }
            c.properties.logImport(ImportHistory(propertyId = id, source = d.site, url = d.url, status = if (st.source == "partial") ImportStatus.PARTIAL else ImportStatus.SUCCESS,
                fieldsFound = found.value?.size ?: 0, photosFound = d.photos.size))
            if (sel.isNotEmpty()) PhotoDownloadWorker.enqueue(c.appContext, PhotoJob(id, sel, mainPhoto.value, d.site, d.url))
        }
        if (st.source == "screenshot" && keepScreenshots.value) {
            screenshots.value.forEach { u -> runCatching { c.photos.addFromUri(id, u, PhotoKind.REFERENCE) } }
        }
        onDone(id)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPropertyScreen(c: AppContainer, nav: Nav, shared: SharedPayload?, onSharedConsumed: () -> Unit) {
    val vm = vm { AddPropertyVm(c) }
    LaunchedEffect(shared) { if (shared != null) { vm.handleShared(shared); onSharedConsumed() } }
    val mode by vm.mode.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val url by vm.url.collectAsStateWithLifecycle()
    val text by vm.text.collectAsStateWithLifecycle()
    val shots by vm.screenshots.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) { vm.screenshots.value = uris; vm.runOcr() }
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.add_property), nav::back) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state !is AddState.Preview) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AddMode.URL to (R.string.mode_url to Icons.Filled.Link), AddMode.TEXT to (R.string.mode_text to Icons.Filled.TextSnippet),
                        AddMode.SCREENSHOT to (R.string.mode_screenshot to Icons.Filled.Image), AddMode.MANUAL to (R.string.mode_manual to Icons.Filled.Edit)).forEach { (m, li) ->
                        FilterChip(selected = mode == m, onClick = { vm.mode.value = m; if (m == AddMode.MANUAL) vm.manual() }, label = { Text(stringResource(li.first)) },
                            leadingIcon = { Icon(li.second, null) })
                    }
                }
            }
            when (val s = state) {
                is AddState.Working -> SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator()
                        Column(Modifier.weight(1f)) {
                            Text(when {
                                s.ocrTotal != null -> stringResource(R.string.ocr_progress, s.ocrIndex ?: 0, s.ocrTotal)
                                else -> stringResource(when (s.progress?.stage) {
                                    ImportStage.FETCHING -> R.string.stage_fetching; ImportStage.PARSING -> R.string.stage_parsing
                                    ImportStage.PHOTOS -> R.string.stage_photos; ImportStage.DONE -> R.string.stage_done; else -> R.string.stage_validating })
                            }, style = MaterialTheme.typography.titleMedium)
                            s.progress?.takeIf { it.fieldsFound > 0 }?.let { Text(stringResource(R.string.found_counts, it.fieldsFound, it.photosFound)) }
                        }
                    }
                    OutlinedButton(onClick = vm::cancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
                }
                is AddState.Blocked -> {
                    WarningCard(stringResource(R.string.import_blocked, s.site, s.code))
                    InfoNote(stringResource(R.string.import_blocked_explain))
                    FallbackButtons(vm, onScreens = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onOpen = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } })
                }
                is AddState.Error -> {
                    WarningCard(stringResource(R.string.import_failed, s.message))
                    FallbackButtons(vm, onScreens = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, onOpen = null)
                }
                is AddState.Preview -> PreviewSection(c, vm, s, nav)
                AddState.Input -> {}
            }
            if (state is AddState.Input || state is AddState.Error || state is AddState.Blocked) when (mode) {
                AddMode.URL -> SectionCard(stringResource(R.string.mode_url)) {
                    OutlinedTextField(url, { vm.url.value = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.listing_url_hint)) }, singleLine = true)
                    val ok = UrlRecognizer.isSupported(url.trim())
                    if (url.isNotBlank() && !ok) Text(stringResource(R.string.url_not_supported), color = MaterialTheme.colorScheme.error)
                    Button(onClick = vm::importUrl, enabled = ok, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.import_action)) }
                    Text(stringResource(R.string.import_note), style = MaterialTheme.typography.bodySmall)
                }
                AddMode.TEXT -> SectionCard(stringResource(R.string.mode_text)) {
                    Text(stringResource(R.string.paste_text_explain), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(text, { vm.text.value = it }, Modifier.fillMaxWidth(), minLines = 8, label = { Text(stringResource(R.string.listing_text)) })
                    Button(onClick = vm::parseText, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.analyse_text)) }
                    TranslateTextBlock(c, text)
                }
                AddMode.SCREENSHOT -> SectionCard(stringResource(R.string.mode_screenshot)) {
                    Text(stringResource(R.string.screenshot_explain), style = MaterialTheme.typography.bodyMedium)
                    if (shots.isNotEmpty()) Text(stringResource(R.string.n_selected, shots.size))
                    Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.choose_screenshots))
                    }
                }
                AddMode.MANUAL -> {}
            }
        }
    }
}

@Composable
private fun FallbackButtons(vm: AddPropertyVm, onScreens: () -> Unit, onOpen: (() -> Unit)?) {
    SectionCard(stringResource(R.string.alternatives)) {
        OutlinedButton(onClick = { vm.mode.value = AddMode.TEXT; vm.state.value = AddState.Input }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.fallback_paste)) }
        OutlinedButton(onClick = { vm.mode.value = AddMode.SCREENSHOT; onScreens() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.fallback_screens)) }
        OutlinedButton(onClick = { vm.mode.value = AddMode.MANUAL; vm.manual() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.fallback_manual)) }
        if (onOpen != null) TextButton(onClick = onOpen, Modifier.fillMaxWidth()) { Icon(Icons.Filled.OpenInBrowser, null); Text(stringResource(R.string.open_in_browser)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewSection(c: AppContainer, vm: AddPropertyVm, s: AddState.Preview, nav: Nav) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val found by vm.found.collectAsStateWithLifecycle()
    val sel by vm.selectedPhotos.collectAsStateWithLifecycle()
    val main by vm.mainPhoto.collectAsStateWithLifecycle()
    val dup by vm.duplicate.collectAsStateWithLifecycle()
    val ocr by vm.ocrText.collectAsStateWithLifecycle()
    val keep by vm.keepScreenshots.collectAsStateWithLifecycle()
    dup?.let { d ->
        WarningCard(stringResource(R.string.duplicate_found, d.title.ifBlank { "#${d.id}" }))
        OutlinedButton(onClick = { nav.property(d.id) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_existing)) }
    }
    if (s.data != null) {
        val f = found.orEmpty()
        InfoNote(stringResource(R.string.preview_summary, f.size, s.data.photos.size))
        if (s.source == "partial") WarningCard(stringResource(R.string.import_partial), MaterialTheme.colorScheme.tertiary)
    }
    if (s.source == "screenshot" && ocr != null) SectionCard(stringResource(R.string.ocr_text)) {
        var t by androidx.compose.runtime.remember { mutableStateOf(ocr ?: "") }
        OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), minLines = 4, maxLines = 10)
        OutlinedButton(onClick = { vm.reparseOcr(t) }) { Text(stringResource(R.string.analyse_again)) }
        TranslateTextBlock(c, t)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(keep, { vm.keepScreenshots.value = it }); Text(stringResource(R.string.keep_screenshots)) }
    }
    val photos = s.data?.photos.orEmpty()
    if (photos.isNotEmpty()) SectionCard(stringResource(R.string.listing_photos)) {
        Text(stringResource(R.string.photo_select_hint), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.selectedPhotos.value = photos.map { it.position }.toSet() }) { Text(stringResource(R.string.select_all)) }
            TextButton(onClick = { vm.selectedPhotos.value = emptySet() }) { Text(stringResource(R.string.select_none)) }
            Text(stringResource(R.string.n_selected, sel.size), Modifier.align(Alignment.CenterVertically))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            photos.forEach { ph ->
                val on = ph.position in sel
                Card(Modifier.size(104.dp).clickable { vm.selectedPhotos.value = if (on) sel - ph.position else sel + ph.position },
                    border = if (main == ph.position) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null) {
                    Box {
                        AsyncImage(ph.thumbUrl ?: ph.url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        if (on) Icon(Icons.Filled.CheckCircle, stringResource(R.string.selected), Modifier.align(Alignment.TopStart).padding(4.dp), tint = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Filled.Star, stringResource(R.string.set_main_photo),
                            Modifier.align(Alignment.BottomEnd).padding(4.dp).size(28.dp).clickable { vm.mainPhoto.value = ph.position; if (!on) vm.selectedPhotos.value = sel + ph.position },
                            tint = if (main == ph.position) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        if (ph.isFloorplan) Text(stringResource(R.string.floorplan), Modifier.align(Alignment.BottomStart).padding(4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    PropertyForm(draft, if (s.data != null) found else null) { _, t -> vm.change(t) }
    InfoNote(stringResource(R.string.features_confirm_after_save))
    Button(onClick = { vm.save { id -> nav.propertyReplacingAdd(id) } }, Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.save_property)) }
}
