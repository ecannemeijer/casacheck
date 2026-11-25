package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Defect
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.Priority
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.ui.common.EmptyState
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.StatusBadge
import nl.ecannemeijer.casacheck.ui.common.severityStyle
import nl.ecannemeijer.casacheck.ui.common.specialistStyle
import nl.ecannemeijer.casacheck.ui.nav.Nav

enum class DefectFilter(val label: Int, val test: (Defect) -> Boolean) {
    OPEN(R.string.df_open, { it.status != DefectStatus.RESOLVED }),
    SERIOUS(R.string.df_serious, { it.status != DefectStatus.RESOLVED && it.severity == Severity.SERIOUS }),
    MOISTURE(R.string.df_moisture, { it.status != DefectStatus.RESOLVED && it.isMoisture }),
    STRUCTURAL(R.string.df_structural, { it.status != DefectStatus.RESOLVED && it.isStructural }),
    IMMEDIATE(R.string.df_immediate, { it.status != DefectStatus.RESOLVED && it.priority == Priority.IMMEDIATE }),
    SPECIALIST(R.string.df_specialist, { it.status != DefectStatus.RESOLVED && it.specialistNeeded }),
    NO_COST(R.string.df_no_cost, { it.status != DefectStatus.RESOLVED && it.costMin == null && it.costMax == null && it.costOwn == null && it.repairProjectId == null }),
    RESOLVED(R.string.ds_resolved, { it.status == DefectStatus.RESOLVED }),
}

@Composable
fun DefectsTab(vm: DetailVm, nav: Nav) {
    val defects by vm.defects.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(DefectFilter.OPEN) }
    val list = defects.filter(filter.test)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { nav.defect(vm.pid) }) { Icon(Icons.Filled.Add, null); Text(" " + stringResource(R.string.add_defect)) }
                Text(stringResource(R.string.defects_auto_note), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DefectFilter.entries.forEach { f -> FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(stringResource(f.label) + " (${defects.count(f.test)})") }) }
                }
            }
        }
        if (list.isEmpty()) item { EmptyState(Icons.Filled.CheckCircle, stringResource(R.string.no_defects_registered)) }
        items(list, key = { it.id }) { d ->
            Card(Modifier.fillMaxWidth().clickable { nav.defect(vm.pid, d.id) }) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(severityStyle(d.severity))
                        if (d.specialistNeeded) StatusBadge(specialistStyle)
                        Text(stringResource(Labels.defectStatus(d.status)), style = MaterialTheme.typography.labelMedium)
                    }
                    Text(d.title, style = MaterialTheme.typography.titleMedium)
                    val room = rooms.firstOrNull { it.id == d.roomId }?.name
                    Text(listOfNotNull(room, stringResource(Labels.priority(d.priority))).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    val proj = projects.firstOrNull { it.id == d.repairProjectId }
                    when {
                        proj != null -> Text(stringResource(R.string.part_of_project, proj.name), style = MaterialTheme.typography.bodySmall)
                        d.costOwn != null -> Text(stringResource(R.string.cost_estimate) + ": " + Fmt.euro(d.costOwn), style = MaterialTheme.typography.bodySmall)
                        d.costMin != null || d.costMax != null -> Text(stringResource(R.string.cost_estimate) + ": " + Fmt.range(d.costMin ?: d.costMax!!, d.costMax ?: d.costMin!!), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
