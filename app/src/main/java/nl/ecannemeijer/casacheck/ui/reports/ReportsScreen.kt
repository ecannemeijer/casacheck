package nl.ecannemeijer.casacheck.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Report
import nl.ecannemeijer.casacheck.data.report.PdfReportGenerator
import nl.ecannemeijer.casacheck.data.report.ReportOptions
import nl.ecannemeijer.casacheck.data.report.ReportType
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.EmptyState
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.nav.Nav
import java.text.DateFormat
import java.util.Date

val REPORT_LANGS = listOf("nl", "en", "it")

@Composable
fun ReportsScreen(c: AppContainer, nav: Nav, pid: Long?) {
    Scaffold(topBar = { BackTopBar(stringResource(R.string.reports), nav::back) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) { ReportsPanel(c, nav, pid) }
    }
}

/** Generator + list of generated PDFs. With [pid] null only the list of all reports is shown. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsPanel(c: AppContainer, nav: Nav, pid: Long?, compareIds: List<Long> = emptyList()) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val reports by remember(pid) { c.db.reportDao().observeAll().map { l -> if (pid == null) l else l.filter { it.propertyId == pid } } }.collectAsState(emptyList())
    val types = if (compareIds.size >= 2) listOf(ReportType.COMPARISON) else ReportType.entries.filter { it != ReportType.COMPARISON }
    var type by remember { mutableStateOf(types.first()) }
    var lang by remember { mutableStateOf(java.util.Locale.getDefault().language.takeIf { it in REPORT_LANGS } ?: "nl") }
    var photos by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf(true) }
    var listing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<Report?>(null) }
    LaunchedEffect(Unit) { listing = c.settings.current().listingPhotosInReports }
    val ids = if (compareIds.size >= 2) compareIds else listOfNotNull(pid)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ids.isNotEmpty()) item {
            SectionCard(stringResource(R.string.new_report)) {
                Text(stringResource(R.string.report_type), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t -> FilterChip(type == t, { type = t }, label = { Text(stringResource(PdfReportGenerator.reportTypeName(t))) }) }
                }
                Text(stringResource(R.string.report_language), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    REPORT_LANGS.forEach { l -> FilterChip(lang == l, { lang = l }, label = { Text(stringResource(when (l) { "nl" -> R.string.lang_nl; "en" -> R.string.lang_en; else -> R.string.lang_it })) }) }
                }
                SwitchRow(stringResource(R.string.report_include_photos), photos) { photos = it }
                SwitchRow(stringResource(R.string.report_include_notes), notes) { notes = it }
                SwitchRow(stringResource(R.string.report_include_listing_photos), listing) { listing = it }
                if (listing) Text(stringResource(R.string.listing_photos_copyright), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = !busy, onClick = {
                        busy = true; error = null
                        scope.launch {
                            runCatching { c.reports.generate(ids, ReportOptions(type, lang, photos, notes, listing)) }
                                .onSuccess { f -> Intents.open(ctx, f, "application/pdf") }
                                .onFailure { error = it.message ?: it.javaClass.simpleName }
                            busy = false
                        }
                    }) { Icon(Icons.Filled.PictureAsPdf, null); Text(" " + stringResource(R.string.generate_pdf)) }
                    if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                }
                error?.let { Text(stringResource(R.string.report_failed, it), color = MaterialTheme.colorScheme.error) }
            }
        }
        item { Text(stringResource(R.string.generated_reports), style = MaterialTheme.typography.titleMedium) }
        if (reports.isEmpty()) item { EmptyState(Icons.Filled.PictureAsPdf, stringResource(R.string.no_reports)) }
        items(reports, key = { it.id }) { r ->
            val f = c.files.resolve(r.filePath)
            val typeName = ReportType.entries.firstOrNull { it.name == r.type }?.let { stringResource(PdfReportGenerator.reportTypeName(it)) } ?: r.type
            SectionCard {
                Text(r.title.ifBlank { stringResource(R.string.untitled_property) }, style = MaterialTheme.typography.titleSmall)
                Text("$typeName · ${r.language.uppercase()} · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.createdAt))}", style = MaterialTheme.typography.bodySmall)
                Row {
                    IconButton(enabled = f?.exists() == true, onClick = { f?.let { Intents.open(ctx, it, "application/pdf") } }) { Icon(Icons.Filled.OpenInNew, stringResource(R.string.open)) }
                    IconButton(enabled = f?.exists() == true, onClick = { f?.let { Intents.share(ctx, it, "application/pdf", r.title) } }) { Icon(Icons.Filled.Share, stringResource(R.string.share)) }
                    IconButton(enabled = f?.exists() == true, onClick = { f?.let { Intents.print(ctx, it, "CasaCheck – ${r.title}") } }) { Icon(Icons.Filled.Print, stringResource(R.string.print)) }
                    IconButton(onClick = { toDelete = r }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                }
            }
        }
    }
    toDelete?.let { r ->
        ConfirmDialog(stringResource(R.string.delete_report), r.title, stringResource(R.string.delete), onConfirm = {
            scope.launch { withContext(Dispatchers.IO) { c.files.resolve(r.filePath)?.delete(); c.db.reportDao().delete(r.id) } }
            toDelete = null
        }, onDismiss = { toDelete = null })
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange)
    }
}
