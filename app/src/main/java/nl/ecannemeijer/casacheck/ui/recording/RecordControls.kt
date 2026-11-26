package nl.ecannemeijer.casacheck.ui.recording

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.audio.AudioMath
import nl.ecannemeijer.casacheck.data.recording.RecState
import nl.ecannemeijer.casacheck.data.recording.Recorder
import nl.ecannemeijer.casacheck.ui.common.WarningCard

val RecordRed = Color(0xFFD32F2F)

private fun granted(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

/** Elapsed recording time, refreshed twice a second. */
@Composable
fun rememberElapsed(s: RecState?): Long {
    val v by produceState(s?.elapsedMs() ?: 0L, s) {
        while (s != null) { value = s.elapsedMs(); if (s.paused) break; delay(500) }
    }
    return v
}

/** Returns a "start recording" action that first explains and asks for microphone (+ notification) permission. */
@Composable
fun rememberRecordStarter(propertyId: Long): () -> Unit {
    val ctx = LocalContext.current
    var rationale by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.RECORD_AUDIO] == true || granted(ctx, Manifest.permission.RECORD_AUDIO)) Recorder.start(ctx, propertyId) else denied = true
    }
    if (rationale) AlertDialog(
        onDismissRequest = { rationale = false },
        icon = { Icon(Icons.Filled.Mic, null) },
        title = { Text(stringResource(R.string.rec_perm_title)) },
        text = { Text(stringResource(R.string.rec_perm_text)) },
        confirmButton = {
            TextButton(onClick = {
                rationale = false
                val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
                launcher.launch(perms.toTypedArray())
            }) { Text(stringResource(R.string.rec_perm_continue)) }
        },
        dismissButton = { TextButton(onClick = { rationale = false }) { Text(stringResource(R.string.cancel)) } },
    )
    if (denied) AlertDialog(
        onDismissRequest = { denied = false },
        title = { Text(stringResource(R.string.rec_perm_title)) },
        text = { Text(stringResource(R.string.rec_perm_denied)) },
        confirmButton = {
            TextButton(onClick = {
                denied = false
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text(stringResource(R.string.rec_open_settings)) }
        },
        dismissButton = { TextButton(onClick = { denied = false }) { Text(stringResource(R.string.close)) } },
    )
    return {
        if (granted(ctx, Manifest.permission.RECORD_AUDIO)) Recorder.start(ctx, propertyId) else rationale = true
    }
}

@Composable
fun RecDot(paused: Boolean) {
    Box(Modifier.size(12.dp).clip(CircleShape).background(if (paused) Color.Gray else RecordRed))
}

/** Big record button / running-recording controls for one property (Overview and Recordings tab). */
@Composable
fun RecordControl(propertyId: Long, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val st by Recorder.state.collectAsStateWithLifecycle()
    val start = rememberRecordStarter(propertyId)
    val s = st
    when {
        s == null -> Button(onClick = start, modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RecordRed, contentColor = Color.White)) {
            Icon(Icons.Filled.Mic, null); Text(" " + stringResource(R.string.rec_record_viewing))
        }
        s.propertyId == propertyId -> Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(12.dp).let { m -> if (onOpen != null) m.clickable { onOpen() } else m }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val elapsed = rememberElapsed(s)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RecDot(s.paused)
                    Text("  " + stringResource(R.string.rec_running_here, AudioMath.formatTime(elapsed)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (s.paused) Text(stringResource(if (s.autoPaused) R.string.rec_notif_paused_call else R.string.rec_notif_paused, AudioMath.formatTime(elapsed)))
                if (s.markers > 0) Text(stringResource(R.string.rec_markers_count, s.markers), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.paused) FilledTonalButton(onClick = { Recorder.resume(ctx) }) { Icon(Icons.Filled.PlayArrow, null); Text(" " + stringResource(R.string.rec_resume)) }
                    else FilledTonalButton(onClick = { Recorder.pause(ctx) }) { Icon(Icons.Filled.Pause, null); Text(" " + stringResource(R.string.rec_pause)) }
                    OutlinedButton(onClick = { Recorder.mark(ctx) }) { Icon(Icons.Filled.BookmarkAdd, null) }
                    Button(onClick = { Recorder.stop(ctx) }, colors = ButtonDefaults.buttonColors(containerColor = RecordRed, contentColor = Color.White)) {
                        Icon(Icons.Filled.Stop, null); Text(" " + stringResource(R.string.rec_stop))
                    }
                }
            }
        }
        else -> Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WarningCard(stringResource(R.string.rec_running_other, s.propertyTitle), MaterialTheme.colorScheme.tertiary)
            OutlinedButton(onClick = { Recorder.stop(ctx) }) { Icon(Icons.Filled.Stop, null); Text(" " + stringResource(R.string.rec_stop)) }
        }
    }
}

/** App-wide bar shown while a recording runs, on every screen. Tap opens the property's recordings. */
@Composable
fun RecordingMiniBar(insetBottom: Boolean = false, onOpen: (Long) -> Unit) {
    val ctx = LocalContext.current
    val st by Recorder.state.collectAsStateWithLifecycle()
    val s = st ?: return
    val elapsed = rememberElapsed(s)
    Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth().let { if (insetBottom) it.navigationBarsPadding() else it }) {
        Row(Modifier.clickable { onOpen(s.propertyId) }.padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            RecDot(s.paused)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(stringResource(R.string.rec_minibar, AudioMath.formatTime(elapsed)), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(s.propertyTitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (s.paused) IconButton(onClick = { Recorder.resume(ctx) }) { Icon(Icons.Filled.PlayArrow, stringResource(R.string.rec_resume)) }
            else IconButton(onClick = { Recorder.pause(ctx) }) { Icon(Icons.Filled.Pause, stringResource(R.string.rec_pause)) }
            IconButton(onClick = { Recorder.mark(ctx) }) { Icon(Icons.Filled.BookmarkAdd, stringResource(R.string.rec_mark)) }
            IconButton(onClick = { Recorder.stop(ctx) }) { Icon(Icons.Filled.Stop, stringResource(R.string.rec_stop), tint = RecordRed) }
        }
    }
}

/** Elapsed real time helper for previews/tests. */
fun nowElapsed() = SystemClock.elapsedRealtime()