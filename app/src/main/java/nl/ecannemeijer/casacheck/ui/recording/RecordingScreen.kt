@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package nl.ecannemeijer.casacheck.ui.recording

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.audio.AudioMath
import nl.ecannemeijer.casacheck.data.db.RecStatus
import nl.ecannemeijer.casacheck.data.db.Recording
import nl.ecannemeijer.casacheck.data.db.RecordingMarker
import nl.ecannemeijer.casacheck.data.db.TranscriptSegment
import nl.ecannemeijer.casacheck.data.db.TxStatus
import nl.ecannemeijer.casacheck.data.transcribe.TranscribeWorker
import nl.ecannemeijer.casacheck.data.translate.TranslateResult
import nl.ecannemeijer.casacheck.data.translate.TranslateStage
import nl.ecannemeijer.casacheck.data.translate.TranslationRepository
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.BaseVm
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.TranslateBusy
import nl.ecannemeijer.casacheck.ui.common.TranslateProblem
import nl.ecannemeijer.casacheck.ui.common.WarningCard
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.Nav

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingVm(val c: AppContainer, val id: Long) : BaseVm() {
    val rec = c.recordings.observeOne(id).state(null)
    val markers = c.recordings.observeMarkers(id).state(emptyList())
    val segments = c.recordings.observeSegments(id).state(emptyList())
    /** Cached translations of this transcript: target language -> lines. */
    val translations = c.recordings.observeOne(id).flatMapLatest { r ->
        if (r == null) flowOf(emptyMap()) else c.translations.observeTranscript(r.propertyId, id).map { l -> l.associate { it.targetLang to it.text } }
    }.state(emptyMap())
    val work = WorkManager.getInstance(c.appContext).getWorkInfosForUniqueWorkFlow(TranscribeWorker.uniqueName(id)).map { it.firstOrNull() }.state(null)
}

private val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f)
private val TX_LANGS = listOf("auto" to R.string.tx_lang_auto, "it" to R.string.tx_lang_it, "nl" to R.string.tx_lang_nl, "en" to R.string.tx_lang_en)

