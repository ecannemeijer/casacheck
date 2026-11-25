package nl.ecannemeijer.casacheck.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.score.PrefOutcome
import nl.ecannemeijer.casacheck.domain.score.PrefResult
import nl.ecannemeijer.casacheck.domain.score.PreferenceEvaluator
import nl.ecannemeijer.casacheck.domain.score.PropertySnapshot
import nl.ecannemeijer.casacheck.domain.score.ScoreCalculator
import nl.ecannemeijer.casacheck.domain.score.ScoreGroup
import nl.ecannemeijer.casacheck.domain.score.ScoreResult
import nl.ecannemeijer.casacheck.data.db.PreferenceType
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.BaseVm
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.Nav
import nl.ecannemeijer.casacheck.ui.reports.ReportsPanel

data class CompareCol(val s: PropertySnapshot, val score: ScoreResult, val prefs: List<PrefResult>, val mainPhoto: String?)

@OptIn(ExperimentalCoroutinesApi::class)
class CompareVm(private val c: AppContainer, private val ids: List<Long>) : BaseVm() {
    private val reload = MutableStateFlow(0)
    val cols = combine(c.settings.settings, c.preferences.observe(), reload) { st, prefs, _ -> Triple(st, prefs, 0) }
        .flatMapLatest { (st, prefs) ->
            flow {
                emit(ids.mapNotNull { id ->
                    val s = c.snapshots.snapshot(id) ?: return@mapNotNull null
                    val main = s.property.mainPhotoId?.let { c.photos.get(it) } ?: c.photos.list(id).firstOrNull()
                    CompareCol(s, ScoreCalculator.calculate(s, st.weightMap(), CheckpointLibrary.byCode), PreferenceEvaluator.evaluate(s, prefs), main?.let { it.thumbPath ?: it.filePath })
                })
            }
        }.state(null)
    val weights = c.settings.settings.state(null)
    fun setWeight(g: ScoreGroup, v: Int) = launch { c.settings.update { it.copy(weights = it.weights + (g.name to v)) } }
}

