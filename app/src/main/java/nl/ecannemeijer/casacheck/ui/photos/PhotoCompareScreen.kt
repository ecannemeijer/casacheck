package nl.ecannemeijer.casacheck.ui.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.nav.Nav

/** Side-by-side comparison (e.g. listing photo vs. own photo) with optional synchronised zoom. */
@Composable
fun PhotoCompareScreen(c: AppContainer, nav: Nav, pid: Long, a: Long?, b: Long?) {
    val photos by remember { c.photos.observe(pid) }.collectAsState(emptyList())
    var left by remember { mutableStateOf(a) }
    var right by remember { mutableStateOf(b) }
    var sync by remember { mutableStateOf(true) }
    var picking by remember { mutableStateOf<Int?>(null) }
    val shared = remember { ZoomState() }
    val zl = remember { ZoomState() }
    val zr = remember { ZoomState() }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.compare_photos), nav::back) {
        Text(stringResource(R.string.sync_zoom), style = MaterialTheme.typography.bodySmall); Switch(sync, { sync = it; shared.reset(); zl.reset(); zr.reset() })
    } }) { pad ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(pad)) {
            val landscape = maxWidth > maxHeight
            @Composable fun Pane(id: Long?, side: Int, z: ZoomState, m: Modifier) {
                val ph = photos.firstOrNull { it.id == id }
                Column(m) {
                    Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                        if (ph != null) ZoomableImage(CasaFiles.file(ph.editedPath ?: ph.filePath), if (sync) shared else z, Modifier.fillMaxSize())
                        else Text(stringResource(R.string.choose_photo), Modifier.align(Alignment.Center), color = Color.White)
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ph?.let { stringResource(Labels.photoKind(it.kind)) + (it.caption?.let { c -> " · $c" } ?: "") } ?: "", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { picking = side }) { Text(stringResource(R.string.choose_photo)) }
                    }
                }
            }
            if (landscape) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Pane(left, 0, zl, Modifier.weight(1f)); Pane(right, 1, zr, Modifier.weight(1f))
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Pane(left, 0, zl, Modifier.weight(1f)); Pane(right, 1, zr, Modifier.weight(1f))
            }
        }
    }
    picking?.let { side ->
        AlertDialog(onDismissRequest = { picking = null }, title = { Text(stringResource(R.string.choose_photo)) }, text = {
            LazyVerticalGrid(GridCells.Adaptive(88.dp), Modifier.heightIn(max = 440.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(photos, key = { it.id }) { ph: Photo ->
                    Thumb(CasaFiles.file(ph.thumbPath ?: ph.filePath), Modifier.aspectRatio(1f).border(1.dp, MaterialTheme.colorScheme.outline).clickable {
                        if (side == 0) left = ph.id else right = ph.id; picking = null; shared.reset()
                    })
                }
            }
        }, confirmButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel)) } })
    }
}