@Composable
fun RecordingScreen(c: AppContainer, nav: Nav, id: Long) {
    val vm = vm("rec$id") { RecordingVm(c, id) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val r by vm.rec.collectAsStateWithLifecycle()
    val markers by vm.markers.collectAsStateWithLifecycle()
    val segments by vm.segments.collectAsStateWithLifecycle()
    val translations by vm.translations.collectAsStateWithLifecycle()
    val work by vm.work.collectAsStateWithLifecycle()
    val ps by c.player.state.collectAsStateWithLifecycle()
    var rename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf("orig") }
    var trStage by remember { mutableStateOf<TranslateStage?>(null) }
    var trProblem by remember { mutableStateOf<TranslateResult?>(null) }
    var txLang by remember { mutableStateOf("auto") }
    val list = rememberLazyListState()

    val rr = r
    LaunchedEffect(rr?.filePath, rr?.status) { if (rr != null && rr.status == RecStatus.DONE) c.player.load(rr.id, c.recordings.fileOf(rr)) }

    if (rename && rr != null) RenameDialog(rr.name, onDismiss = { rename = false }) { n -> rename = false; scope.launch { c.recordings.rename(id, n) } }
    if (confirmDelete && rr != null) ConfirmDialog(stringResource(R.string.rec_delete_title), stringResource(R.string.rec_delete_text, rr.name), stringResource(R.string.delete),
        onConfirm = { confirmDelete = false; deleteRecording(c, ctx, rr); nav.back() }, onDismiss = { confirmDelete = false })

    Scaffold(topBar = {
        BackTopBar(rr?.name ?: "", nav::back) {
            if (rr != null) {
                IconButton(onClick = { rename = true }) { Icon(Icons.Filled.Edit, stringResource(R.string.rec_rename)) }
                IconButton(onClick = { shareRecording(c, ctx, rr) }) { Icon(Icons.Filled.Share, stringResource(R.string.share)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
            }
        }
    }) { pad ->
        if (rr == null) return@Scaffold
        val mine = ps.recordingId == id
        val pos = if (mine) ps.positionMs else 0L
        val dur = if (mine && ps.durationMs > 0) ps.durationMs else rr.durationMs
        val active = AudioMath.activeSegment(segments.map { it.startMs }, pos)
        fun seekPlay(ms: Long) { c.player.seekTo(ms); if (!c.player.state.value.playing) c.player.play() }

        LazyColumn(Modifier.fillMaxSize().padding(pad), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard {
                    if (mine && ps.error) WarningCard(stringResource(R.string.rec_cannot_play))
                    PlayerControls(pos, dur, playing = mine && ps.playing, speed = ps.speed, markers = markers,
                        onSeek = { c.player.seekTo(it) }, onToggle = { c.player.toggle() }, onSkip = { c.player.skip(it) }, onSpeed = { c.player.setSpeed(it) })
                    Text(listOfNotNull(AudioMath.formatSize(rr.sizeBytes), rr.mime).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                SectionCard(stringResource(R.string.rec_markers)) {
                    if (markers.isEmpty()) Text(stringResource(R.string.rec_markers_empty), style = MaterialTheme.typography.bodySmall)
                    markers.forEachIndexed { i, m -> MarkerRow(i, m, onClick = { seekPlay(m.positionMs) }, onDelete = { scope.launch { c.recordings.deleteMarker(m.id) } }) }
                    OutlinedButton(onClick = { scope.launch { c.recordings.addMarker(id, pos, null, null) } }) {
                        Icon(Icons.Filled.BookmarkAdd, null); Text(" " + stringResource(R.string.rec_mark_here, AudioMath.formatTime(pos)))
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.notes)) {
                    AutoSaveField(stringResource(R.string.rec_notes_hint), rr.notes, { scope.launch { c.recordings.setNotes(id, it) } }, singleLine = false, minLines = 3)
                }
            }
            item {
                SectionCard(stringResource(R.string.tx_title)) {
                    val running = work?.state == WorkInfo.State.RUNNING || work?.state == WorkInfo.State.ENQUEUED || rr.transcriptStatus == TxStatus.RUNNING
                    when {
                        running -> {
                            val stage = work?.progress?.getString(TranscribeWorker.KEY_STAGE)
                            val p = work?.progress?.getFloat(TranscribeWorker.KEY_PROGRESS, 0f) ?: 0f
                            Text(stringResource(if (stage == TranscribeWorker.STAGE_DOWNLOAD) R.string.tx_downloading_model else R.string.tx_transcribing, (p * 100).toInt()))
                            LinearProgressIndicator(progress = { p }, Modifier.fillMaxWidth())
                            Text(stringResource(R.string.tx_takes_a_while), style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { TranscribeWorker.cancel(ctx, id) }) { Icon(Icons.Filled.Close, null); Text(" " + stringResource(R.string.tx_cancel)) }
                        }
                        rr.transcriptStatus == TxStatus.DONE -> TranscriptHeader(rr, segments, view, translations,
                            onView = { v ->
                                view = v; trProblem = null
                                if (v != "orig" && translations[v] == null && segments.isNotEmpty()) scope.launch {
                                    try {
                                        val (res, _) = c.translations.translateTranscript(rr.propertyId, id, segments.map { it.text }, v, rr.transcriptLang) { trStage = it }
                                        if (res !is TranslateResult.Ok) { trProblem = res; if (res is TranslateResult.AlreadyTarget) view = "orig" }
                                    } finally { trStage = null }
                                }
                            },
                            onCopy = { copyText(ctx, transcriptText(segments, linesFor(view, translations, segments.size))) },
                            onShare = { Intents.shareText(ctx, rr.name + "\n\n" + transcriptText(segments, linesFor(view, translations, segments.size))) },
                            onRedo = { scope.launch { c.recordings.clearTranscript(id) } })
                        else -> {
                            if (rr.transcriptStatus == TxStatus.FAILED) WarningCard(if (rr.transcriptError == TranscribeWorker.ERR_WIFI) stringResource(R.string.tx_needs_wifi) else stringResource(R.string.tx_failed, rr.transcriptError ?: "?"))
                            Text(stringResource(R.string.tx_intro), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.tx_language), style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TX_LANGS.forEach { (code, label) -> FilterChip(selected = txLang == code, onClick = { txLang = code }, label = { Text(stringResource(label)) }) }
                            }
                            Button(onClick = { TranscribeWorker.enqueue(ctx, id, txLang) }, enabled = rr.status == RecStatus.DONE) {
                                Icon(Icons.Filled.Subtitles, null); Text(" " + stringResource(R.string.tx_start))
                            }
                        }
                    }
                    trStage?.let { TranslateBusy(it) }
                    trProblem?.let { TranslateProblem(it) }
                }
            }
            if (rr.transcriptStatus == TxStatus.DONE) {
                val lines = linesFor(view, translations, segments.size)
                if (segments.isEmpty()) item { InfoNote(stringResource(R.string.tx_empty)) }
                itemsIndexed(segments, key = { _, s -> s.id }) { i, s ->
                    SegmentRow(s, lines?.getOrNull(i), active = i == active && mine, onClick = { seekPlay(s.startMs) })
                }
                if (view != "orig" && lines != null) item { Text(stringResource(R.string.translate_note), style = MaterialTheme.typography.bodySmall) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun linesFor(view: String, translations: Map<String, String>, n: Int): List<String>? =
    if (view == "orig") null else translations[view]?.let { TranslationRepository.alignLines(it, n) }

private fun transcriptText(segments: List<TranscriptSegment>, lines: List<String>?): String =
    segments.mapIndexed { i, s -> "[" + AudioMath.formatTime(s.startMs) + "] " + (lines?.getOrNull(i)?.takeIf { it.isNotBlank() } ?: s.text) }.joinToString("\n")

private fun copyText(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("transcript", text))
    Toast.makeText(ctx, R.string.tx_copied, Toast.LENGTH_SHORT).show()
}

@Composable
private fun PlayerControls(pos: Long, dur: Long, playing: Boolean, speed: Float, markers: List<RecordingMarker>,
                           onSeek: (Long) -> Unit, onToggle: () -> Unit, onSkip: (Long) -> Unit, onSpeed: (Float) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val max = dur.coerceAtLeast(1).toFloat()
    val tickColor = MaterialTheme.colorScheme.tertiary
    Box(Modifier.fillMaxWidth()) {
        // Marker ticks behind the slider track
        Canvas(Modifier.matchParentSizeCompat().padding(horizontal = 10.dp)) {
            markers.forEach { m ->
                val x = size.width * AudioMath.fraction(m.positionMs, dur)
                drawLine(tickColor, Offset(x, size.height * 0.15f), Offset(x, size.height * 0.85f), strokeWidth = 3.dp.toPx())
            }
        }
        Slider(value = (drag ?: pos.toFloat()).coerceIn(0f, max), onValueChange = { drag = it },
            onValueChangeFinished = { drag?.let { onSeek(it.toLong()) }; drag = null }, valueRange = 0f..max)
    }
    Row(Modifier.fillMaxWidth()) {
        Text(AudioMath.formatTime(drag?.toLong() ?: pos), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        Text(AudioMath.formatTime(dur), style = MaterialTheme.typography.labelMedium)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSkip(-10_000) }) { Icon(Icons.Filled.Replay10, stringResource(R.string.rec_back10)) }
        Spacer(Modifier.width(16.dp))
        FilledIconButton(onClick = onToggle, Modifier.size(64.dp)) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, stringResource(if (playing) R.string.rec_pause else R.string.rec_play), Modifier.size(36.dp))
        }
        Spacer(Modifier.width(16.dp))
        IconButton(onClick = { onSkip(30_000) }) { Icon(Icons.Filled.Forward30, stringResource(R.string.rec_fwd30)) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SPEEDS.forEach { s ->
            FilterChip(selected = speed == s, onClick = { onSpeed(s) }, label = { Text((if (s % 1f == 0f) s.toInt().toString() else s.toString()) + "×") })
        }
    }
}

/** Modifier.matchParentSize() is only available inside BoxScope; this keeps the Canvas the slider's height. */
private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxWidth().height(48.dp)

@Composable
private fun MarkerRow(i: Int, m: RecordingMarker, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text(AudioMath.formatTime(m.positionMs), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
        Text(m.label ?: stringResource(R.string.rec_marker_n, i + 1), Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, stringResource(R.string.delete)) }
    }
}

@Composable
private fun TranscriptHeader(r: Recording, segments: List<TranscriptSegment>, view: String, translations: Map<String, String>,
                             onView: (String) -> Unit, onCopy: () -> Unit, onShare: () -> Unit, onRedo: () -> Unit) {
    val langName = when (r.transcriptLang) { "it" -> stringResource(R.string.tx_lang_it); "nl" -> stringResource(R.string.tx_lang_nl); "en" -> stringResource(R.string.tx_lang_en); null -> "?"; else -> r.transcriptLang }
    Text(stringResource(R.string.tx_done_info, segments.size, langName), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.tx_tap_hint), style = MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = view == "orig", onClick = { onView("orig") }, label = { Text(stringResource(R.string.tx_original)) })
        if (r.transcriptLang != "nl") FilterChip(selected = view == "nl", onClick = { onView("nl") }, label = { Text(stringResource(R.string.tx_to_nl)) })
        if (r.transcriptLang != "en") FilterChip(selected = view == "en", onClick = { onView("en") }, label = { Text(stringResource(R.string.tx_to_en)) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onCopy, enabled = segments.isNotEmpty()) { Icon(Icons.Filled.ContentCopy, null); Text(" " + stringResource(R.string.copy)) }
        TextButton(onClick = onShare, enabled = segments.isNotEmpty()) { Icon(Icons.Filled.Share, null); Text(" " + stringResource(R.string.share)) }
        TextButton(onClick = onRedo) { Icon(Icons.Filled.Replay, null); Text(" " + stringResource(R.string.tx_redo)) }
    }
}

@Composable
private fun SegmentRow(s: TranscriptSegment, translated: String?, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    Row(Modifier.fillMaxWidth().background(bg, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(8.dp)) {
        Text(AudioMath.formatTime(s.startMs), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp))
        Column(Modifier.weight(1f)) {
            Text(translated?.takeIf { it.isNotBlank() } ?: s.text, style = MaterialTheme.typography.bodyMedium)
            if (translated != null && translated.isNotBlank()) Text(s.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    // Whole name selected, so typing replaces it.
    var text by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(current, androidx.compose.ui.text.TextRange(0, current.length))) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.rec_rename)) },
        text = { OutlinedTextField(text, { text = if (it.text.length <= 120) it else it.copy(text = it.text.take(120)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focus)) },
        confirmButton = { TextButton(onClick = { onSave(text.text) }, enabled = text.text.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}