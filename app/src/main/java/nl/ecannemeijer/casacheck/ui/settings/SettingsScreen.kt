package nl.ecannemeijer.casacheck.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.BuildConfig
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.backup.BackupException
import nl.ecannemeijer.casacheck.data.backup.BackupInfo
import nl.ecannemeijer.casacheck.data.backup.DuplicateMode
import nl.ecannemeijer.casacheck.data.backup.RestoreReport
import nl.ecannemeijer.casacheck.data.db.Destination
import nl.ecannemeijer.casacheck.data.settings.ThemeMode
import nl.ecannemeijer.casacheck.domain.score.ScoreGroup
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.detail.DestinationDialog
import nl.ecannemeijer.casacheck.ui.nav.Nav
import nl.ecannemeijer.casacheck.ui.reports.SwitchRow
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface BackupUi {
    data object Idle : BackupUi
    data object Working : BackupUi
    data class Exported(val count: Int) : BackupUi
    data class Inspected(val info: BackupInfo, val dir: File) : BackupUi
    data class Restored(val r: RestoreReport) : BackupUi
    data class Failed(val msg: String) : BackupUi
}

@Composable
fun SettingsScreen(c: AppContainer, nav: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by c.settings.settings.collectAsState(null)
    val dests by remember { c.environment.observeDestinations() }.collectAsState(emptyList())
    var backup by remember { mutableStateOf<BackupUi>(BackupUi.Idle) }
    var editDest by remember { mutableStateOf<Destination?>(null) }
    var newDest by remember { mutableStateOf(false) }
    var delDest by remember { mutableStateOf<Destination?>(null) }
    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').ifBlank { "" }

    fun errText(e: Throwable) = (e as? BackupException)?.let { ctx.getString(when (it.reason) {
        BackupException.Reason.NOT_A_BACKUP -> R.string.backup_err_not_backup
        BackupException.Reason.NEWER_VERSION -> R.string.backup_err_newer
        BackupException.Reason.INTEGRITY -> R.string.backup_err_integrity
        BackupException.Reason.UNSAFE_PATH -> R.string.backup_err_unsafe
        BackupException.Reason.TOO_LARGE -> R.string.backup_err_too_large
        BackupException.Reason.CORRUPT -> R.string.backup_err_corrupt
    }) } ?: (e.message ?: e.javaClass.simpleName)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) { backup = BackupUi.Working; scope.launch {
            backup = runCatching { BackupUi.Exported(c.backup.exportToUri(uri, null, c.settings.current())) }.getOrElse { BackupUi.Failed(errText(it)) }
        } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { backup = BackupUi.Working; scope.launch {
            backup = runCatching {
                val input = ctx.contentResolver.openInputStream(uri) ?: error("cannot open")
                val (info, dir) = input.use { c.backup.inspect(it) }
                BackupUi.Inspected(info, dir)
            }.getOrElse { BackupUi.Failed(errText(it)) }
        } }
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.settings), nav::back) }) { pad ->
        val s = st ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(stringResource(R.string.appearance)) {
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { m -> FilterChip(s.theme == m, { scope.launch { c.settings.update { it.copy(theme = m) } } }, label = {
                        Text(stringResource(when (m) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }))
                    }) }
                }
                Text(stringResource(R.string.app_language), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("" to R.string.theme_system, "nl" to R.string.lang_nl, "en" to R.string.lang_en, "it" to R.string.lang_it).forEach { (tag, label) ->
                        FilterChip(currentLang == tag, { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) },
                            label = { Text(stringResource(label)) })
                    }
                }
            }
            SectionCard(stringResource(R.string.costs_defaults)) {
                AutoSaveField(stringResource(R.string.purchase_cost_percent), Fmt.num(s.purchaseCostPercent), { v -> Fmt.parseDouble(v)?.let { d -> scope.launch { c.settings.update { it.copy(purchaseCostPercent = d.coerceIn(0.0, 50.0)) } } } },
                    keyboardType = KeyboardType.Decimal, supporting = stringResource(R.string.purchase_cost_help))
                AutoSaveField(stringResource(R.string.contingency_percent), Fmt.num(s.contingencyPercent), { v -> Fmt.parseDouble(v)?.let { d -> scope.launch { c.settings.update { it.copy(contingencyPercent = d.coerceIn(0.0, 100.0)) } } } },
                    keyboardType = KeyboardType.Decimal, supporting = stringResource(R.string.contingency_help))
            }
            SectionCard(stringResource(R.string.weights)) {
                Text(stringResource(R.string.weights_help), style = MaterialTheme.typography.bodySmall)
                s.weightMap().forEach { (g, w) ->
                    Text("${stringResource(Labels.group(g))}: $w")
                    Slider(w.toFloat(), { v -> scope.launch { c.settings.update { it.copy(weights = it.weights + (g.name to v.toInt())) } } }, valueRange = 0f..10f, steps = 9)
                }
                TextButton(onClick = { scope.launch { c.settings.update { it.copy(weights = nl.ecannemeijer.casacheck.domain.score.ScoreCalculator.DEFAULT_WEIGHTS.mapKeys { e -> e.key.name }) } } }) { Text(stringResource(R.string.reset_defaults)) }
            }
            SectionCard(stringResource(R.string.moisture_settings)) {
                AutoSaveField(stringResource(R.string.moisture_heights), s.moistureHeights, { v ->
                    val clean = v.split(',', ';', ' ').mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..400 }.distinct().sorted().joinToString(",")
                    if (clean.isNotEmpty()) scope.launch { c.settings.update { it.copy(moistureHeights = clean) } }
                }, supporting = stringResource(R.string.moisture_heights_help))
            }
            SectionCard(stringResource(R.string.reports)) {
                SwitchRow(stringResource(R.string.report_include_listing_photos), s.listingPhotosInReports) { b -> scope.launch { c.settings.update { it.copy(listingPhotosInReports = b) } } }
                Text(stringResource(R.string.listing_photos_copyright), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(stringResource(R.string.translation_settings)) {
                SwitchRow(stringResource(R.string.settings_wifi_models), s.translateWifiOnly) { b -> scope.launch { c.settings.update { it.copy(translateWifiOnly = b) } } }
                Text(stringResource(R.string.translate_wifi_only_help), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(stringResource(R.string.settings_recordings)) {
                val model = remember { nl.ecannemeijer.casacheck.data.transcribe.WhisperModel(c.appContext) }
                var present by remember { mutableStateOf(model.isPresent()) }
                Text(stringResource(R.string.settings_speech_model, stringResource(if (present) R.string.settings_speech_model_present else R.string.settings_speech_model_none)), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.rec_quality_note, nl.ecannemeijer.casacheck.data.audio.AudioMath.formatSize(nl.ecannemeijer.casacheck.data.audio.AudioMath.bytesPerHour()),
                    nl.ecannemeijer.casacheck.data.audio.AudioMath.formatSize(c.recordings.freeBytes())), style = MaterialTheme.typography.bodySmall)
                if (present) androidx.compose.material3.OutlinedButton(onClick = { model.delete(); present = false }) { Text(stringResource(R.string.settings_speech_model_delete)) }
            }
            SectionCard(stringResource(R.string.destinations)) {
                Text(stringResource(R.string.destinations_help), style = MaterialTheme.typography.bodySmall)
                dests.forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(d.name); Text(d.query, style = MaterialTheme.typography.bodySmall) }
                        IconButton(onClick = { editDest = d }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                        IconButton(onClick = { delDest = d }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                    }
                }
                OutlinedButton(onClick = { newDest = true }) { Icon(Icons.Filled.Add, null); Text(stringResource(R.string.add_destination)) }
            }
            SectionCard(stringResource(R.string.backup)) {
                Text(stringResource(R.string.backup_help), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = backup !is BackupUi.Working, onClick = {
                        exportLauncher.launch("casacheck_backup_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date()) + ".zip")
                    }) { Text(stringResource(R.string.backup_export)) }
                    OutlinedButton(enabled = backup !is BackupUi.Working, onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) { Text(stringResource(R.string.backup_restore)) }
                }
                when (val b = backup) {
                    BackupUi.Working -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    is BackupUi.Exported -> Text(stringResource(R.string.backup_done, b.count))
                    is BackupUi.Restored -> Text(stringResource(R.string.restore_done, b.r.added, b.r.merged, b.r.skipped, b.r.photos))
                    is BackupUi.Failed -> Text(stringResource(R.string.backup_failed, b.msg), color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
            }
            SectionCard(stringResource(R.string.about)) {
                Text("CasaCheck ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodySmall)
                val upd by c.updates.state.collectAsState()
                OutlinedButton(enabled = upd !is nl.ecannemeijer.casacheck.data.update.UpdateState.Checking && upd !is nl.ecannemeijer.casacheck.data.update.UpdateState.Downloading, onClick = { scope.launch { c.updates.check(manual = true) } }) { Text(stringResource(R.string.update_check)) }
                Text(stringResource(R.string.update_auto_note), style = MaterialTheme.typography.bodySmall)
                InfoNote(stringResource(R.string.score_not_guarantee))
            }
        }
    }
    (backup as? BackupUi.Inspected)?.let { b ->
        var mode by remember { mutableStateOf(DuplicateMode.SKIP) }
        AlertDialog(onDismissRequest = { backup = BackupUi.Idle }, title = { Text(stringResource(R.string.backup_restore)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.backup_info, DateFormat.getDateTimeInstance().format(Date(b.info.manifest.createdAt)), b.info.manifest.propertyCount, b.info.manifest.appVersion))
                if (b.info.duplicates.isNotEmpty()) {
                    Text(stringResource(R.string.backup_duplicates, b.info.duplicates.size), style = MaterialTheme.typography.labelLarge)
                    b.info.duplicates.take(8).forEach { Text("• " + it.second, style = MaterialTheme.typography.bodySmall) }
                    DuplicateMode.entries.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(mode == m, { mode = m })
                            Text(stringResource(if (m == DuplicateMode.MERGE) R.string.dup_merge else R.string.dup_skip))
                        }
                    }
                }
                Text(stringResource(R.string.restore_note), style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onClick = {
            backup = BackupUi.Working
            scope.launch {
                backup = runCatching { BackupUi.Restored(c.backup.restore(b.info, b.dir, mode) { s -> c.scope.launch { c.settings.update { s } } }) }.getOrElse { BackupUi.Failed(errText(it)) }
            }
        }) { Text(stringResource(R.string.restore)) } }, dismissButton = { TextButton(onClick = { backup = BackupUi.Idle }) { Text(stringResource(R.string.cancel)) } })
    }
    if (newDest || editDest != null) DestinationDialog(editDest, onSave = { d -> scope.launch { c.environment.saveDestination(d) }; newDest = false; editDest = null }) { newDest = false; editDest = null }
    delDest?.let { d -> ConfirmDialog(stringResource(R.string.delete), d.name, stringResource(R.string.delete), onConfirm = { scope.launch { c.environment.deleteDestination(d.id) }; delDest = null }, onDismiss = { delDest = null }) }
}
