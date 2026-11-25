package nl.ecannemeijer.casacheck.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.data.db.Room
import nl.ecannemeijer.casacheck.data.db.RoomType
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.Dropdown
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.nav.CameraTarget
import nl.ecannemeijer.casacheck.ui.nav.Nav

@Composable
fun RoomEditScreen(c: AppContainer, nav: Nav, pid: Long, id: Long?) {
    var roomId by remember { mutableStateOf(id) }
    val room by remember(roomId) { roomId?.let { c.rooms.observeRoom(it) } ?: flowOf(null) }.collectAsState(null)
    val floors by c.rooms.observeFloors(pid).collectAsStateWithLifecycle(emptyList())
    val photos by remember(roomId) { roomId?.let { c.photos.observeForRoom(it) } ?: flowOf(emptyList()) }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        val rid = roomId ?: return@rememberLauncherForActivityResult
        scope.launch { uris.forEach { runCatching { c.photos.addFromUri(pid, it, PhotoKind.INSPECTION, roomId = rid) } } }
    }
    fun save(t: (Room) -> Room) { val r = room ?: return; scope.launch { c.rooms.updateRoom(t(r)) } }

    Scaffold(topBar = { BackTopBar(room?.name ?: stringResource(R.string.add_room), nav::back) {
        if (room != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
    } }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val r = room
            if (roomId == null) {
                var name by remember { mutableStateOf("") }
                var type by remember { mutableStateOf(RoomType.LIVING) }
                var floorId by remember { mutableStateOf<Long?>(null) }
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.room_name)) }, singleLine = true)
                Dropdown(stringResource(R.string.room_type), RoomType.entries, type, { stringResource(Labels.roomType(it)) }) { type = it; if (name.isBlank()) name = "" }
                Dropdown(stringResource(R.string.floor), listOf<Long?>(null) + floors.map { it.id }, floorId, { fid -> floors.firstOrNull { it.id == fid }?.name ?: stringResource(R.string.no_floor) }) { floorId = it }
                val defaultName = stringResource(Labels.roomType(type))
                Button(onClick = { scope.launch { roomId = c.rooms.addRoom(Room(propertyId = pid, name = name.trim().ifBlank { defaultName }, type = type, floorId = floorId)) } },
                    Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.create_room)) }
                Text(stringResource(R.string.room_create_help))
                return@Column
            }
            if (r == null) return@Column
            SectionCard {
                AutoSaveField(stringResource(R.string.room_name), r.name, { v -> if (v.isNotBlank()) save { it.copy(name = v.trim()) } })
                Dropdown(stringResource(R.string.room_type), RoomType.entries, r.type, { stringResource(Labels.roomType(it)) }) { t -> save { it.copy(type = t) } }
                Dropdown(stringResource(R.string.floor), listOf<Long?>(null) + floors.map { it.id }, r.floorId, { fid -> floors.firstOrNull { it.id == fid }?.name ?: stringResource(R.string.no_floor) }) { f -> save { it.copy(floorId = f) } }
                AutoSaveField(stringResource(R.string.room_area), Fmt.num(r.area), { v -> save { it.copy(area = Fmt.parseDouble(v)) } }, keyboardType = KeyboardType.Decimal)
                AutoSaveField(stringResource(R.string.room_dimensions), r.dimensions, { v -> save { it.copy(dimensions = v.ifBlank { null }) } })
            }
            SectionCard(stringResource(R.string.room_condition)) {
                AutoSaveField(stringResource(R.string.room_general), r.generalCondition, { v -> save { it.copy(generalCondition = v.ifBlank { null }) } }, singleLine = false)
                AutoSaveField(stringResource(R.string.f_heating), r.heating, { v -> save { it.copy(heating = v.ifBlank { null }) } })
                AutoSaveField(stringResource(R.string.room_windows), r.windows, { v -> save { it.copy(windows = v.ifBlank { null }) } })
                AutoSaveField(stringResource(R.string.room_electricity), r.electricity, { v -> save { it.copy(electricity = v.ifBlank { null }) } })
                AutoSaveField(stringResource(R.string.room_moisture), r.moisture, { v -> save { it.copy(moisture = v.ifBlank { null }) } })
                AutoSaveField(stringResource(R.string.room_cracks), r.cracks, { v -> save { it.copy(cracks = v.ifBlank { null }) } })
                AutoSaveField(stringResource(R.string.room_flooring), r.flooring, { v -> save { it.copy(flooring = v.ifBlank { null }) } })
                AutoSaveField(stringResource(R.string.room_ceilings), r.ceilings, { v -> save { it.copy(ceilings = v.ifBlank { null }) } })
                AutoSaveField(stringResource(R.string.room_repair_cost), r.repairCost?.toString(), { v -> save { it.copy(repairCost = Fmt.parseLong(v)) } }, keyboardType = KeyboardType.Number)
                AutoSaveField(stringResource(R.string.notes), r.notes, { v -> save { it.copy(notes = v.ifBlank { null }) } }, singleLine = false, minLines = 3)
            }
            SectionCard(stringResource(R.string.tab_photos)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { nav.camera(CameraTarget(pid, roomId = r.id)) }) { Icon(Icons.Filled.CameraAlt, null); Text(" " + stringResource(R.string.take_photo)) }
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Filled.AddPhotoAlternate, null) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    photos.forEach { ph -> nl.ecannemeijer.casacheck.ui.photos.DeletablePhotoThumb(c, ph, Modifier.size(96.dp)) { nav.photo(pid, ph.id) } }
                }
            }
            Button(onClick = { scope.launch { c.inspections.startSession(pid, InspectionMode.BY_ROOM, r.id, null); nav.wizard(pid, InspectionMode.BY_ROOM, r.id) } },
                Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.inspect_room)) }
        }
    }
    if (confirmDelete) room?.let { r ->
        ConfirmDialog(stringResource(R.string.delete_room), stringResource(R.string.delete_room_text, r.name), stringResource(R.string.delete),
            onConfirm = { c.scope.launch { c.rooms.deleteRoom(r) }; nav.back() }, onDismiss = { confirmDelete = false })
    }
}
