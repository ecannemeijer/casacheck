package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.data.db.ItemWithResult
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.ui.common.StatusBadge
import nl.ecannemeijer.casacheck.ui.common.specialistStyle
import nl.ecannemeijer.casacheck.ui.common.statusStyle
import nl.ecannemeijer.casacheck.ui.nav.Nav
import androidx.compose.ui.platform.LocalContext

enum class ItemFilter(val label: Int) { ALL(R.string.all), OPEN(R.string.status_not_checked), DEFECTS(R.string.filter_defects), SPECIALIST(R.string.specialist_needed), UNSURE(R.string.status_unsure), DONE(R.string.filter_done) }

fun ItemFilter.matches(i: ItemWithResult) = when (this) {
    ItemFilter.ALL -> true; ItemFilter.OPEN -> i.effectiveStatus == CheckStatus.NOT_CHECKED
    ItemFilter.DEFECTS -> i.effectiveStatus.isDefect; ItemFilter.SPECIALIST -> i.specialistNeeded == true
    ItemFilter.UNSURE -> i.effectiveStatus == CheckStatus.UNSURE; ItemFilter.DONE -> i.effectiveStatus.isChecked
}

@Composable
fun InspectionTab(vm: DetailVm, nav: Nav) {
    val items by vm.items.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val roomNames = rooms.associate { it.id to it.name }
    var filter by rememberSaveable { mutableStateOf(ItemFilter.ALL) }
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showStart by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val byCat = items.filter { filter.matches(it) }.groupBy { it.categoryCode }
    val cats = CheckpointLibrary.categories.filter { byCat.containsKey(it.code) }
    val done = items.count { it.effectiveStatus.isChecked }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.items_progress, done, items.size), style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = { if (items.isEmpty()) 0f else done.toFloat() / items.size }, Modifier.fillMaxWidth())
                Button(onClick = { showStart = true }, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Filled.PlayArrow, null); Text(" " + stringResource(R.string.start_inspection)) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ItemFilter.entries.forEach { f -> FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(stringResource(f.label) + " (${items.count { f.matches(it) }})") }) }
                }
            }
        }
        cats.forEach { cat ->
            val list = byCat[cat.code].orEmpty()
            val all = items.filter { it.categoryCode == cat.code }
            val catDone = all.count { it.effectiveStatus.isChecked }
            val open = cat.code in expanded || filter != ItemFilter.ALL
            item(key = "cat_" + cat.code) {
                Row(Modifier.fillMaxWidth().clickable { expanded = if (cat.code in expanded) expanded - cat.code else expanded + cat.code }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(cat.name), style = MaterialTheme.typography.titleSmall)
                        Text("$catDone / ${all.size}" + (all.count { it.effectiveStatus.isDefect }.takeIf { it > 0 }?.let { " · " + ctx.getString(R.string.n_defects, it) } ?: ""),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    androidx.compose.material3.TextButton(onClick = { vm.startSession(InspectionMode.BY_CATEGORY, null, cat.code) { nav.wizard(vm.pid, InspectionMode.BY_CATEGORY, cat = cat.code) } }) { Text(stringResource(R.string.check)) }
                    Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                }
                HorizontalDivider()
            }
            if (open) items(list, key = { it.id }) { i -> ItemRow(i, vm.c.inspections.titleOf(i), i.roomId?.let { roomNames[it] }) { nav.item(i.id) } }
        }
    }
    if (showStart) StartInspectionDialog(vm, nav) { showStart = false }
}

@Composable
fun ItemRow(i: ItemWithResult, title: String, room: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBadge(statusStyle(i.effectiveStatus), compact = true)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            val sub = listOfNotNull(room, i.notes?.take(60)).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        if (i.specialistNeeded == true) StatusBadge(specialistStyle, compact = true)
        if (i.photoCount > 0) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.PhotoCamera, null); Text("${i.photoCount}", style = MaterialTheme.typography.labelSmall) }
    }
}
