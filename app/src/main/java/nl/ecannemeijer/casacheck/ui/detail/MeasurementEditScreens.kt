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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CrackDirection
import nl.ecannemeijer.casacheck.data.db.CrackMeasurement
import nl.ecannemeijer.casacheck.data.db.MoistureMeasurement
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.data.report.PdfReportGenerator
import nl.ecannemeijer.casacheck.domain.moisture.MoistureAnalyzer
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.Dropdown
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.CameraTarget
import nl.ecannemeijer.casacheck.ui.nav.Nav

@Composable
fun DecField(label: String, value: Double?, modifier: Modifier = Modifier, onChange: (Double?) -> Unit) {
    var t by remember(value == null) { mutableStateOf(Fmt.num(value)) }
    OutlinedTextField(t, { t = it; onChange(Fmt.parseDouble(it)) }, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun TextF(label: String, value: String?, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String?) -> Unit) =
    OutlinedTextField(value ?: "", { onChange(it.ifBlank { null }) }, modifier, label = { Text(label) }, singleLine = true)

@Composable
fun MoistureEditScreen(c: AppContainer, nav: Nav, pid: Long, id: Long?) {
    val vm = vm("detail$pid") { DetailVm(c, pid) }
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var m by remember { mutableStateOf<MoistureMeasurement?>(if (id == null) MoistureMeasurement(propertyId = pid, wall = "") else null) }
    val pts = remember { mutableStateListOf<Pair<Int, Double?>>() }
    var savedId by remember { mutableStateOf(id) }
    var newHeight by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(id, settings) {
        if (loaded || settings == null) return@LaunchedEffect
        if (id != null) { m = c.measurements.moisture(id); pts.addAll(c.measurements.points(id).map { it.heightCm to it.value }) }
        else pts.addAll(MoistureAnalyzer.parseHeights(settings!!.moistureHeights).map { it to null })
        loaded = true
    }
    val snapshot = pts.toList()
    LaunchedEffect(m, snapshot) { val x = m; if (loaded && x != null && x.id != 0L && x.wall.isNotBlank()) { delay(600); c.measurements.saveMoisture(x, snapshot) } }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.moisture_measurement), nav::back) {
        if (savedId != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
    } }) { pad ->
        val x = m ?: return@Scaffold
        fun set(t: (MoistureMeasurement) -> MoistureMeasurement) { m = t(x) }
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Dropdown(stringResource(R.string.room), listOf<Long?>(null) + rooms.map { it.id }, x.roomId, { rid -> rooms.firstOrNull { it.id == rid }?.name ?: "–" }) { v -> set { it.copy(roomId = v) } }
            TextF(stringResource(R.string.wall_name), x.wall) { v -> set { it.copy(wall = v ?: "") } }
            TextF(stringResource(R.string.floor), x.floorName) { v -> set { it.copy(floorName = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextF(stringResource(R.string.m_meter), x.meterType, Modifier.weight(1f)) { v -> set { it.copy(meterType = v) } }
                TextF(stringResource(R.string.m_unit), x.unit, Modifier.weight(1f)) { v -> set { it.copy(unit = v) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextF(stringResource(R.string.m_method), x.method, Modifier.weight(1f)) { v -> set { it.copy(method = v) } }
                TextF(stringResource(R.string.m_material), x.material, Modifier.weight(1f)) { v -> set { it.copy(material = v) } }
            }
            DecField(stringResource(R.string.m_reference), x.referenceValue, Modifier.fillMaxWidth()) { v -> set { it.copy(referenceValue = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecField(stringResource(R.string.m_temperature), x.temperature, Modifier.weight(1f)) { v -> set { it.copy(temperature = v) } }
                DecField(stringResource(R.string.m_rh), x.relativeHumidity, Modifier.weight(1f)) { v -> set { it.copy(relativeHumidity = v) } }
            }
            SectionCard(stringResource(R.string.readings_per_height)) {
                pts.sortedBy { it.first }.forEach { (h, v) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$h cm", Modifier.width(70.dp), style = MaterialTheme.typography.titleMedium)
                        DecField(stringResource(R.string.reading), v, Modifier.weight(1f)) { nv -> val i = pts.indexOfFirst { it.first == h }; if (i >= 0) pts[i] = h to nv }
                        IconButton(onClick = { pts.removeAll { it.first == h } }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(newHeight, { newHeight = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.extra_height_cm)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedButton(onClick = { newHeight.toIntOrNull()?.takeIf { it in 1..1000 && pts.none { p -> p.first == it } }?.let { pts.add(it to null) }; newHeight = "" }) { Text(stringResource(R.string.add)) }
                }
            }
            val valid = pts.filter { it.second != null }.map { it.first to it.second!! }
            val a = MoistureAnalyzer.analyze(valid, x.referenceValue)
            SectionCard(stringResource(R.string.wall_view)) {
                MoistureWall(valid, x.referenceValue, Modifier.fillMaxWidth().height(200.dp))
                Text(stringResource(PdfReportGenerator.patternText(a.pattern)), style = MaterialTheme.typography.bodyLarge)
                if (a.elevatedHeights.isNotEmpty()) Text(stringResource(R.string.elevated_at, a.elevatedHeights.joinToString { "$it cm" }), style = MaterialTheme.typography.bodySmall)
                InfoNote(stringResource(R.string.moisture_disclaimer))
            }
            OutlinedTextField(x.notes ?: "", { v -> set { it.copy(notes = v.ifBlank { null }) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.notes)) }, minLines = 2)
            if (savedId == null) Button(enabled = x.wall.isNotBlank(), onClick = { scope.launch { val nid = c.measurements.saveMoisture(x, pts.toList()); savedId = nid; m = x.copy(id = nid) } },
                modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.save)) }
            else {
                val sid = savedId!!
                Button(onClick = { nav.camera(CameraTarget(pid, moistureId = sid, roomId = x.roomId)) }) { Icon(Icons.Filled.CameraAlt, null); Text(" " + stringResource(R.string.take_photo)) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    photos.filter { it.moistureId == sid }.forEach { ph -> Thumb(CasaFiles.file(ph.thumbPath ?: ph.filePath), Modifier.size(96.dp).clickable { nav.photo(pid, ph.id) }) }
                }
            }
        }
    }
    if (confirmDelete) ConfirmDialog(stringResource(R.string.delete), stringResource(R.string.delete_measurement_text), stringResource(R.string.delete),
        onConfirm = { savedId?.let { sid -> c.scope.launch { c.measurements.deleteMoisture(sid) } }; nav.back() }, onDismiss = { confirmDelete = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrackEditScreen(c: AppContainer, nav: Nav, pid: Long, id: Long?, prev: Long?) {
    val vm = vm("detail$pid") { DetailVm(c, pid) }
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val cracks by vm.cracks.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var cr by remember { mutableStateOf<CrackMeasurement?>(null) }
    var savedId by remember { mutableStateOf(id) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(cracks.isNotEmpty() || id == null && prev == null) {
        if (cr != null) return@LaunchedEffect
        cr = when {
            id != null -> cracks.firstOrNull { it.id == id }
            prev != null -> cracks.firstOrNull { it.id == prev }?.let { p -> CrackMeasurement(propertyId = pid, roomId = p.roomId, previousId = p.id, location = p.location, direction = p.direction, inside = p.inside) }
            else -> CrackMeasurement(propertyId = pid, location = "")
        }
    }
    LaunchedEffect(cr) { val x = cr; if (x != null && x.id != 0L && x.location.isNotBlank()) { delay(500); c.measurements.saveCrack(x) } }
    Scaffold(topBar = { BackTopBar(stringResource(if (prev != null) R.string.crack_followup else R.string.crack_measurement), nav::back) {
        if (savedId != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
    } }) { pad ->
        val x = cr ?: return@Scaffold
        fun set(t: (CrackMeasurement) -> CrackMeasurement) { cr = t(x) }
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val previous = x.previousId?.let { p -> cracks.firstOrNull { it.id == p } }
            previous?.let { InfoNote(stringResource(R.string.previous_measurement, java.text.DateFormat.getDateInstance().format(java.util.Date(it.measuredAt)), it.widthMm?.toString() ?: "–", it.lengthCm?.toString() ?: "–")) }
            Dropdown(stringResource(R.string.room), listOf<Long?>(null) + rooms.map { it.id }, x.roomId, { rid -> rooms.firstOrNull { it.id == rid }?.name ?: "–" }) { v -> set { it.copy(roomId = v) } }
            TextF(stringResource(R.string.crack_location), x.location) { v -> set { it.copy(location = v ?: "") } }
            Text(stringResource(R.string.crack_direction), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { CrackDirection.entries.forEach { d -> FilterChip(x.direction == d, { set { it.copy(direction = d) } }, { Text(stringResource(Labels.crack(d))) }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(x.inside, { set { it.copy(inside = true) } }, { Text(stringResource(R.string.inside)) })
                FilterChip(!x.inside, { set { it.copy(inside = false) } }, { Text(stringResource(R.string.outside)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecField(stringResource(R.string.crack_width_mm), x.widthMm, Modifier.weight(1f)) { v -> set { it.copy(widthMm = v) } }
                DecField(stringResource(R.string.crack_length_cm), x.lengthCm, Modifier.weight(1f)) { v -> set { it.copy(lengthCm = v) } }
            }
            InfoNote(stringResource(R.string.crack_width_note))
            if (previous != null) OutlinedTextField(x.changeNotes ?: "", { v -> set { it.copy(changeNotes = v.ifBlank { null }) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.crack_changes)) })
            OutlinedTextField(x.notes ?: "", { v -> set { it.copy(notes = v.ifBlank { null }) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.notes)) }, minLines = 2)
            if (savedId == null) Button(enabled = x.location.isNotBlank(), onClick = { scope.launch { val nid = c.measurements.saveCrack(x); savedId = nid; cr = x.copy(id = nid) } },
                modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.save)) }
            else {
                val sid = savedId!!
                Button(onClick = { nav.camera(CameraTarget(pid, crackId = sid, roomId = x.roomId)) }) { Icon(Icons.Filled.CameraAlt, null); Text(" " + stringResource(R.string.take_photo)) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    photos.filter { it.crackId == sid }.forEach { ph -> Thumb(CasaFiles.file(ph.thumbPath ?: ph.filePath), Modifier.size(96.dp).clickable { nav.photo(pid, ph.id) }) }
                }
                OutlinedButton(onClick = { nav.back(); nav.crack(pid, prev = sid) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.crack_followup)) }
            }
        }
    }
    if (confirmDelete) ConfirmDialog(stringResource(R.string.delete), stringResource(R.string.delete_measurement_text), stringResource(R.string.delete),
        onConfirm = { savedId?.let { sid -> c.scope.launch { c.measurements.deleteCrack(sid) } }; nav.back() }, onDismiss = { confirmDelete = false })
}
