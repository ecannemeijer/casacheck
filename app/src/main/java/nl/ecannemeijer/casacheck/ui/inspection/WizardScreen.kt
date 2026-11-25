package nl.ecannemeijer.casacheck.ui.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.EmptyState
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.nav.Nav

/**
 * Step-by-step inspection. The list of checkpoints is fixed when the wizard opens (so items do not jump while statuses change),
 * the current position is stored in the session for "continue last check", and every change is saved immediately.
 */
@Composable
fun WizardScreen(c: AppContainer, nav: Nav, pid: Long, mode: InspectionMode, room: Long?, cat: String?, start: Long?) {
    val all by remember { c.inspections.observeItems(pid) }.collectAsState(null)
    val rooms by remember { c.rooms.observeRooms(pid) }.collectAsState(emptyList())
    var order by rememberSaveable { mutableStateOf<List<Long>?>(null) }
    var index by rememberSaveable { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(all) {
        val items = all ?: return@LaunchedEffect
        if (order == null) {
            val list = c.inspections.filterForMode(items, mode, room, cat)
            order = list.map { it.id }
            index = start?.let { s -> list.indexOfFirst { it.id == s }.takeIf { it >= 0 } } ?: list.indexOfFirst { it.effectiveStatus == CheckStatus.NOT_CHECKED }.coerceAtLeast(0)
        }
    }
    val ids = order
    val current = ids?.getOrNull(index)?.let { id -> all?.firstOrNull { it.id == id } }
    LaunchedEffect(current?.id) { current?.let { c.inspections.setSessionPosition(pid, mode, room, cat, it.id) } }
    val title = stringResource(Labels.mode(mode)) + (room?.let { r -> " – " + (rooms.firstOrNull { it.id == r }?.name ?: "") } ?: "") + (cat?.let { " – " + stringResource(Labels.category(it)) } ?: "")
    val finished = ids != null && (ids.isEmpty() || index >= ids.size)

    Scaffold(topBar = { BackTopBar(title, nav::back) }, bottomBar = {
        if (ids != null && !finished) Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = index > 0, onClick = { index-- }, modifier = Modifier.weight(1f).height(60.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Text(" " + stringResource(R.string.previous)) }
                Button(onClick = { index++ }, modifier = Modifier.weight(1.4f).height(60.dp)) {
                    Text(stringResource(if (current?.effectiveStatus == CheckStatus.NOT_CHECKED) R.string.skip else R.string.next) + " ")
                    Icon(if (current?.effectiveStatus == CheckStatus.NOT_CHECKED) Icons.Filled.SkipNext else Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (ids == null) return@Column
            val doneCount = ids.count { id -> all?.firstOrNull { it.id == id }?.effectiveStatus?.isChecked == true }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.wizard_progress, (index + 1).coerceAtMost(ids.size), ids.size, doneCount), style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { if (ids.isEmpty()) 1f else doneCount.toFloat() / ids.size }, Modifier.fillMaxWidth())
            }
            if (finished) {
                EmptyState(Icons.Filled.CheckCircle, stringResource(if (ids.isEmpty()) R.string.wizard_empty else R.string.wizard_done),
                    stringResource(R.string.wizard_done_text, ids.size - doneCount)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val firstOpen = ids.indexOfFirst { id -> all?.firstOrNull { it.id == id }?.effectiveStatus == CheckStatus.NOT_CHECKED }
                        if (firstOpen >= 0) TextButton(onClick = { index = firstOpen }) { Text(stringResource(R.string.go_to_first_open)) }
                        Button(onClick = { scope.launch { c.inspections.endSession(pid) }; nav.back() }) { Text(stringResource(R.string.finish_inspection)) }
                    }
                }
                return@Column
            }
            current?.let { item -> androidx.compose.runtime.key(item.id) {
                CheckpointPage(c, nav, item, item.roomId?.let { r -> rooms.firstOrNull { it.id == r }?.name },
                    Modifier.verticalScroll(rememberScrollState()).padding(16.dp))
            } }
        }
    }
}

@Composable
fun ItemScreen(c: AppContainer, nav: Nav, itemId: Long) {
    val item by remember { c.inspections.observeItem(itemId) }.collectAsState(null)
    val rooms by remember(item?.propertyId) { c.rooms.observeRooms(item?.propertyId ?: -1) }.collectAsState(emptyList())
    Scaffold(topBar = { BackTopBar(stringResource(R.string.checkpoint), nav::back) }) { pad ->
        item?.let { i ->
            CheckpointPage(c, nav, i, i.roomId?.let { r -> rooms.firstOrNull { it.id == r }?.name }, Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp))
        }
    }
}
