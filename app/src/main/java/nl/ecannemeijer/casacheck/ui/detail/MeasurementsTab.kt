package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.report.PdfReportGenerator
import nl.ecannemeijer.casacheck.domain.moisture.MoistureAnalyzer
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.nav.Nav
import nl.ecannemeijer.casacheck.ui.theme.StatusColors
import java.text.DateFormat
import java.util.Date

/** Wall view: readings drawn at their height on a schematic wall; colour relative to the reference or the wall's minimum. */
@Composable
fun MoistureWall(points: List<Pair<Int, Double>>, reference: Double?, modifier: Modifier = Modifier) {
    val wall = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.colorScheme.outline
    val maxH = (points.maxOfOrNull { it.first } ?: 200).coerceAtLeast(200)
    val base = reference?.takeIf { it > 0 } ?: points.minOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    val maxV = points.maxOfOrNull { it.second }?.coerceAtLeast(base) ?: base
    Canvas(modifier) {
        val wallW = size.width * 0.35f
        drawRect(wall, Offset(0f, 0f), Size(wallW, size.height))
        drawRect(line, Offset(0f, 0f), Size(wallW, size.height), style = Stroke(2f))
        drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), 4f)
        points.forEach { (h, v) ->
            val y = size.height - (h.toFloat() / maxH) * size.height * 0.95f
            val ratio = v / base
            val col = when { ratio >= 2.0 -> StatusColors.serious; ratio >= 1.5 -> StatusColors.major; ratio >= 1.2 -> StatusColors.minor; else -> StatusColors.ok }
            drawCircle(col, 10f, Offset(wallW / 2, y))
            val len = ((v / maxV) * (size.width - wallW - 16f)).toFloat()
            drawRect(col.copy(alpha = 0.7f), Offset(wallW + 8f, y - 8f), Size(len.coerceAtLeast(2f), 16f))
        }
        reference?.let { r ->
            val x = wallW + 8f + ((r / maxV) * (size.width - wallW - 16f)).toFloat()
            drawLine(Color.Gray, Offset(x, 0f), Offset(x, size.height), 2f)
        }
    }
}

@Composable
fun MeasurementsTab(vm: DetailVm, nav: Nav) {
    val moisture by vm.moisture.collectAsStateWithLifecycle()
    val points by vm.points.collectAsStateWithLifecycle()
    val cracks by vm.cracks.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(stringResource(R.string.section_moisture), style = MaterialTheme.typography.titleLarge)
            InfoNote(stringResource(R.string.moisture_disclaimer))
            Button(onClick = { nav.moisture(vm.pid) }, Modifier.padding(top = 8.dp)) { Icon(Icons.Filled.Add, null); Text(" " + stringResource(R.string.add_measurement)) }
        }
        if (moisture.isEmpty()) item { Text(stringResource(R.string.no_measurements)) }
        items(moisture, key = { "m" + it.id }) { m ->
            val pts = points.filter { it.measurementId == m.id && it.value != null }.map { it.heightCm to it.value!! }
            val a = MoistureAnalyzer.analyze(pts, m.referenceValue)
            Card(Modifier.fillMaxWidth().clickable { nav.moisture(vm.pid, m.id) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MoistureWall(pts, m.referenceValue, Modifier.width(110.dp).height(90.dp))
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(listOfNotNull(rooms.firstOrNull { it.id == m.roomId }?.name, m.wall).joinToString(" – "), style = MaterialTheme.typography.titleSmall)
                        Text(df.format(Date(m.measuredAt)) + (m.unit?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                        Text(pts.joinToString("  ") { "${it.first}cm: ${nl.ecannemeijer.casacheck.ui.common.Fmt.num(it.second)}" }, style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(PdfReportGenerator.patternText(a.pattern)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.section_cracks), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
            InfoNote(stringResource(R.string.crack_width_note))
            Button(onClick = { nav.crack(vm.pid) }, Modifier.padding(top = 8.dp)) { Icon(Icons.Filled.Add, null); Text(" " + stringResource(R.string.add_crack)) }
        }
        if (cracks.isEmpty()) item { Text(stringResource(R.string.no_measurements)) }
        val latest = cracks.filter { c -> cracks.none { it.previousId == c.id } }
        items(latest, key = { "c" + it.id }) { cr ->
            val chain = generateSequence(cr) { x -> x.previousId?.let { pid -> cracks.firstOrNull { it.id == pid } } }.toList()
            Card(Modifier.fillMaxWidth().clickable { nav.crack(vm.pid, cr.id) }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(cr.location + " · " + stringResource(Labels.crack(cr.direction)), style = MaterialTheme.typography.titleSmall)
                    Text(listOfNotNull(rooms.firstOrNull { it.id == cr.roomId }?.name, stringResource(if (cr.inside) R.string.inside else R.string.outside)).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    chain.reversed().forEach { m ->
                        Text("${df.format(Date(m.measuredAt))}: " + listOfNotNull(m.widthMm?.let { "$it mm" }, m.lengthCm?.let { "$it cm" }).joinToString(" × ").ifBlank { "–" }, style = MaterialTheme.typography.bodySmall)
                    }
                    if (chain.size >= 2) {
                        val w1 = chain.last().widthMm; val w2 = chain.first().widthMm
                        if (w1 != null && w2 != null) {
                            val grew = w2 > w1
                            Text(stringResource(if (grew) R.string.crack_grew else R.string.crack_stable, "%.1f".format(w2 - w1)),
                                color = if (grew) StatusColors.serious else StatusColors.ok, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    TextButton(onClick = { nav.crack(vm.pid, prev = cr.id) }) { Text(stringResource(R.string.crack_followup)) }
                }
            }
        }
    }
}