private val LABEL_W = 132.dp
private val COL_W = 156.dp
private val BEST = Color(0x3343A047)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(c: AppContainer, nav: Nav, ids: List<Long>) {
    val vm = vm("compare_" + ids.joinToString(",")) { CompareVm(c, ids) }
    val cols by vm.cols.collectAsStateWithLifecycle()
    var weightsOpen by remember { mutableStateOf(false) }
    var pdfOpen by remember { mutableStateOf(false) }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.compare), nav::back) {
        IconButton(onClick = { weightsOpen = true }) { Icon(Icons.Filled.Tune, stringResource(R.string.weights)) }
        IconButton(enabled = (cols?.size ?: 0) >= 2, onClick = { pdfOpen = true }) { Icon(Icons.Filled.PictureAsPdf, stringResource(R.string.generate_pdf)) }
    } }) { pad ->
        val list = cols
        if (list == null) Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            if (list.size < 2) { ComparePicker(c, nav, ids); return@Column }
            Column(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
                Row {
                    Box(Modifier.width(LABEL_W))
                    list.forEach { col ->
                        Column(Modifier.width(COL_W).padding(4.dp).clickable { nav.property(col.s.property.id) }) {
                            Thumb(CasaFiles.file(col.mainPhoto), Modifier.fillMaxWidth().height(96.dp))
                            Text(col.s.property.title.ifBlank { stringResource(R.string.untitled_property) }, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(col.s.property.city, col.s.property.province).joinToString(", "), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
                fun nums(f: (CompareCol) -> Double?) = list.map(f)
                @Composable fun row(label: String, values: List<String>, numeric: List<Double?>? = null, higherBetter: Boolean = true, warn: List<Boolean>? = null) {
                    val valid = numeric?.filterNotNull().orEmpty()
                    val best = if (valid.size >= 2 && valid.distinct().size > 1) (if (higherBetter) valid.max() else valid.min()) else null
                    HorizontalDivider()
                    Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.width(LABEL_W).padding(4.dp), style = MaterialTheme.typography.labelMedium)
                        values.forEachIndexed { i, v ->
                            val isBest = best != null && numeric!![i] == best
                            Text(v, Modifier.width(COL_W).background(if (isBest) BEST else Color.Transparent).padding(6.dp),
                                color = if (warn?.get(i) == true) MaterialTheme.colorScheme.error else Color.Unspecified,
                                fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                val dash = "–"
                row(stringResource(R.string.score_total), list.map { it.score.total?.let { t -> "$t / 100" } ?: dash }, nums { it.score.total?.toDouble() })
                row(stringResource(R.string.inspection_completion), list.map { "${it.score.completion}%" }, nums { it.score.completion.toDouble() })
                ScoreGroup.entries.forEach { g ->
                    row("  " + stringResource(Labels.group(g)), list.map { col -> col.score.groups.firstOrNull { it.group == g }?.score?.toString() ?: dash },
                        nums { col -> col.score.groups.firstOrNull { it.group == g }?.score?.toDouble() })
                }
                row(stringResource(R.string.f_asking_price), list.map { Fmt.euro(it.s.property.askingPrice) }, nums { it.s.property.askingPrice?.toDouble() }, higherBetter = false)
                row(stringResource(R.string.price_per_sqm), list.map { col -> ppsqm(col)?.let { Fmt.euro(it) } ?: dash }, nums { ppsqm(it)?.toDouble() }, higherBetter = false)
                row(stringResource(R.string.f_living_area), list.map { Fmt.sqm(it.s.property.livingArea) }, nums { it.s.property.livingArea })
                row(stringResource(R.string.f_plot_area), list.map { Fmt.sqm(it.s.property.plotArea) }, nums { it.s.property.plotArea })
                row(stringResource(R.string.f_bedrooms), list.map { it.s.property.bedrooms?.toString() ?: dash }, nums { it.s.property.bedrooms?.toDouble() })
                row(stringResource(R.string.f_build_year), list.map { it.s.property.buildYear?.toString() ?: dash })
                row(stringResource(R.string.f_energy_label), list.map { it.s.property.energyLabel ?: dash })
                row(stringResource(R.string.cost_renovation), list.map { if (it.s.costs.lines.isEmpty()) dash else Fmt.range(it.s.costs.renovation.min, it.s.costs.renovation.max) },
                    nums { if (it.s.costs.lines.isEmpty()) null else it.s.costs.renovation.max.toDouble() }, higherBetter = false)
                row(stringResource(R.string.cost_immediate), list.map { if (it.s.costs.lines.isEmpty()) dash else Fmt.range(it.s.costs.immediate.min, it.s.costs.immediate.max) })
                row(stringResource(R.string.cost_total), list.map { col -> col.s.costs.totalMin?.let { Fmt.range(it, col.s.costs.totalMax ?: it) } ?: dash },
                    nums { it.s.costs.totalMax?.toDouble() }, higherBetter = false)
                row(stringResource(R.string.defects_without_cost), list.map { it.s.costs.defectsWithoutCost.toString() }, warn = list.map { it.s.costs.defectsWithoutCost > 0 })
                val serious = list.map { col -> col.s.defects.count { it.severity == Severity.SERIOUS && it.status != DefectStatus.RESOLVED } }
                row(stringResource(R.string.serious_defects), serious.map { it.toString() }, serious.map { it.toDouble() }, higherBetter = false, warn = serious.map { it > 0 })
                row(stringResource(R.string.critical_issues), list.map { it.score.critical.size.toString() }, nums { it.score.critical.size.toDouble() }, higherBetter = false, warn = list.map { it.score.critical.isNotEmpty() })
                row(stringResource(R.string.hard_prefs_failed), list.map { col -> col.prefs.count { it.type == PreferenceType.HARD && it.outcome == PrefOutcome.NOT_MET }.toString() },
                    nums { col -> col.prefs.count { it.type == PreferenceType.HARD && it.outcome == PrefOutcome.NOT_MET }.toDouble() }, higherBetter = false,
                    warn = list.map { col -> col.prefs.any { it.type == PreferenceType.HARD && it.outcome == PrefOutcome.NOT_MET } })
                row(stringResource(R.string.prefs_met), list.map { col -> "${col.prefs.count { it.outcome == PrefOutcome.MET }} / ${col.prefs.size}" }, nums { col -> col.prefs.count { it.outcome == PrefOutcome.MET }.toDouble() })
                row(stringResource(R.string.prefs_unknown), list.map { col -> col.prefs.count { it.outcome == PrefOutcome.UNKNOWN }.toString() })
                row(stringResource(R.string.personal_rating), list.map { col -> col.s.property.personalRating?.let { "★".repeat(it) } ?: dash }, nums { it.s.property.personalRating?.toDouble() })
                val dests = list.flatMap { it.s.destinations }.distinctBy { it.id }
                dests.forEach { d ->
                    row(d.name, list.map { col -> col.s.distances.firstOrNull { it.destinationId == d.id }?.let { x -> listOfNotNull(x.distanceKm?.let { "${Fmt.num(it)} km" }, x.travelMinutes?.let { "$it min" }).joinToString(" · ").ifBlank { dash } } ?: dash },
                        nums { col -> col.s.distances.firstOrNull { it.destinationId == d.id }?.distanceKm }, higherBetter = false)
                }
            }
            InfoNote(stringResource(R.string.score_not_guarantee))
        }
    }
    if (weightsOpen) {
        val st by vm.weights.collectAsStateWithLifecycle()
        AlertDialog(onDismissRequest = { weightsOpen = false }, title = { Text(stringResource(R.string.weights)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.weights_help), style = MaterialTheme.typography.bodySmall)
                st?.weightMap()?.forEach { (g, w) ->
                    Text("${stringResource(Labels.group(g))}: $w")
                    Slider(w.toFloat(), { vm.setWeight(g, it.toInt()) }, valueRange = 0f..10f, steps = 9)
                }
            }
        }, confirmButton = { TextButton(onClick = { weightsOpen = false }) { Text(stringResource(R.string.close)) } })
    }
    if (pdfOpen) ModalBottomSheet(onDismissRequest = { pdfOpen = false }) {
        Box(Modifier.fillMaxWidth().heightIn(max = 600.dp)) { ReportsPanel(c, nav, null, compareIds = ids) }
    }
}

private fun ppsqm(c: CompareCol): Long? = c.s.property.pricePerSqm ?: run {
    val p = c.s.property.askingPrice; val a = c.s.property.livingArea
    if (p != null && a != null && a > 0) Math.round(p / a) else null
}

@Composable
private fun ComparePicker(c: AppContainer, nav: Nav, initial: List<Long>) {
    val cards by remember { c.properties.observeCards() }.collectAsStateWithLifecycle(emptyList())
    var sel by remember { mutableStateOf(initial.toSet()) }
    Column(Modifier.padding(12.dp)) {
        InfoNote(stringResource(R.string.compare_pick_hint, sel.size))
        cards.filter { !it.property.archived }.forEach { card ->
            val p = card.property
            Row(Modifier.fillMaxWidth().clickable { sel = if (p.id in sel) sel - p.id else if (sel.size < 4) sel + p.id else sel }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(p.id in sel, null)
                Thumb(CasaFiles.file(card.thumbPath), Modifier.width(64.dp).height(48.dp))
                Column(Modifier.padding(start = 8.dp)) {
                    Text(p.title.ifBlank { stringResource(R.string.untitled_property) }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(p.city, Fmt.euro(p.askingPrice)).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        androidx.compose.material3.Button(enabled = sel.size >= 2, onClick = { nav.compare(sel.toList()) }, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.compare)) }
    }
}
