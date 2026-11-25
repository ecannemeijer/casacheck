package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.ListingState
import nl.ecannemeijer.casacheck.data.importer.ImportOutcome
import nl.ecannemeijer.casacheck.data.importer.ListingData
import nl.ecannemeijer.casacheck.data.importer.PhotoDownloadWorker
import nl.ecannemeijer.casacheck.data.importer.PhotoJob
import nl.ecannemeijer.casacheck.data.repo.PropertyRepository
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.LabeledValue
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.WarningCard
import nl.ecannemeijer.casacheck.ui.nav.Nav
import java.text.DateFormat
import java.util.Date
import java.util.Locale

sealed interface RefreshUi {
    data object Idle : RefreshUi
    data object Busy : RefreshUi
    data class Done(val r: PropertyRepository.RefreshResult) : RefreshUi
    data class Blocked(val site: String, val code: Int) : RefreshUi
    data class Error(val msg: String) : RefreshUi
}

@Composable
fun ListingTab(vm: DetailVm, nav: Nav) {
    val p = vm.property.collectAsStateWithLifecycle().value ?: return
    val listing by vm.listing.collectAsStateWithLifecycle()
    val prices by vm.prices.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf<RefreshUi>(RefreshUi.Idle) }
    var pasteMode by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf("") }
    val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    suspend fun apply(d: ListingData) {
        val r = vm.c.properties.applyRefresh(p.id, d)
        val newPhotos = d.photos.filter { it.url in r.newPhotoUrls }
        if (newPhotos.isNotEmpty()) PhotoDownloadWorker.enqueue(vm.c.appContext, PhotoJob(p.id, newPhotos, null, d.site, d.url))
        ui = RefreshUi.Done(r)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard(stringResource(R.string.listing_status)) {
            LabeledValue(stringResource(R.string.source), p.sourceSite ?: stringResource(R.string.manual_entry))
            LabeledValue(stringResource(R.string.f_listing_id), p.listingId)
            LabeledValue(stringResource(R.string.f_reference), p.referenceNumber)
            listing?.let { l ->
                LabeledValue(stringResource(R.string.listing_state), stringResource(when (l.state) {
                    ListingState.ACTIVE -> R.string.ls_active; ListingState.REMOVED -> R.string.ls_removed; ListingState.BLOCKED -> R.string.ls_blocked; ListingState.UNKNOWN -> R.string.unknown }))
                LabeledValue(stringResource(R.string.last_checked), l.lastCheckedAt?.let { df.format(Date(it)) })
                l.descriptionChangedAt?.let { LabeledValue(stringResource(R.string.description_changed), df.format(Date(it))) }
                if (l.photosAdded > 0 || l.photosRemoved > 0) Text(stringResource(R.string.photos_changed, l.photosAdded, l.photosRemoved), style = MaterialTheme.typography.bodySmall)
                l.lastMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            p.listingUrl?.let { u ->
                OutlinedButton(onClick = { Intents.url(ctx, u) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_listing)) }
                Button(enabled = ui != RefreshUi.Busy, onClick = {
                    ui = RefreshUi.Busy
                    scope.launch {
                        when (val out = vm.c.importer.importUrl(u, Locale.getDefault().toLanguageTag() + ",it;q=0.8") {}) {
                            is ImportOutcome.Success -> apply(out.data)
                            is ImportOutcome.Blocked -> { vm.c.properties.markListingState(p.id, ListingState.BLOCKED, "HTTP ${out.httpCode}"); ui = RefreshUi.Blocked(out.site, out.httpCode) }
                            is ImportOutcome.NotFound -> { vm.c.properties.markListingState(p.id, ListingState.REMOVED, "HTTP ${out.httpCode}"); ui = RefreshUi.Error(ctx.getString(R.string.listing_removed_msg)) }
                            is ImportOutcome.Failed -> ui = RefreshUi.Error(out.message)
                            ImportOutcome.Cancelled -> ui = RefreshUi.Idle
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.refresh_listing)) }
            }
            TextButton(onClick = { pasteMode = !pasteMode }) { Text(stringResource(R.string.refresh_from_text)) }
            if (pasteMode) {
                OutlinedTextField(pasted, { pasted = it }, Modifier.fillMaxWidth(), minLines = 5, label = { Text(stringResource(R.string.listing_text)) })
                Button(enabled = pasted.isNotBlank(), onClick = { scope.launch { apply(vm.c.textImport.import(pasted)); pasteMode = false; pasted = "" } }) { Text(stringResource(R.string.analyse_text)) }
            }
            InfoNote(stringResource(R.string.refresh_note))
        }
        when (val s = ui) {
            RefreshUi.Busy -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(); Text("  " + stringResource(R.string.stage_fetching)) }
            is RefreshUi.Blocked -> { WarningCard(stringResource(R.string.import_blocked, s.site, s.code)); InfoNote(stringResource(R.string.refresh_blocked_hint)) }
            is RefreshUi.Error -> WarningCard(stringResource(R.string.import_failed, s.msg))
            else -> {}
        }
        SectionCard(stringResource(R.string.price_history)) {
            if (prices.isEmpty()) Text("–")
            prices.forEachIndexed { i, ph ->
                val prev = prices.getOrNull(i - 1)?.price
                val diff = prev?.let { ph.price - it }
                Row {
                    Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ph.recordedAt)), Modifier.weight(1f))
                    Text(Fmt.euro(ph.price))
                    diff?.takeIf { it != 0L }?.let { Text("  " + (if (it < 0) "▼ " else "▲ ") + Fmt.euro(kotlin.math.abs(it)), color = if (it < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                }
            }
        }
        SectionCard(stringResource(R.string.f_description)) { Text(p.description ?: "–", style = MaterialTheme.typography.bodyMedium) }
    }

    (ui as? RefreshUi.Done)?.let { d ->
        val r = d.r
        AlertDialog(onDismissRequest = { ui = RefreshUi.Idle }, title = { Text(stringResource(R.string.refresh_result)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (r.priceChanged) Text(stringResource(R.string.price_changed, Fmt.euro(r.oldPrice), Fmt.euro(r.newPrice)))
                if (r.descriptionChanged) Text(stringResource(R.string.description_changed))
                Text(stringResource(R.string.photos_changed, r.newPhotoUrls.size, r.removedPhotoUrls.size))
                if (r.appliedFields.isNotEmpty()) Text(stringResource(R.string.fields_updated, r.appliedFields.joinToString()))
                if (r.pending.isEmpty() && !r.priceChanged && r.appliedFields.isEmpty() && r.newPhotoUrls.isEmpty()) Text(stringResource(R.string.no_changes))
                if (r.pending.isNotEmpty()) {
                    Text(stringResource(R.string.pending_changes), style = MaterialTheme.typography.titleSmall)
                    r.pending.forEach { pc ->
                        var accepted by remember { mutableStateOf(false) }
                        Text("${pc.field}: ${pc.oldValue ?: "–"} → ${pc.newValue ?: "–"}")
                        if (!accepted) TextButton(onClick = { accepted = true; scope.launch { vm.c.properties.acceptPending(p.id, pc) } }) { Text(stringResource(R.string.accept_change)) }
                        else Text("✓", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(stringResource(R.string.user_data_untouched), style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onClick = { ui = RefreshUi.Idle }) { Text(stringResource(R.string.close)) } })
    }
}
