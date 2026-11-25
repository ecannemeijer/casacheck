package nl.ecannemeijer.casacheck.ui.detail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.DocumentStatus
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.data.db.ListingState
import nl.ecannemeijer.casacheck.data.db.PreferenceType
import nl.ecannemeijer.casacheck.domain.checklist.FeatureKeys
import nl.ecannemeijer.casacheck.domain.content.ContentLibrary
import nl.ecannemeijer.casacheck.domain.score.CriticalKind
import nl.ecannemeijer.casacheck.domain.score.PrefOutcome
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.LabeledValue
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.common.TriStateRow
import nl.ecannemeijer.casacheck.ui.common.WarningCard
import nl.ecannemeijer.casacheck.ui.nav.Nav
import nl.ecannemeijer.casacheck.ui.theme.StatusColors
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewTab(vm: DetailVm, nav: Nav, goTab: (Int) -> Unit) {
    val p = vm.property.collectAsStateWithLifecycle().value ?: return
    val analysis by vm.analysis.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val listing by vm.listing.collectAsStateWithLifecycle()
    val prices by vm.prices.collectAsStateWithLifecycle()
    val session by vm.lastSession.collectAsStateWithLifecycle()
    val appts by vm.appointments.collectAsStateWithLifecycle()
    val defects by vm.defects.collectAsStateWithLifecycle()
    val docs by vm.documents.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var showStart by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ---- Warnings on top ----
        if (!p.featuresConfirmed) {
            WarningCard(stringResource(R.string.features_unconfirmed), MaterialTheme.colorScheme.tertiary)
            FilledTonalButton(onClick = { showFeatures = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.confirm_features)) }
        }
        val crit = analysis?.score?.critical.orEmpty()
        if (crit.isNotEmpty()) WarningCard(stringResource(R.string.warn_critical, crit.size) + "\n" + crit.take(4).joinToString("\n") { "• " + it.title + " (" + ctx.getString(critName(it.kind)) + ")" })
        val serious = defects.count { it.severity == nl.ecannemeijer.casacheck.data.db.Severity.SERIOUS && it.status != nl.ecannemeijer.casacheck.data.db.DefectStatus.RESOLVED }
        if (serious > 0) WarningCard(stringResource(R.string.warn_serious_defects, serious))
        val spec = items.count { it.specialistNeeded == true }
        if (spec > 0) WarningCard(stringResource(R.string.warn_specialist, spec), StatusColors.specialist)
        val devDocs = docs.filter { it.status == DocumentStatus.DEVIATION || it.status == DocumentStatus.MISSING }
        if (devDocs.isNotEmpty()) WarningCard(stringResource(R.string.warn_documents, devDocs.joinToString { d -> ContentLibrary.documents.firstOrNull { it.code == d.typeCode }?.italianName ?: d.typeCode }))
        val failedHard = analysis?.prefs.orEmpty().filter { it.type == PreferenceType.HARD && it.outcome == PrefOutcome.NOT_MET }
        if (failedHard.isNotEmpty()) WarningCard(stringResource(R.string.warn_hard_prefs, failedHard.joinToString { r -> ctx.getString(prefName(r.key)) }))
        when (listing?.state) {
            ListingState.REMOVED -> WarningCard(stringResource(R.string.warn_listing_removed), MaterialTheme.colorScheme.tertiary)
            else -> {}
        }
        if (prices.size >= 2 && prices.last().price != prices[prices.size - 2].price) {
            val a = prices[prices.size - 2].price; val b = prices.last().price
            InfoNote(stringResource(R.string.price_changed, Fmt.euro(a), Fmt.euro(b)))
        }

        // ---- Inspection actions ----
        val s = session
        if (s != null && s.endedAt == null) {
            Button(onClick = { nav.wizard(p.id, s.mode, s.roomId, s.categoryCode, s.lastItemId) }, Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Filled.PlayArrow, null); Text(" " + stringResource(R.string.resume_inspection) + " – " + stringResource(Labels.mode(s.mode)))
            }
            OutlinedButton(onClick = { showStart = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_other_inspection)) }
        } else Button(onClick = { showStart = true }, Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Filled.PlayArrow, null); Text(" " + stringResource(R.string.start_inspection)) }

        // ---- Main photo + key data ----
        val main = photos.firstOrNull { it.id == p.mainPhotoId } ?: photos.firstOrNull()
        if (main != null) Thumb(CasaFiles.file(main.thumbPath ?: main.filePath), Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)).clickable { nav.photo(p.id, main.id) })
        SectionCard {
            Text(listOfNotNull(p.address, p.city, p.province).joinToString(", ").ifBlank { "–" }, style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledValue(stringResource(R.string.f_asking_price), Fmt.euro(p.askingPrice))
                LabeledValue(stringResource(R.string.f_living_area), Fmt.sqm(p.livingArea))
                LabeledValue(stringResource(R.string.f_price_sqm), Fmt.euro(p.pricePerSqm ?: p.askingPrice?.let { pr -> p.livingArea?.takeIf { it > 0 }?.let { a -> (pr / a).toLong() } }))
                LabeledValue(stringResource(R.string.f_plot_area), Fmt.sqm(p.plotArea))
                LabeledValue(stringResource(R.string.f_rooms), p.rooms?.toString())
                LabeledValue(stringResource(R.string.f_bedrooms), p.bedrooms?.toString())
                LabeledValue(stringResource(R.string.f_bathrooms), p.bathrooms?.toString())
                LabeledValue(stringResource(R.string.f_build_year), p.buildYear?.toString())
                LabeledValue(stringResource(R.string.f_energy_label), p.energyLabel)
                LabeledValue(stringResource(R.string.f_heating), p.heatingType)
            }
        }

        // ---- Score ----
        analysis?.let { a ->
            SectionCard(stringResource(R.string.score_total)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.score.total?.toString() ?: "–", style = MaterialTheme.typography.displaySmall)
                    Text(" / 100", style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(stringResource(R.string.inspection_completion) + ": ${a.score.completion}%", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(progress = { a.score.completion / 100f }, Modifier.fillMaxWidth())
                    }
                }
                a.score.groups.forEach { g ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Labels.group(g.group)), Modifier.width(140.dp), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { (g.score ?: 0) / 100f }, Modifier.weight(1f))
                        Text(g.score?.toString() ?: "–", Modifier.width(36.dp).padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(stringResource(R.string.score_not_guarantee), style = MaterialTheme.typography.bodySmall)
            }
            // ---- Costs ----
            SectionCard(stringResource(R.string.section_costs)) {
                val cs = a.snapshot.costs
                LabeledValue(stringResource(R.string.f_asking_price), Fmt.euro(cs.askingPrice))
                LabeledValue(stringResource(R.string.cost_renovation), Fmt.range(cs.renovation.min, cs.renovation.max))
                LabeledValue(stringResource(R.string.cost_purchase), Fmt.euro(cs.purchaseCosts))
                if (cs.totalMin != null && cs.totalMax != null) LabeledValue(stringResource(R.string.cost_total), Fmt.range(cs.totalMin, cs.totalMax))
                if (cs.defectsWithoutCost > 0) Text(stringResource(R.string.defects_without_cost, cs.defectsWithoutCost), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                TextButton(onClick = { goTab(TAB_COSTS) }) { Text(stringResource(R.string.show_costs)) }
            }
            // ---- Preferences ----
            if (a.prefs.isNotEmpty()) SectionCard(stringResource(R.string.my_wishes)) {
                a.prefs.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(when (r.outcome) { PrefOutcome.MET -> "✓"; PrefOutcome.NOT_MET -> "✗"; PrefOutcome.UNKNOWN -> "?" }, Modifier.width(24.dp),
                            color = when (r.outcome) { PrefOutcome.MET -> StatusColors.ok; PrefOutcome.NOT_MET -> StatusColors.serious; PrefOutcome.UNKNOWN -> StatusColors.neutral })
                        Text(stringResource(prefName(r.key)), Modifier.weight(1f))
                        Text(stringResource(Labels.prefType(r.type)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // ---- Contact & links ----
        SectionCard(stringResource(R.string.contact_and_links)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.agentPhone?.let { ph -> OutlinedButton(onClick = { Intents.dial(ctx, ph) }) { Icon(Icons.Filled.Call, null); Text(" " + stringResource(R.string.call_agent)) } }
                p.agentEmail?.let { em -> OutlinedButton(onClick = { Intents.email(ctx, em, p.referenceNumber?.let { "Rif. $it" } ?: p.title) }) { Icon(Icons.Filled.Email, null); Text(" " + stringResource(R.string.email_agent)) } }
                p.listingUrl?.let { u -> OutlinedButton(onClick = { Intents.url(ctx, u) }) { Icon(Icons.Filled.OpenInBrowser, null); Text(" " + stringResource(R.string.open_listing)) } }
                if (p.latitude != null || !p.address.isNullOrBlank() || !p.city.isNullOrBlank())
                    OutlinedButton(onClick = { Intents.mapsLocation(ctx, p.latitude, p.longitude, listOfNotNull(p.address, p.city, p.province, "Italia").joinToString(", ")) }) { Icon(Icons.Filled.Map, null); Text(" " + stringResource(R.string.show_on_map)) }
                OutlinedButton(onClick = { nav.agent(p.id) }) { Icon(Icons.Filled.QuestionAnswer, null); Text(" " + stringResource(R.string.agent_questions)) }
                OutlinedButton(onClick = { nav.checkpointQuestions(p.id) }) { Icon(Icons.Filled.QuestionAnswer, null); Text(" " + stringResource(R.string.cp_questions_overview)) }
            }
            p.agentName?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }

        // ---- Viewings ----
        SectionCard(stringResource(R.string.viewings)) {
            val df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
            if (appts.isEmpty()) Text(stringResource(R.string.no_viewings), style = MaterialTheme.typography.bodyMedium)
            appts.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Event, null); Text(" " + df.format(Date(a.startsAt)), Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteAppointment(a.id) }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                }
            }
            OutlinedButton(onClick = {
                val cal = Calendar.getInstance()
                DatePickerDialog(ctx, { _, y, m, d ->
                    TimePickerDialog(ctx, { _, h, min -> cal.set(y, m, d, h, min, 0); vm.addAppointment(cal.timeInMillis) }, 10, 0, true).show()
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }) { Text(stringResource(R.string.plan_viewing)) }
        }

        // ---- Personal ----
        SectionCard(stringResource(R.string.personal_assessment)) {
            Row {
                (1..5).forEach { i ->
                    IconButton(onClick = { vm.rating(if (p.personalRating == i) null else i) }) {
                        Icon(if ((p.personalRating ?: 0) >= i) Icons.Filled.Star else Icons.Filled.StarBorder, stringResource(R.string.rating_n, i), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                    }
                }
            }
            AutoSaveField(stringResource(R.string.f_personal_notes), p.notes, { v -> vm.update("notes") { it.copy(notes = v.ifBlank { null }) } }, singleLine = false, minLines = 3)
        }
    }
    if (showStart) StartInspectionDialog(vm, nav) { showStart = false }
    if (showFeatures) FeatureDialog(vm) { showFeatures = false }
}

fun critName(k: CriticalKind) = when (k) { CriticalKind.STRUCTURAL -> R.string.critical_structural; CriticalKind.SAFETY -> R.string.critical_safety; CriticalKind.LEGAL -> R.string.critical_legal; CriticalKind.MOISTURE -> R.string.critical_moisture }
fun prefName(key: String): Int = ContentLibrary.preferences.firstOrNull { it.code == key }?.name ?: R.string.unknown

@Composable
fun StartInspectionDialog(vm: DetailVm, nav: Nav, onDismiss: () -> Unit) {
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf<InspectionMode?>(null) }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(if (step == null) R.string.choose_mode else Labels.mode(step!!))) },
        text = {
            LazyColumn {
                when (step) {
                    null -> InspectionMode.entries.forEach { m ->
                        item {
                            ListItem(headlineContent = { Text(stringResource(Labels.mode(m))) }, supportingContent = { Text(stringResource(Labels.modeHelp(m))) },
                                modifier = Modifier.clickable {
                                    if (m == InspectionMode.BY_ROOM || m == InspectionMode.BY_CATEGORY) step = m
                                    else vm.startSession(m, null, null) { onDismiss(); nav.wizard(vm.pid, m) }
                                })
                            HorizontalDivider()
                        }
                    }
                    InspectionMode.BY_ROOM -> {
                        if (rooms.isEmpty()) item { Text(stringResource(R.string.no_rooms_yet)) }
                        rooms.forEach { r ->
                            item {
                                val n = items.count { it.roomId == r.id }
                                ListItem(headlineContent = { Text(r.name) }, supportingContent = { Text(stringResource(R.string.n_points, n)) },
                                    modifier = Modifier.clickable { vm.startSession(InspectionMode.BY_ROOM, r.id, null) { onDismiss(); nav.wizard(vm.pid, InspectionMode.BY_ROOM, r.id) } })
                            }
                        }
                    }
                    else -> {
                        val byCat = items.groupBy { it.categoryCode }
                        nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary.categories.filter { byCat.containsKey(it.code) }.forEach { cat ->
                            item {
                                val list = byCat[cat.code].orEmpty()
                                val done = list.count { it.effectiveStatus != CheckStatus.NOT_CHECKED }
                                ListItem(headlineContent = { Text(stringResource(cat.name)) }, supportingContent = { Text("$done / ${list.size}") },
                                    modifier = Modifier.clickable { vm.startSession(InspectionMode.BY_CATEGORY, null, cat.code) { onDismiss(); nav.wizard(vm.pid, InspectionMode.BY_CATEGORY, cat = cat.code) } })
                            }
                        }
                    }
                }
            }
        })
}

