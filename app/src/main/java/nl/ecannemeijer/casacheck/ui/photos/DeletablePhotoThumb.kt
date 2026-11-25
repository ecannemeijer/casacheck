package nl.ecannemeijer.casacheck.ui.photos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.Thumb

/**
 * Photo thumbnail with delete: tap opens the full view, long-press or the small bin button asks to delete it
 * (file + DB reference), with confirmation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeletablePhotoThumb(c: AppContainer, ph: Photo, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    var ask by remember(ph.id) { mutableStateOf(false) }
    Box(modifier) {
        Thumb(CasaFiles.file(ph.thumbPath ?: ph.filePath), Modifier.fillMaxSize().combinedClickable(onClick = onOpen, onLongClick = { ask = true }))
        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(32.dp)) {
            IconButton(onClick = { ask = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, stringResource(R.string.delete_photo), tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (ask) ConfirmDialog(stringResource(R.string.delete_photo), stringResource(R.string.delete_photo_text), stringResource(R.string.delete),
        onConfirm = { c.scope.launch { c.photos.delete(ph) } }, onDismiss = { ask = false })
}