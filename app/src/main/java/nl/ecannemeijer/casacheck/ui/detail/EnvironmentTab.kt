package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Destination
import nl.ecannemeijer.casacheck.data.db.EnvRating
import nl.ecannemeijer.casacheck.data.db.InspectionMode
import nl.ecannemeijer.casacheck.domain.checklist.FeatureKeys
import nl.ecannemeijer.casacheck.domain.content.ContentLibrary
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.nav.Nav

val MOUNTAIN_CATEGORIES = listOf("SLOPES", "RETAINING", "DRAINAGE", "ACCESS", "GROUNDS")

@Composable
fun EnvironmentTab(vm: DetailVm, nav: Nav) {
    val p = vm.property.collectAsStateWithLifecycle().value ?: return
    val env by vm.env.collectAsStateWithLifecycle()
    val dests by vm.destinations.collectAsStateWithLifecycle()
    val dists by vm.distances.collectAsStateWithLifecycle()
    val features by vm.features.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var addDest by remember { mutableStateOf(false) }
    val origin = if (p.latitude != null && p.longitude != null) "${p.latitude},${p.longitude}" else listOfNotNull(p.address, p.city, p.province, "Italia").joinToString(", ")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { InfoNote(stringResource(R.string.env_note)) }
        if (features[FeatureKeys.HILLSIDE] != "false") item {
            SectionCard(stringResource(R.string.mountain_module)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Landscape, null); Text("  " + stringResource(R.string.mountain_help), style = MaterialTheme.typography.bodyMedium) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MOUNTAIN_CATEGORIES.forEach { cat ->
                        OutlinedButton(onClick = { vm.startSession(InspectionMode.BY_CATEGORY, null, cat) { nav.wizard(vm.pid, InspectionMode.BY_CATEGORY, cat = cat) } }) { Text(stringResource(Labels.category(cat))) }
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.my_destinations)) {
                if (dests.isEmpty()) Text(stringResource(R.string.no_destinations), style = MaterialTheme.typography.bodySmall)
                dests.forEach { d ->
                    val dd = dists.firstOrNull { it.destinationId == d.id }
                    Text(d.name, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DecField(stringResource(R.string.km), dd?.distanceKm, Modifier.weight(1f)) { v -> vm.launch { vm.c.environment.saveDistance(vm.pid, d.id, v, dd?.travelMinutes) } }
                        NumField(stringResource(R.string.minutes), dd?.travelMinutes?.toLong(), Modifier.weight(1f)) { v -> vm.launch { vm.c.environment.saveDistance(vm.pid, d.id, dd?.distanceKm, v?.toInt()) } }
                        OutlinedButton(onClick = { Intents.mapsDirections(ctx, origin, d.query) }) { Icon(Icons.Filled.Directions, stringResource(R.string.route)) }
                    }
                }
                TextButton(onClick = { addDest = true }) { Text(stringResource(R.string.add_destination)) }
            }
        }
        items(ContentLibrary.environment, key = { it.code }) { def ->
            val r = env.firstOrNull { it.itemCode == def.code }
            val rating = r?.rating ?: EnvRating.UNKNOWN
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(def.name), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        def.mapsQuery?.let { q -> OutlinedButton(onClick = { Intents.mapsSearch(ctx, p.latitude, p.longitude, if (p.latitude != null) q else "$q ${p.city ?: ""}") }) { Icon(Icons.Filled.Search, null); Text(" Maps") } }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EnvRating.entries.forEach { e -> FilterChip(rating == e, { vm.launch { vm.c.environment.rate(vm.pid, def.code, e, r?.distanceKm, r?.travelMinutes, r?.notes) } }, { Text(stringResource(Labels.env(e))) }) }
                    }
                    if (def.mapsQuery != null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DecField(stringResource(R.string.km), r?.distanceKm, Modifier.weight(1f)) { v -> vm.launch { vm.c.environment.rate(vm.pid, def.code, rating, v, r?.travelMinutes, r?.notes) } }
                        NumField(stringResource(R.string.minutes), r?.travelMinutes?.toLong(), Modifier.weight(1f)) { v -> vm.launch { vm.c.environment.rate(vm.pid, def.code, rating, r?.distanceKm, v?.toInt(), r?.notes) } }
                    }
                }
            }
        }
    }
    if (addDest) DestinationDialog(onSave = { d -> vm.launch { vm.c.environment.saveDestination(d) } }) { addDest = false }
}

@Composable
fun DestinationDialog(initial: Destination? = null, onSave: (Destination) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var query by remember { mutableStateOf(initial?.query ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.add_destination)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.destination_name)) }, singleLine = true)
            OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.destination_query)) }, singleLine = true)
            Text(stringResource(R.string.destination_help), style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave((initial ?: Destination(name = "", query = "")).copy(name = name.trim(), query = query.trim().ifBlank { name.trim() })); onDismiss() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
