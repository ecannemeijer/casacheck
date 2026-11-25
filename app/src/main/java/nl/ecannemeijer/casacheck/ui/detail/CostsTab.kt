package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CostEstimate
import nl.ecannemeijer.casacheck.data.db.CostType
import nl.ecannemeijer.casacheck.data.db.CostUrgency
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.RepairProject
import nl.ecannemeijer.casacheck.domain.content.COST_CATEGORIES
import nl.ecannemeijer.casacheck.domain.cost.CostCalculator
import nl.ecannemeijer.casacheck.ui.common.Dropdown
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.nav.Nav

@Composable
private fun Line(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else null)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else null)
    }
}

@Composable
fun CostsTab(vm: DetailVm, nav: Nav) {
    val p = vm.property.collectAsStateWithLifecycle().value ?: return
    val analysis by vm.analysis.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val estimates by vm.estimates.collectAsStateWithLifecycle()
    val defects by vm.defects.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var editProject by remember { mutableStateOf<RepairProject?>(null) }
    var editEstimate by remember { mutableStateOf<CostEstimate?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoNote(stringResource(R.string.costs_indicative))
        val cs = analysis?.snapshot?.costs
        if (cs != null) SectionCard(stringResource(R.string.cost_summary)) {
            Line(stringResource(R.string.f_asking_price), Fmt.euro(cs.askingPrice))
            Line(stringResource(R.string.cost_immediate), Fmt.range(cs.immediate.min, cs.immediate.max))
            Line(stringResource(R.string.cost_within_year), Fmt.range(cs.withinYear.min, cs.withinYear.max))
            Line(stringResource(R.string.cost_renovation), Fmt.range(cs.renovation.min, cs.renovation.max), bold = true)
            Line(stringResource(R.string.cost_contingency), Fmt.range(cs.contingency.min, cs.contingency.max))
            Line(stringResource(R.string.cost_purchase), Fmt.euro(cs.purchaseCosts))
            if (cs.totalMin != null && cs.totalMax != null) Line(stringResource(R.string.cost_total), Fmt.range(cs.totalMin, cs.totalMax), bold = true)
            Line(stringResource(R.string.cost_optional), Fmt.range(cs.optional.min, cs.optional.max))
            if (cs.ownOffer != null && cs.totalOwnOfferMin != null && cs.totalOwnOfferMax != null) {
                HorizontalDivider()
                Line(stringResource(R.string.f_own_offer), Fmt.euro(cs.ownOffer))
                Line(stringResource(R.string.cost_total_own_offer), Fmt.range(cs.totalOwnOfferMin, cs.totalOwnOfferMax), bold = true)
            }
            if (cs.defectsWithoutCost > 0) Text(stringResource(R.string.defects_without_cost, cs.defectsWithoutCost), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        }
        SectionCard(stringResource(R.string.cost_percentages)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecField(stringResource(R.string.purchase_cost_pct) + " (" + Fmt.num(settings?.purchaseCostPercent) + ")", p.purchaseCostPercent, Modifier.weight(1f)) { v -> vm.update("purchaseCostPercent") { it.copy(purchaseCostPercent = v?.takeIf { x -> x in 0.0..50.0 }) } }
                DecField(stringResource(R.string.contingency_pct) + " (" + Fmt.num(settings?.contingencyPercent) + ")", p.contingencyPercent, Modifier.weight(1f)) { v -> vm.update("contingencyPercent") { it.copy(contingencyPercent = v?.takeIf { x -> x in 0.0..100.0 }) } }
            }
            NumField(stringResource(R.string.f_own_offer), p.ownOffer, Modifier.fillMaxWidth()) { v -> vm.update("ownOffer") { it.copy(ownOffer = v) } }
            Text(stringResource(R.string.pct_override_help), style = MaterialTheme.typography.bodySmall)
        }
        if (cs != null && cs.byCategory.isNotEmpty()) SectionCard(stringResource(R.string.by_category)) {
            cs.byCategory.entries.sortedByDescending { it.value.max }.forEach { (k, r) -> Line(stringResource(Labels.costCat(k)), Fmt.range(r.min, r.max)) }
        }
        if (cs != null) SectionCard(stringResource(R.string.cost_lines)) {
            if (cs.lines.isEmpty()) Text(stringResource(R.string.no_cost_estimate))
            cs.lines.forEach { l ->
                Column(Modifier.fillMaxWidth().clickable {
                    when (l.source) { "defect" -> nav.defect(vm.pid, l.id); "project" -> editProject = projects.firstOrNull { it.id == l.id }; else -> editEstimate = estimates.firstOrNull { it.id == l.id } }
                }.padding(vertical = 4.dp)) {
                    Line(l.title, if (l.hasAmount) Fmt.range(l.range.min, l.range.max) else "–")
                    Text(stringResource(Labels.costCat(l.category)) + " · " + stringResource(Labels.urgency(l.urgency)) + " · " + stringResource(when (l.source) { "defect" -> R.string.defect; "project" -> R.string.repair_project; else -> R.string.estimate }),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        SectionCard(stringResource(R.string.repair_projects)) {
            Text(stringResource(R.string.projects_help), style = MaterialTheme.typography.bodySmall)
            projects.forEach { pr ->
                val linked = defects.filter { it.repairProjectId == pr.id }
                val r = CostCalculator.projectRange(pr, estimates.filter { it.repairProjectId == pr.id }, linked)
                Column(Modifier.fillMaxWidth().clickable { editProject = pr }.padding(vertical = 4.dp)) {
                    Line(pr.name, r?.let { Fmt.range(it.min, it.max) } ?: "–", bold = true)
                    Text(stringResource(R.string.n_linked_defects, linked.size) + " · " + stringResource(Labels.urgency(pr.urgency)), style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = { editProject = RepairProject(propertyId = vm.pid, name = "", costCategory = "OTHER") }) { Icon(Icons.Filled.Add, null); Text(" " + stringResource(R.string.add_project)) }
        }
        SectionCard(stringResource(R.string.estimates)) {
            estimates.forEach { e ->
                val r = CostCalculator.estimateRange(e)
                Column(Modifier.fillMaxWidth().clickable { editEstimate = e }.padding(vertical = 4.dp)) {
                    Line(e.description, r?.let { Fmt.range(it.min, it.max) } ?: "–")
                    Text(stringResource(Labels.costCat(e.costCategory)) + (e.repairProjectId?.let { pid -> " · " + (projects.firstOrNull { it.id == pid }?.name ?: "") } ?: ""), style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = { editEstimate = CostEstimate(propertyId = vm.pid, costCategory = "OTHER", description = "") }) { Icon(Icons.Filled.Add, null); Text(" " + stringResource(R.string.add_estimate)) }
        }
    }
    editProject?.let { ProjectDialog(vm, it) { editProject = null } }
    editEstimate?.let { EstimateDialog(vm, it) { editEstimate = null } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectDialog(vm: DetailVm, initial: RepairProject, onDismiss: () -> Unit) {
    var p by remember { mutableStateOf(initial) }
    val defects by vm.defects.collectAsStateWithLifecycle()
    var linked by remember { mutableStateOf(defects.filter { it.repairProjectId == initial.id && initial.id != 0L }.map { it.id }.toSet()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (initial.id == 0L) R.string.add_project else R.string.repair_project)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(p.name, { p = p.copy(name = it) }, label = { Text(stringResource(R.string.project_name)) }, singleLine = true)
            Dropdown(stringResource(R.string.category), COST_CATEGORIES, p.costCategory, { stringResource(Labels.costCat(it)) }) { p = p.copy(costCategory = it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { CostUrgency.entries.forEach { u -> FilterChip(p.urgency == u, { p = p.copy(urgency = u) }, { Text(stringResource(Labels.urgency(u))) }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumField(stringResource(R.string.cost_min), p.costMin, Modifier.weight(1f)) { p = p.copy(costMin = it) }
                NumField(stringResource(R.string.cost_max), p.costMax, Modifier.weight(1f)) { p = p.copy(costMax = it) }
            }
            NumField(stringResource(R.string.cost_own), p.costOwn) { p = p.copy(costOwn = it) }
            NumField(stringResource(R.string.quote_amount), p.quoteAmount) { p = p.copy(quoteAmount = it) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(p.quoteReceived, { p = p.copy(quoteReceived = it) }); Text(stringResource(R.string.quote_received)) }
            OutlinedTextField(p.notes ?: "", { p = p.copy(notes = it.ifBlank { null }) }, label = { Text(stringResource(R.string.notes)) })
            Text(stringResource(R.string.link_defects), style = MaterialTheme.typography.titleSmall)
            defects.filter { it.status != DefectStatus.RESOLVED }.forEach { d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(d.id in linked, { linked = if (it) linked + d.id else linked - d.id })
                    Text(d.title, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }, confirmButton = {
        TextButton(enabled = p.name.isNotBlank(), onClick = {
            vm.launch {
                val id = vm.c.defects.saveProject(p)
                val before = defects.filter { it.repairProjectId == id }.map { it.id }.toSet()
                vm.c.defects.linkToProject((before - linked).toList(), null)
                vm.c.defects.linkToProject(linked.toList(), id)
            }
            onDismiss()
        }) { Text(stringResource(R.string.save)) }
    }, dismissButton = {
        Row {
            if (initial.id != 0L) TextButton(onClick = { vm.launch { vm.c.defects.deleteProject(initial.id) }; onDismiss() }) { Text(stringResource(R.string.delete)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EstimateDialog(vm: DetailVm, initial: CostEstimate, onDismiss: () -> Unit) {
    var e by remember { mutableStateOf(initial) }
    val projects by vm.projects.collectAsStateWithLifecycle()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (initial.id == 0L) R.string.add_estimate else R.string.estimate)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(e.description, { e = e.copy(description = it) }, label = { Text(stringResource(R.string.f_description)) }, singleLine = true)
            Dropdown(stringResource(R.string.category), COST_CATEGORIES, e.costCategory, { stringResource(Labels.costCat(it)) }) { e = e.copy(costCategory = it) }
            Dropdown(stringResource(R.string.cost_type), CostType.entries, e.type, { stringResource(Labels.costType(it)) }) { e = e.copy(type = it) }
            if (e.type == CostType.PER_SQM || e.type == CostType.PER_UNIT) {
                DecField(stringResource(if (e.type == CostType.PER_SQM) R.string.quantity_sqm else R.string.quantity), e.quantity) { e = e.copy(quantity = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumField(stringResource(R.string.unit_price_min), e.unitPriceMin, Modifier.weight(1f)) { e = e.copy(unitPriceMin = it) }
                    NumField(stringResource(R.string.unit_price_max), e.unitPriceMax, Modifier.weight(1f)) { e = e.copy(unitPriceMax = it) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumField(stringResource(R.string.material_cost), e.materialCost, Modifier.weight(1f)) { e = e.copy(materialCost = it) }
                NumField(stringResource(R.string.labor_cost), e.laborCost, Modifier.weight(1f)) { e = e.copy(laborCost = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumField(stringResource(R.string.cost_min), e.costMin, Modifier.weight(1f)) { e = e.copy(costMin = it) }
                NumField(stringResource(R.string.cost_max), e.costMax, Modifier.weight(1f)) { e = e.copy(costMax = it) }
            }
            NumField(stringResource(R.string.cost_own), e.costOwn) { e = e.copy(costOwn = it) }
            NumField(stringResource(R.string.quote_amount), e.quoteAmount) { e = e.copy(quoteAmount = it) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(e.quoteReceived, { e = e.copy(quoteReceived = it) }); Text(stringResource(R.string.quote_received)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { CostUrgency.entries.forEach { u -> FilterChip(e.urgency == u, { e = e.copy(urgency = u) }, { Text(stringResource(Labels.urgency(u))) }) } }
            Dropdown(stringResource(R.string.repair_project), listOf<Long?>(null) + projects.map { it.id }, e.repairProjectId, { id -> projects.firstOrNull { it.id == id }?.name ?: stringResource(R.string.none) }) { e = e.copy(repairProjectId = it) }
            CostCalculator.estimateRange(e)?.let { Text(stringResource(R.string.computed_amount, Fmt.range(it.min, it.max)), style = MaterialTheme.typography.bodyMedium) }
        }
    }, confirmButton = {
        TextButton(enabled = e.description.isNotBlank(), onClick = { vm.launch { vm.c.defects.saveCost(e) }; onDismiss() }) { Text(stringResource(R.string.save)) }
    }, dismissButton = {
        Row {
            if (initial.id != 0L) TextButton(onClick = { vm.launch { vm.c.defects.deleteCost(initial.id) }; onDismiss() }) { Text(stringResource(R.string.delete)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    })
}
