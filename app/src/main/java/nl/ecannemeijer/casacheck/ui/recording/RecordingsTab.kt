package nl.ecannemeijer.casacheck.ui.recording

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.audio.AudioMath
import nl.ecannemeijer.casacheck.data.db.RecStatus
import nl.ecannemeijer.casacheck.data.db.Recording
import nl.ecannemeijer.casacheck.data.db.TxStatus
import nl.ecannemeijer.casacheck.data.transcribe.TranscribeWorker
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.EmptyState
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.nav.Nav
import java.text.DateFormat
import java.util.Date

/** Recordings tab of a property: record button, import, list with share/delete. */
@Composable
fun RecordingsTab(c: AppContainer, nav: Nav, pid: Long) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val list by remember(pid) { c.recordings.observe(pid) }.collectAsState(null)
    var toDelete by remember { mutableStateOf<Recording?>(null) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { c.recordings.importAudio(pid, uri) }
                .onSuccess { Toast.makeText(ctx, R.string.rec_imported, Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, ctx.getString(R.string.rec_import_failed, it.message ?: "?"), Toast.LENGTH_LONG).show() }
        }
    }
    toDelete?.let { r ->
        ConfirmDialog(stringResource(R.string.rec_delete_title), stringResource(R.string.rec_delete_text, r.name), stringResource(R.string.delete),
            onConfirm = { toDelete = null; deleteRecording(c, ctx, r) }, onDismiss = { toDelete = null })
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { RecordControl(pid) }
        item {
            Text(stringResource(R.string.rec_quality_note, AudioMath.formatSize(AudioMath.bytesPerHour()), AudioMath.formatSize(c.recordings.freeBytes())),
                style = MaterialTheme.typography.bodySmall)
        }
        val l = list
        if (l != null && l.isEmpty()) item {
            EmptyState(Icons.Filled.GraphicEq, stringResource(R.string.rec_empty_title), stringResource(R.string.rec_empty_text))
        }
        items(l.orEmpty(), key = { it.id }) { r ->
            RecordingRow(r, onOpen = { if (r.status == RecStatus.DONE) nav.recording(r.id) },
                onShare = { shareRecording(c, ctx, r) }, onDelete = { toDelete = r })
        }
        item {
            OutlinedButton(onClick = { import.launch(arrayOf("audio/*")) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.LibraryMusic, null); Text(" " + stringResource(R.string.rec_import))
            }
        }
    }
}

@Composable
private fun RecordingRow(r: Recording, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.GraphicEq, null, tint = if (r.status == RecStatus.RECORDING) RecordRed else MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(r.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.createdAt))
                Text(if (r.status == RecStatus.RECORDING) stringResource(R.string.rec_status_recording)
                     else "$date · ${AudioMath.formatTime(r.durationMs)} · ${AudioMath.formatSize(r.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                val tx = when (r.transcriptStatus) {
                    TxStatus.DONE -> R.string.tx_badge_done; TxStatus.RUNNING, TxStatus.QUEUED -> R.string.tx_badge_running; TxStatus.FAILED -> R.string.tx_badge_failed; else -> null
                }
                tx?.let { Text(stringResource(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
                if (!r.notes.isNullOrBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (r.status == RecStatus.DONE) {
                IconButton(onClick = onShare) { Icon(Icons.Filled.Share, stringResource(R.string.share)) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
            }
        }
    }
}

fun shareRecording(c: AppContainer, ctx: android.content.Context, r: Recording) {
    val f = c.recordings.fileOf(r)
    if (!f.exists()) { Toast.makeText(ctx, R.string.rec_file_missing, Toast.LENGTH_LONG).show(); return }
    // Share a copy with a readable name (the stored file is named after its uuid).
    val out = runCatching {
        val dir = java.io.File(ctx.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.filter { it.extension in setOf("m4a", "aac", "wav", "mp3", "ogg", "opus", "amr", "3gp") }?.forEach { it.delete() }
        f.copyTo(java.io.File(dir, AudioMath.shareName(r.name, r.filePath)), overwrite = true)
    }.getOrDefault(f)
    Intents.share(ctx, out, r.mime.takeIf { it.startsWith("audio/") } ?: "audio/*", r.name)
}

fun deleteRecording(c: AppContainer, ctx: android.content.Context, r: Recording) {
    c.player.release(r.id)
    TranscribeWorker.cancel(ctx, r.id)
    c.scope.launch { c.recordings.delete(r.id) }
}