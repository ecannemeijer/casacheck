package nl.ecannemeijer.casacheck.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.DocumentStatus
import nl.ecannemeijer.casacheck.domain.content.ContentLibrary
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.Dropdown
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.WarningCard
import nl.ecannemeijer.casacheck.ui.common.rememberSpeaker
import nl.ecannemeijer.casacheck.ui.nav.Nav

val CATASTO_CODES = setOf("VISURA", "PLANIMETRIA", "CONF_CATASTALE", "CONF_URBANISTICA")

fun copyText(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("CasaCheck", text))
    Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
}

fun displayName(ctx: Context, uri: Uri): String =
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        ?: uri.lastPathSegment ?: "document"

@Composable
fun DocumentsTab(vm: DetailVm, nav: Nav) {
    val docs by vm.documents.collectAsStateWithLifecycle()
    val files by vm.docFiles.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val speaker = rememberSpeaker()
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var attachFor by rememberSaveable { mutableStateOf<String?>(null) }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val code = attachFor ?: return@rememberLauncherForActivityResult
        vm.launch {
            uris.forEach { u ->
                val mime = ctx.contentResolver.getType(u) ?: "application/octet-stream"
                runCatching { vm.c.documents.addFile(vm.pid, code, u, displayName(ctx, u), mime) }
                    .onFailure { e -> android.os.Handler(android.os.Looper.getMainLooper()).post { Toast.makeText(ctx, e.message ?: "", Toast.LENGTH_LONG).show() } }
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { WarningCard(stringResource(R.string.catasto_vs_urbanistica), MaterialTheme.colorScheme.tertiary) }
        items(ContentLibrary.documents, key = { it.code }) { def ->
            val doc = docs.firstOrNull { it.typeCode == def.code }
            val status = doc?.status ?: DocumentStatus.NOT_REQUESTED
            val myFiles = files.filter { f -> doc != null && f.documentId == doc.id }
            val open = expanded == def.code
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { expanded = if (open) null else def.code }, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(def.italianName, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(def.name), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(stringResource(Labels.docStatus(status)) + if (myFiles.isNotEmpty()) " · ${myFiles.size} 📎" else "", style = MaterialTheme.typography.labelMedium,
                            color = when (status) { DocumentStatus.CHECKED, DocumentStatus.RECEIVED -> MaterialTheme.colorScheme.primary; DocumentStatus.DEVIATION, DocumentStatus.MISSING -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurfaceVariant })
                        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    }
                    if (open) {
                        Dropdown(stringResource(R.string.status), DocumentStatus.entries, status, { stringResource(Labels.docStatus(it)) }) { s -> vm.launch { vm.c.documents.setStatus(vm.pid, def.code, s) } }
                        Text(stringResource(R.string.doc_why), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.why))
                        Text(stringResource(R.string.doc_check), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.check))
                        Text(stringResource(R.string.doc_who), style = MaterialTheme.typography.titleSmall); Text(stringResource(def.who))
                        if (def.code in CATASTO_CODES) WarningCard(stringResource(R.string.catasto_vs_urbanistica))
                        Text(stringResource(R.string.ask_in_italian), style = MaterialTheme.typography.titleSmall)
                        Text(def.questionIt, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(def.question), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                        Row {
                            IconButton(onClick = { if (!speaker.speak(def.questionIt)) Toast.makeText(ctx, R.string.tts_unavailable, Toast.LENGTH_LONG).show() }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.speak)) }
                            IconButton(onClick = { copyText(ctx, def.questionIt) }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy)) }
                        }
                        AutoSaveField(stringResource(R.string.notes), doc?.notes, { v -> vm.launch { vm.c.documents.setNotes(vm.pid, def.code, v.ifBlank { null }) } }, singleLine = false)
                        myFiles.forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Description, null)
                                Text(f.displayName, Modifier.weight(1f).padding(start = 6.dp).clickable { CasaFiles.file(f.filePath)?.let { Intents.open(ctx, it, f.mimeType) } })
                                IconButton(onClick = { vm.launch { vm.c.documents.deleteFile(f) } }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                            }
                        }
                        OutlinedButton(onClick = { attachFor = def.code; opener.launch(arrayOf("application/pdf", "image/*")) }) { Icon(Icons.Filled.AttachFile, null); Text(" " + stringResource(R.string.attach_file)) }
                    }
                }
            }
        }
    }
}
