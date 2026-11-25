package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Defect
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.data.db.Priority
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.Dropdown
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.common.WarningCard
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.CameraTarget
import nl.ecannemeijer.casacheck.ui.nav.Nav

@Composable
fun NumField(label: String, value: Long?, modifier: Modifier = Modifier, onChange: (Long?) -> Unit) {
    var t by remember(value == null) { mutableStateOf(value?.toString() ?: "") }
    OutlinedTextField(t, { t = it; onChange(Fmt.parseLong(it)) }, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefectEditScreen(c: AppContainer, nav: Nav, pid: Long, id: Long?) {
    val vm = vm("detail$pid") { DetailVm(c, pid) }
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var d by remember { mutableStateOf<Defect?>(if (id == null) Defect(propertyId = pid, title = "") else null) }
    var savedId by remember { mutableStateOf(id) }
    var similar by remember { mutableStateOf<Defect?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(id) { if (id != null) d = c.defects.get(id) }
    // Autosave for existing defects.
    LaunchedEffect(d) { val x = d; if (x != null && x.id != 0L && x.title.isNotBlank()) { delay(500); c.defects.save(x) } }

    Scaffold(topBar = { BackTopBar(stringResource(if (savedId == null) R.string.add_defect else R.string.defect), nav::back) {
        if (savedId != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
    } }) { pad ->
        val x = d ?: return@Scaffold
        fun set(t: (Defect) -> Defect) { d = t(x) }
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (x.itemId != null) InfoNote(stringResource(R.string.defect_from_checkpoint))
            OutlinedTextField(x.title, { v -> set { it.copy(title = v) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.defect_title)) })
            OutlinedTextField(x.description ?: "", { v -> set { it.copy(description = v.ifBlank { null }) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.f_description)) }, minLines = 2)
            Text(stringResource(R.string.severity), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Severity.entries.forEach { s -> FilterChip(x.severity == s, { set { it.copy(severity = s) } }, { Text(stringResource(Labels.severity(s))) }) } }
            Text(stringResource(R.string.priority), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Priority.entries.forEach { s -> FilterChip(x.priority == s, { set { it.copy(priority = s) } }, { Text(stringResource(Labels.priority(s))) }) } }
            Dropdown(stringResource(R.string.room), listOf<Long?>(null) + rooms.map { it.id }, x.roomId, { rid -> rooms.firstOrNull { it.id == rid }?.name ?: stringResource(R.string.whole_property) }) { v -> set { it.copy(roomId = v) } }
            Dropdown(stringResource(R.string.category), listOf<String?>(null) + CheckpointLibrary.categories.map { it.code }, x.categoryCode, { code -> code?.let { stringResource(Labels.category(it)) } ?: "–" }) { v -> set { it.copy(categoryCode = v) } }
            Dropdown(stringResource(R.string.defect_status), DefectStatus.entries, x.status, { stringResource(Labels.defectStatus(it)) }) { v -> set { it.copy(status = v) } }
            FlowRow {
                listOf(R.string.specialist_needed to x.specialistNeeded, R.string.df_moisture to x.isMoisture, R.string.df_structural to x.isStructural,
                    R.string.critical_safety to x.isSafety, R.string.critical_legal to x.isLegal, R.string.quote_received to x.quoteReceived).forEachIndexed { i, (l, v) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(v, { b -> set { when (i) { 0 -> it.copy(specialistNeeded = b); 1 -> it.copy(isMoisture = b); 2 -> it.copy(isStructural = b); 3 -> it.copy(isSafety = b); 4 -> it.copy(isLegal = b); else -> it.copy(quoteReceived = b) } } })
                        Text(stringResource(l))
                    }
                }
            }
            OutlinedTextField(x.cause ?: "", { v -> set { it.copy(cause = v.ifBlank { null }) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.possible_cause)) })
            OutlinedTextField(x.solution ?: "", { v -> set { it.copy(solution = v.ifBlank { null }) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.possible_solution)) })
            SectionCard(stringResource(R.string.cost_estimate)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(stringResource(R.string.cost_min), x.costMin, Modifier.weight(1f)) { v -> set { it.copy(costMin = v) } }
                    NumField(stringResource(R.string.cost_max), x.costMax, Modifier.weight(1f)) { v -> set { it.copy(costMax = v) } }
                }
                NumField(stringResource(R.string.cost_own), x.costOwn, Modifier.fillMaxWidth()) { v -> set { it.copy(costOwn = v) } }
                Dropdown(stringResource(R.string.repair_project), listOf<Long?>(null) + projects.map { it.id }, x.repairProjectId, { pj -> projects.firstOrNull { it.id == pj }?.name ?: stringResource(R.string.none) }) { v -> set { it.copy(repairProjectId = v) } }
                if (x.repairProjectId != null) Text(stringResource(R.string.project_no_double), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(x.notes ?: "", { v -> set { it.copy(notes = v.ifBlank { null }) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.notes)) }, minLines = 3)
            if (savedId == null) {
                Button(enabled = x.title.isNotBlank(), onClick = {
                    scope.launch {
                        val s = c.defects.findSimilar(pid, x.title, x.roomId)
                        if (s != null) similar = s else { val nid = c.defects.save(x); savedId = nid; d = x.copy(id = nid) }
                    }
                }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.save)) }
            } else {
                val sid = savedId!!
                SectionCard(stringResource(R.string.tab_photos)) {
                    Button(onClick = { nav.camera(CameraTarget(pid, defectId = sid, roomId = x.roomId, kind = PhotoKind.DEFECT)) }) { Icon(Icons.Filled.CameraAlt, null); Text(" " + stringResource(R.string.take_photo)) }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        photos.filter { it.defectId == sid }.forEach { ph -> Thumb(CasaFiles.file(ph.thumbPath ?: ph.filePath), Modifier.size(96.dp).clickable { nav.photo(pid, ph.id) }) }
                    }
                }
                x.itemId?.let { iid -> OutlinedButton(onClick = { nav.item(iid) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_checkpoint)) } }
            }
        }
    }
    similar?.let { s ->
        AlertDialog(onDismissRequest = { similar = null }, title = { Text(stringResource(R.string.possible_duplicate)) },
            text = { Column { WarningCard(stringResource(R.string.possible_duplicate_text, s.title)) } },
            confirmButton = { TextButton(onClick = { val x = d!!; similar = null; scope.launch { val nid = c.defects.save(x); savedId = nid; d = x.copy(id = nid) } }) { Text(stringResource(R.string.save_anyway)) } },
            dismissButton = { TextButton(onClick = { similar = null; nav.back(); nav.defect(pid, s.id) }) { Text(stringResource(R.string.open_existing_defect)) } })
    }
    if (confirmDelete) ConfirmDialog(stringResource(R.string.delete_defect), stringResource(R.string.delete_defect_text), stringResource(R.string.delete),
        onConfirm = { savedId?.let { sid -> c.scope.launch { c.defects.delete(sid) } }; nav.back() }, onDismiss = { confirmDelete = false })
}