@Composable
fun FeatureDialog(vm: DetailVm, onDismiss: () -> Unit) {
    val p = vm.property.collectAsStateWithLifecycle().value ?: return
    val f by vm.features.collectAsStateWithLifecycle()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.confirm_features)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.confirm_features_help), style = MaterialTheme.typography.bodySmall)
                FeatureKeys.BOOL_KEYS.forEach { k -> TriStateRow(stringResource(Labels.feature(k)), f[k]?.toBooleanStrictOrNull()) { vm.setFeature(k, it) } }
                TriStateRow(stringResource(R.string.f_cellar), p.cellar) { v -> vm.update("cellar") { it.copy(cellar = v) } }
                TriStateRow(stringResource(R.string.f_garage), p.garage) { v -> vm.update("garage") { it.copy(garage = v) } }
                TriStateRow(stringResource(R.string.f_attic), p.attic) { v -> vm.update("attic") { it.copy(attic = v) } }
                TriStateRow(stringResource(R.string.f_garden), p.garden) { v -> vm.update("garden") { it.copy(garden = v) } }
                TriStateRow(stringResource(R.string.f_terrace), p.terrace) { v -> vm.update("terrace") { it.copy(terrace = v) } }
                TriStateRow(stringResource(R.string.f_balcony), p.balcony) { v -> vm.update("balcony") { it.copy(balcony = v) } }
            }
        },
        confirmButton = { TextButton(onClick = { vm.confirmFeatures(); onDismiss() }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.later)) } })
}
