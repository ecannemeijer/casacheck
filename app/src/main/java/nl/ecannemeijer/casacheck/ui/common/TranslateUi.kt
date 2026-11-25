package nl.ecannemeijer.casacheck.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.translate.TranslateResult
import nl.ecannemeijer.casacheck.data.translate.TranslateStage

@Composable
fun TranslateBusy(stage: TranslateStage) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
        Text("  " + stringResource(when (stage) {
            TranslateStage.DETECTING -> R.string.translate_detecting
            TranslateStage.DOWNLOADING_MODEL -> R.string.translate_downloading
            TranslateStage.TRANSLATING -> R.string.translate_running
        }), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Shows a friendly message for a failed or skipped translation; nothing for success. */
@Composable
fun TranslateProblem(r: TranslateResult) {
    when (r) {
        TranslateResult.NeedsWifi -> WarningCard(stringResource(R.string.translate_needs_wifi), MaterialTheme.colorScheme.tertiary)
        is TranslateResult.DownloadFailed -> WarningCard(stringResource(R.string.translate_download_failed))
        is TranslateResult.Failed -> WarningCard(stringResource(R.string.translate_failed, r.message ?: "?"))
        TranslateResult.AlreadyTarget -> InfoNote(stringResource(R.string.translate_already))
        is TranslateResult.Ok -> {}
    }
}

@Composable
fun TranslateButton(onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled) { Icon(Icons.Filled.Translate, null); Text(" " + stringResource(R.string.translate_to_nl)) }
}

/** "Translate to Dutch" for free text that is not stored yet (pasted advertisement text, OCR output). Not cached. */
@Composable
fun TranslateTextBlock(c: AppContainer, text: String) {
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf<TranslateStage?>(null) }
    var result by remember(text) { mutableStateOf<TranslateResult?>(null) }
    val st = stage
    if (st != null) TranslateBusy(st)
    else if (result !is TranslateResult.Ok) TranslateButton(enabled = text.isNotBlank(), onClick = {
        scope.launch { try { result = c.translations.translateText(text) { s -> stage = s } } finally { stage = null } }
    })
    when (val r = result) {
        is TranslateResult.Ok -> SectionCard(stringResource(R.string.show_translation)) {
            SelectionContainer { Text(r.text, style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(R.string.translate_note), style = MaterialTheme.typography.bodySmall)
        }
        null -> {}
        else -> TranslateProblem(r)
    }
}