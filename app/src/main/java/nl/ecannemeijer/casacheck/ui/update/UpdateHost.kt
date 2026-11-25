package nl.ecannemeijer.casacheck.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.update.UpdateManager
import nl.ecannemeijer.casacheck.data.update.UpdateState

/** Checks for updates on start/resume (max. every 6 h) and shows the update dialogs. Place once at the app root. */
@Composable
fun UpdateHost(updates: UpdateManager) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { updates.check(manual = false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updates.onResume(); scope.launch { updates.check(manual = false) } }
    val state by updates.state.collectAsState()
    when (val s = state) {
        is UpdateState.Available -> AlertDialog(onDismissRequest = { updates.later(s.info) },
            title = { Text(stringResource(R.string.update_available, s.info.tag.removePrefix("v"))) },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(s.info.notes.ifBlank { stringResource(R.string.update_no_notes) }, style = MaterialTheme.typography.bodyMedium)
                    Text("\n" + stringResource(R.string.update_confirm_note), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { scope.launch { updates.download(s.info) } }) { Text(stringResource(R.string.update_now)) } },
            dismissButton = { TextButton(onClick = { updates.later(s.info) }) { Text(stringResource(R.string.later)) } })
        is UpdateState.Downloading -> AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.update_downloading)) },
            text = {
                Column {
                    if (s.progress == null) LinearProgressIndicator(Modifier.fillMaxWidth()) else LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    s.progress?.let { Text("${(it * 100).toInt()} %") }
                }
            }, confirmButton = {})
        is UpdateState.ReadyToInstall -> {
            LaunchedEffect(s.file) { updates.install(s.file) }
            AlertDialog(onDismissRequest = { updates.dismiss() }, title = { Text(stringResource(R.string.update_ready)) },
                text = { Text(stringResource(R.string.update_confirm_note)) },
                confirmButton = { TextButton(onClick = { updates.install(s.file) }) { Text(stringResource(R.string.update_install)) } },
                dismissButton = { TextButton(onClick = { updates.dismiss() }) { Text(stringResource(R.string.later)) } })
        }
        is UpdateState.Failed -> AlertDialog(onDismissRequest = { updates.dismiss() }, title = { Text(stringResource(R.string.update_title)) },
            text = { Text(stringResource(when (s.reason) {
                UpdateState.Reason.NETWORK -> R.string.update_err_network
                UpdateState.Reason.VERIFY -> R.string.update_err_verify
                UpdateState.Reason.NEEDS_PERMISSION -> R.string.update_err_permission
            })) },
            confirmButton = {
                if (s.reason == UpdateState.Reason.NEEDS_PERMISSION) TextButton(onClick = { updates.openUnknownSourcesSettings(); updates.dismiss() }) { Text(stringResource(R.string.update_open_settings)) }
                else TextButton(onClick = { updates.dismiss() }) { Text(stringResource(R.string.close)) }
            })
        UpdateState.UpToDate -> AlertDialog(onDismissRequest = { updates.dismiss() }, title = { Text(stringResource(R.string.update_title)) },
            text = { Text(stringResource(R.string.update_up_to_date)) },
            confirmButton = { TextButton(onClick = { updates.dismiss() }) { Text(stringResource(R.string.close)) } })
        else -> {}
    }
}