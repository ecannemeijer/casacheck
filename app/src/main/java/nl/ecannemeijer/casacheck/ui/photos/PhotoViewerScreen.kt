package nl.ecannemeijer.casacheck.ui.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.Dropdown
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.detail.PhotoFilters
import nl.ecannemeijer.casacheck.ui.nav.Nav

@Composable
fun PhotoViewerScreen(c: AppContainer, nav: Nav, pid: Long, photoId: Long, filter: String) {
    val all by remember { c.photos.observe(pid) }.collectAsState(null)
    // The opened photo is shown even if it does not match the filter; once it is deleted, the remaining photos stay browsable.
    val list = all?.let { a -> PhotoFilters.apply(a, filter).let { l -> if (l.any { it.id == photoId } || a.none { it.id == photoId }) l else a.filter { it.id == photoId } } }
    if (list == null) return
    if (list.isEmpty()) { LaunchedEffect(Unit) { nav.back() }; return }
    val startIndex = list.indexOfFirst { it.id == photoId }.coerceAtLeast(0)
    val pager = rememberPagerState(startIndex) { list.size }
    val current = list.getOrNull(pager.currentPage)
    var info by remember { mutableStateOf(false) }
    var showEdited by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }
    var premark by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    Scaffold(topBar = {
        BackTopBar("${pager.currentPage + 1} / ${list.size}", nav::back) {
            current?.let { ph ->
                IconButton(onClick = { nav.annotate(ph.id) }) { Icon(Icons.Filled.Brush, stringResource(R.string.annotate)) }
                IconButton(onClick = { nav.photoCompare(pid, ph.id) }) { Icon(Icons.Filled.CompareArrows, stringResource(R.string.compare)) }
                IconButton(onClick = { CasaFiles.file(if (showEdited) ph.editedPath ?: ph.filePath else ph.filePath)?.let { Intents.share(ctx, it, "image/jpeg") } }) { Icon(Icons.Filled.Share, stringResource(R.string.share)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete_photo)) }
                IconButton(onClick = { info = !info }) { Icon(Icons.Filled.Info, stringResource(R.string.details)) }
            }
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth().background(Color.Black), key = { list[it].id }) { page ->
                val ph = list[page]
                val zoom = remember(ph.id) { ZoomState() }
                ZoomableImage(CasaFiles.file(if (showEdited) ph.editedPath ?: ph.filePath else ph.filePath), zoom, Modifier.fillMaxSize())
            }
            if (info && current != null) PhotoDetails(c, nav, current, showEdited, { showEdited = it }, { confirmDelete = true }, { premark = true }, Modifier.heightIn(max = 380.dp))
        }
    }
    if (confirmDelete && current != null) ConfirmDialog(stringResource(R.string.delete_photo), stringResource(R.string.delete_photo_text), stringResource(R.string.delete),
        onConfirm = { c.scope.launch { c.photos.delete(current) } }, onDismiss = { confirmDelete = false })
    if (premark && current != null) PremarkDialog(c, current, onCreated = { id -> nav.item(id) }) { premark = false }
}

@Composable
private fun PhotoDetails(c: AppContainer, nav: Nav, ph: Photo, showEdited: Boolean, setEdited: (Boolean) -> Unit, onDelete: () -> Unit, onPremark: () -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val rooms by remember(ph.propertyId) { c.rooms.observeRooms(ph.propertyId) }.collectAsState(emptyList())
    val items by remember(ph.propertyId) { c.inspections.observeItems(ph.propertyId) }.collectAsState(emptyList())
    val property by remember(ph.propertyId) { c.properties.observe(ph.propertyId) }.collectAsState(null)
    var links by remember(ph.id) { mutableStateOf<List<Long>>(emptyList()) }
    var linkDialog by remember { mutableStateOf(false) }
    LaunchedEffect(ph.id, linkDialog) { links = c.photos.links(ph.id).map { it.itemId } }
    fun upd(t: (Photo) -> Photo) { scope.launch { c.photos.get(ph.id)?.let { c.photos.update(t(it)) } } }
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ph.editedPath != null) Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.show_annotated), Modifier.weight(1f)); Switch(showEdited, setEdited) }
        AutoSaveField(stringResource(R.string.caption), ph.caption, { v -> upd { it.copy(caption = v.ifBlank { null }) } })
        AutoSaveField(stringResource(R.string.notes), ph.notes, { v -> upd { it.copy(notes = v.ifBlank { null }) } }, singleLine = false)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(ph.important, { v -> upd { it.copy(important = v) } }); Text(stringResource(R.string.important), Modifier.weight(1f))
            Checkbox(ph.includeInReport, { v -> upd { it.copy(includeInReport = v) } }); Text(stringResource(R.string.in_report))
        }
        Dropdown(stringResource(R.string.photo_kind), PhotoKind.entries, ph.kind, { stringResource(Labels.photoKind(it)) }) { k -> upd { it.copy(kind = k) } }
        Dropdown(stringResource(R.string.room), listOf<Long?>(null) + rooms.map { it.id }, ph.roomId, { r -> rooms.firstOrNull { it.id == r }?.name ?: "–" }) { r -> upd { it.copy(roomId = r) } }
        Text(stringResource(R.string.linked_checkpoints), style = MaterialTheme.typography.titleSmall)
        links.forEach { iid -> items.firstOrNull { it.id == iid }?.let { i -> Text("• " + c.inspections.titleOf(i), Modifier.clickable { nav.item(iid) }) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { linkDialog = true }) { Text(stringResource(R.string.link_to_checkpoints)) }
            OutlinedButton(onClick = onPremark) { Text(stringResource(R.string.make_attention_point)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { c.properties.setMainPhoto(ph.propertyId, ph.id) } }, enabled = property?.mainPhotoId != ph.id) { Icon(Icons.Filled.Star, null); Text(" " + stringResource(R.string.set_main_photo)) }
            OutlinedButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null); Text(" " + stringResource(R.string.delete)) }
        }
    }
    if (linkDialog) {
        var q by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { linkDialog = false }, title = { Text(stringResource(R.string.link_to_checkpoints)) }, text = {
            Column {
                OutlinedTextField(q, { q = it }, label = { Text(stringResource(R.string.search)) }, singleLine = true)
                val titled = items.map { it to c.inspections.titleOf(it) }.filter { q.isBlank() || it.second.contains(q, true) }
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(titled, key = { it.first.id }) { (i, t) ->
                        val on = i.id in links
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch { if (on) c.photos.unlink(ph.id, i.id) else c.photos.link(ph.id, i.id); links = c.photos.links(ph.id).map { it.itemId } }
                        }) {
                            Checkbox(on, null)
                            Column { Text(t); i.roomId?.let { r -> rooms.firstOrNull { it.id == r }?.name }?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { linkDialog = false }) { Text(stringResource(R.string.done)) } })
    }
}

/** Pre-marking: turns a (listing) photo into a PREMARKED checkpoint that the wizard shows first. */
@Composable
fun PremarkDialog(c: AppContainer, ph: Photo, onCreated: (Long) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.make_attention_point)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.premark_help), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.premark_title_hint)) }, singleLine = true)
            OutlinedTextField(desc, { desc = it }, label = { Text(stringResource(R.string.f_description)) }, minLines = 2)
        }
    }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = {
        scope.launch { val id = c.inspections.createPremarked(ph.propertyId, title.trim(), desc.ifBlank { null }, ph.id, ph.roomId); onDismiss(); onCreated(id) }
    }) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
