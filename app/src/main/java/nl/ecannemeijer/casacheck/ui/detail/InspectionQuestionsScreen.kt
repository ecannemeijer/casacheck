package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.ItemWithResult
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointQuestions
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.rememberSpeaker
import nl.ecannemeijer.casacheck.ui.inspection.QuestionRow
import nl.ecannemeijer.casacheck.ui.nav.Nav

/** Checkpoints of a property that deserve follow-up questions: marked unsure or as a defect, and having questions. */
fun itemsNeedingQuestions(items: List<ItemWithResult>): List<ItemWithResult> =
    items.filter { (it.effectiveStatus == CheckStatus.UNSURE || it.effectiveStatus.isDefect) && CheckpointQuestions.forCode(it.templateCode).isNotEmpty() }
        .sortedWith(compareBy({ it.categoryCode }, { it.id }))

/** Per-property overview of the Italian questions for all checkpoints marked unsure or defective. */
@Composable
fun InspectionQuestionsScreen(c: AppContainer, nav: Nav, pid: Long) {
    val all by remember(pid) { c.inspections.observeItems(pid) }.collectAsState(emptyList())
    val items = itemsNeedingQuestions(all)
    val ctx = LocalContext.current
    val speaker = rememberSpeaker()
    fun fullText() = items.joinToString("\n\n") { CheckpointQuestions.asText(c.inspections.titleOf(it), CheckpointQuestions.forCode(it.templateCode)) }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.cp_questions_overview), nav::back) {
        if (items.isNotEmpty()) {
            IconButton(onClick = { copyText(ctx, fullText()) }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy)) }
            IconButton(onClick = { Intents.shareText(ctx, fullText()) }) { Icon(Icons.Filled.Share, stringResource(R.string.share)) }
        }
    } }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { InfoNote(stringResource(if (items.isEmpty()) R.string.cp_questions_none else R.string.cp_questions_overview_help)) }
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Labels.category(item.categoryCode)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(c.inspections.titleOf(item), style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable { nav.item(item.id) })
                        CheckpointQuestions.forCode(item.templateCode).forEach { q -> QuestionRow(q, speaker) }
                    }
                }
            }
        }
    }
}