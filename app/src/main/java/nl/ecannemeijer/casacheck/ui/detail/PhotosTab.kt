package nl.ecannemeijer.casacheck.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.ui.common.EmptyState
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.nav.CameraTarget
import nl.ecannemeijer.casacheck.ui.nav.Nav

/** Photo filters shared by the photo tab and the full-screen viewer. */
object PhotoFilters {
    val all = listOf("ALL", "LISTING", "INSPECTION", "DEFECT", "FLOORPLAN", "REFERENCE", "DOCUMENT", "IMPORTANT", "REPORT")
    fun label(f: String) = when (f) {
        "ALL" -> R.string.all; "IMPORTANT" -> R.string.important; "REPORT" -> R.string.in_report
        "LISTING" -> R.string.pk_listing; "INSPECTION" -> R.string.pk_inspection; "DEFECT" -> R.string.pk_defect; "FLOORPLAN" -> R.string.pk_floorplan
        "REFERENCE" -> R.string.pk_reference; else -> R.string.pk_document
    }
    fun apply(list: List<Photo>, f: String): List<Photo> = when (f) {
        "ALL" -> list; "IMPORTANT" -> list.filter { it.important }; "REPORT" -> list.filter { it.includeInReport }
        else -> list.filter { it.kind.name == f }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosTab(vm: DetailVm, nav: Nav) {
    val photos by vm.photos.collectAsStateWithLifecycle()
    val lp by vm.listingPhotos.collectAsStateWithLifecycle()
    val removed = lp.filter { it.removedFromListing }.map { it.photoId }.toSet()
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var sel by remember { mutableStateOf(listOf<Long>()) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
        scope.launch { uris.forEach { u -> runCatching { vm.c.photos.addFromUri(vm.pid, u, PhotoKind.INSPECTION) } } }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { nav.camera(CameraTarget(vm.pid)) }) { Icon(Icons.Filled.CameraAlt, null); Text(" " + stringResource(R.string.take_photo)) }
            OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Filled.AddPhotoAlternate, null); Text(" " + stringResource(R.string.from_gallery)) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PhotoFilters.all.forEach { f ->
                val n = PhotoFilters.apply(photos, f).size
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(stringResource(PhotoFilters.label(f)) + " ($n)") })
            }
        }
        if (sel.isNotEmpty()) Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.compare_pick_hint, sel.size), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Button(enabled = sel.size == 2, onClick = { nav.photoCompare(vm.pid, sel[0], sel[1]); sel = emptyList() }) { Icon(Icons.Filled.CompareArrows, null); Text(" " + stringResource(R.string.compare)) }
        }
        val list = PhotoFilters.apply(photos, filter)
        if (list.isEmpty()) EmptyState(Icons.Filled.CameraAlt, stringResource(R.string.no_photos), stringResource(R.string.no_photos_text))
        LazyVerticalGrid(GridCells.Adaptive(110.dp), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.id }) { ph ->
                val isSel = ph.id in sel
                Box(Modifier.aspectRatio(1f).then(if (isSel) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary)) else Modifier)
                    .combinedClickable(onClick = { if (sel.isNotEmpty()) sel = if (isSel) sel - ph.id else (sel + ph.id).takeLast(2) else nav.photo(vm.pid, ph.id, filter) },
                        onLongClick = { sel = if (isSel) sel - ph.id else (sel + ph.id).takeLast(2) })) {
                    Thumb(CasaFiles.file(ph.thumbPath ?: ph.filePath), Modifier.fillMaxSize())
                    Row(Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                        if (ph.important) Badge(Icons.Filled.PriorityHigh)
                        if (ph.editedPath != null) Badge(Icons.Filled.Brush)
                        if (vm.property.value?.mainPhotoId == ph.id) Badge(Icons.Filled.Star)
                    }
                    if (ph.id in removed) Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                        Text(stringResource(R.string.removed_from_listing), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), shape = MaterialTheme.shapes.small, modifier = Modifier.padding(1.dp)) {
        Icon(icon, null, Modifier.size(18.dp).padding(1.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
