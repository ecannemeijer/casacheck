package nl.ecannemeijer.casacheck.ui.detail

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Floor
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.EmptyState
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.nav.Nav

@Composable
fun RoomsTab(vm: DetailVm, nav: Nav) {
    val floors by vm.floors.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val defects by vm.defects.collectAsStateWithLifecycle()
    var floorDialog by remember { mutableStateOf<Floor?>(null) }
    var deleteFloor by remember { mutableStateOf<Floor?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { nav.room(vm.pid) }) { Icon(Icons.Filled.Add, null); Text(" " + stringResource(R.string.add_room)) }
                OutlinedButton(onClick = { floorDialog = Floor(propertyId = vm.pid, name = "", level = floors.size) }) { Text(stringResource(R.string.add_floor)) }
            }
        }
        if (rooms.isEmpty()) item { EmptyState(Icons.Filled.MeetingRoom, stringResource(R.string.no_rooms_yet), stringResource(R.string.rooms_help)) }
        val groups = floors.map { it to rooms.filter { r -> r.floorId == it.id } } + listOf(null to rooms.filter { r -> r.floorId == null || floors.none { it.id == r.floorId } })
        groups.forEach { (floor, list) ->
            if (floor == null && list.isEmpty()) return@forEach
            item(key = "f" + (floor?.id ?: -1)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(floor?.name ?: stringResource(R.string.no_floor), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).clickable(enabled = floor != null) { floorDialog = floor })
                    if (floor != null) IconButton(onClick = { deleteFloor = floor }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                }
            }
            items(list, key = { it.id }) { r ->
                val ri = items.filter { it.roomId == r.id }
                val rd = defects.count { it.roomId == r.id }
                Card(Modifier.fillMaxWidth().clickable { nav.room(vm.pid, r.id) }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium)
                        Text(listOfNotNull(stringResource(Labels.roomType(r.type)), r.area?.let { Fmt.sqm(it) }, r.dimensions).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.items_progress, ri.count { it.effectiveStatus.isChecked }, ri.size) + if (rd > 0) " · " + stringResource(R.string.n_defects, rd) else "", style = MaterialTheme.typography.bodySmall)
                        r.repairCost?.let { Text(stringResource(R.string.cost_estimate) + ": " + Fmt.euro(it), style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { vm.startSession(InspectionMode.BY_ROOM, r.id, null) { nav.wizard(vm.pid, InspectionMode.BY_ROOM, r.id) } }) { Text(stringResource(R.string.inspect_room)) }
                    }
                }
            }
        }
    }
    floorDialog?.let { f ->
        var name by remember(f) { mutableStateOf(f.name) }
        var level by remember(f) { mutableStateOf(f.level.toString()) }
        AlertDialog(onDismissRequest = { floorDialog = null }, title = { Text(stringResource(if (f.id == 0L) R.string.add_floor else R.string.edit_floor)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.floor_name_hint)) }, singleLine = true)
                    OutlinedTextField(level, { level = it }, label = { Text(stringResource(R.string.floor_level)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
                val nf = f.copy(name = name.trim(), level = level.trim().toIntOrNull() ?: 0)
                vm.launch { if (nf.id == 0L) vm.c.rooms.addFloor(nf) else vm.c.rooms.updateFloor(nf) }
                floorDialog = null
            }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { floorDialog = null }) { Text(stringResource(R.string.cancel)) } })
    }
    deleteFloor?.let { f ->
        ConfirmDialog(stringResource(R.string.delete_floor), stringResource(R.string.delete_floor_text, f.name), stringResource(R.string.delete),
            onConfirm = { vm.launch { vm.c.rooms.deleteFloor(f) } }, onDismiss = { deleteFloor = null })
    }
}
