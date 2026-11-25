package nl.ecannemeijer.casacheck.ui.inspection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.ItemWithResult
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.checklist.splitSteps
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.common.WarningCard
import nl.ecannemeijer.casacheck.ui.common.specialistStyle
import nl.ecannemeijer.casacheck.ui.common.statusStyle
import nl.ecannemeijer.casacheck.ui.nav.CameraTarget
import nl.ecannemeijer.casacheck.ui.nav.Nav

val STATUS_BUTTONS = listOf(CheckStatus.OK, CheckStatus.MINOR, CheckStatus.MAJOR, CheckStatus.SERIOUS, CheckStatus.UNSURE, CheckStatus.NOT_APPLICABLE)

/** Large one-hand status buttons: icon + text + colour (never colour alone). */
@Composable
fun StatusButtons(current: CheckStatus, onSelect: (CheckStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        STATUS_BUTTONS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { s ->
                    val st = statusStyle(s)
                    val selected = current == s
                    Button(onClick = { onSelect(if (selected) CheckStatus.NOT_CHECKED else s) }, modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                        colors = if (selected) ButtonDefaults.buttonColors(containerColor = st.color, contentColor = Color.White)
                        else ButtonDefaults.buttonColors(containerColor = st.color.copy(alpha = 0.12f), contentColor = st.color),
                        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null, shape = RoundedCornerShape(14.dp)) {
                        Icon(st.icon, null, Modifier.size(26.dp))
                        Text(" " + stringResource(st.label) + if (selected) " ✓" else "", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckpointPage(c: AppContainer, nav: Nav, item: ItemWithResult, roomName: String?, modifier: Modifier = Modifier) {
    val def = CheckpointLibrary.byCode[item.templateCode]
    val photos by remember(item.id) { c.photos.observeForItem(item.id) }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var howOpen by rememberSaveable(item.id) { mutableStateOf(false) }
    var moreOpen by rememberSaveable(item.id) { mutableStateOf(false) }
    var compare by rememberSaveable(item.id) { mutableStateOf(false) }
    var linkDialog by remember { mutableStateOf(false) }
    val status = item.effectiveStatus
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        scope.launch { uris.forEach { runCatching { c.photos.addFromUri(item.propertyId, it, if (status.isDefect) PhotoKind.DEFECT else PhotoKind.INSPECTION, itemId = item.id, roomId = item.roomId) } } }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(listOfNotNull(stringResource(Labels.category(item.categoryCode)), roomName).joinToString(" · "), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(c.inspections.titleOf(item), style = MaterialTheme.typography.headlineSmall)
        if (def != null) Text(stringResource(def.explanation), style = MaterialTheme.typography.bodyLarge)
        item.customDescription?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        item.sourcePhotoId?.let { spid ->
            val src by remember(spid) { c.photos.observeOne(spid) }.collectAsState(null)
            src?.let { ph -> Thumb(CasaFiles.file(ph.editedPath ?: ph.filePath), Modifier.fillMaxWidth().aspectRatio(4f / 3f).clickable { nav.photo(item.propertyId, ph.id) }, ContentScale.Fit) }
        }
        if (def != null) {
            Row(Modifier.fillMaxWidth().clickable { howOpen = !howOpen }, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.how_to_check), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(if (howOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
            }
            if (howOpen) SectionCard {
                splitSteps(stringResource(def.steps)).forEachIndexed { i, s -> Text("${i + 1}. $s", style = MaterialTheme.typography.bodyLarge) }
                Text(stringResource(R.string.look_for), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.lookFor))
                Text(stringResource(R.string.normal), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.normal))
                Text(stringResource(R.string.abnormal), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.abnormal))
                if (def.tools.isNotEmpty()) Text(stringResource(R.string.tools) + ": " + def.tools.mapNotNull { t -> CheckpointLibrary.tools[t] }.map { stringResource(it) }.joinToString(), style = MaterialTheme.typography.bodyMedium)
            }
            if (def.illustration != 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.reference_illustration), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (photos.isNotEmpty()) { Text(stringResource(R.string.compare), style = MaterialTheme.typography.bodySmall); Switch(compare, { compare = it }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(def.illustration), stringResource(R.string.reference_illustration), Modifier.weight(1f).aspectRatio(160f / 110f))
                    if (compare && photos.isNotEmpty()) Thumb(CasaFiles.file(photos.last().filePath), Modifier.weight(1f).aspectRatio(160f / 110f), ContentScale.Crop)
                }
                Text(stringResource(R.string.illustration_note), style = MaterialTheme.typography.bodySmall)
            }
        }

        StatusButtons(status) { s -> scope.launch { c.inspections.setStatus(item.id, s) } }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(specialistStyle.icon, null, tint = specialistStyle.color)
            Text(" " + stringResource(R.string.specialist_needed), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(item.specialistNeeded == true, { v -> scope.launch { c.inspections.setSpecialist(item.id, v) } })
        }
        if (status.isDefect && photos.isEmpty()) InfoNote(stringResource(R.string.take_photo_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { nav.camera(CameraTarget(item.propertyId, itemId = item.id, roomId = item.roomId, kind = if (status.isDefect) PhotoKind.DEFECT else PhotoKind.INSPECTION)) },
                Modifier.weight(1f).height(56.dp)) { Icon(Icons.Filled.CameraAlt, null); Text(" " + stringResource(R.string.take_photo)) }
            OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.height(56.dp)) { Icon(Icons.Filled.AddPhotoAlternate, stringResource(R.string.from_gallery)) }
            OutlinedButton(onClick = { linkDialog = true }, Modifier.height(56.dp)) { Icon(Icons.Filled.Link, stringResource(R.string.link_existing_photo)) }
        }
        if (photos.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            photos.forEach { ph -> Thumb(CasaFiles.file(ph.thumbPath ?: ph.filePath), Modifier.size(96.dp).clickable { nav.photo(item.propertyId, ph.id) }) }
        }
        AutoSaveField(stringResource(R.string.notes), item.notes, { v -> scope.launch { c.inspections.setNotes(item.id, v) } }, singleLine = false, minLines = 2)
        if (def != null && (status.isDefect || status == CheckStatus.UNSURE)) {
            Row(Modifier.fillMaxWidth().clickable { moreOpen = !moreOpen }, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.causes_and_repair), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(if (moreOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
            }
            if (moreOpen) SectionCard {
                Text(stringResource(R.string.possible_cause), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.causes))
                Text(stringResource(R.string.consequences), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.consequences))
                Text(stringResource(R.string.possible_solution), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.repair))
                Text(stringResource(R.string.which_expert), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.expert))
            }
            if (def.hasCost) Text(stringResource(R.string.indicative_cost, Fmt.range(def.costMinEur, def.costMaxEur)), style = MaterialTheme.typography.bodyMedium)
            WarningCard(stringResource(def.warning), MaterialTheme.colorScheme.tertiary)
        }
        if (status.isDefect) SectionCard(stringResource(R.string.cost_estimate)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                nl.ecannemeijer.casacheck.ui.detail.NumField(stringResource(R.string.cost_min), item.costMin, Modifier.weight(1f)) { v -> scope.launch { c.inspections.setOwnCost(item.id, v, item.costMax, item.costOwn) } }
                nl.ecannemeijer.casacheck.ui.detail.NumField(stringResource(R.string.cost_max), item.costMax, Modifier.weight(1f)) { v -> scope.launch { c.inspections.setOwnCost(item.id, item.costMin, v, item.costOwn) } }
            }
            Text(stringResource(R.string.defect_auto_created), style = MaterialTheme.typography.bodySmall)
        }
        if (item.templateCode.startsWith("custom_")) TextButton(onClick = { scope.launch { c.inspections.deleteCustomItem(item.id) }; nav.back() }) { Text(stringResource(R.string.delete_custom_point)) }
    }
    if (linkDialog) LinkPhotosDialog(c, item) { linkDialog = false }
}

@Composable
private fun LinkPhotosDialog(c: AppContainer, item: ItemWithResult, onDismiss: () -> Unit) {
    val all by remember { c.photos.observe(item.propertyId) }.collectAsState(emptyList())
    val linked by remember(item.id) { c.photos.observeForItem(item.id) }.collectAsState(emptyList())
    val ids = linked.map { it.id }.toSet()
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.link_existing_photo)) }, text = {
        LazyVerticalGrid(GridCells.Adaptive(88.dp), Modifier.heightIn(max = 420.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(all, key = { it.id }) { ph ->
                val on = ph.id in ids
                Box(Modifier.aspectRatio(1f).then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.primary) else Modifier)
                    .clickable { scope.launch { if (on) c.photos.unlink(ph.id, item.id) else c.photos.link(ph.id, item.id) } }) {
                    Thumb(CasaFiles.file(ph.thumbPath ?: ph.filePath), Modifier.fillMaxSize())
                    if (on) Icon(Icons.Filled.CheckCircle, null, Modifier.align(Alignment.TopEnd), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } })
}
