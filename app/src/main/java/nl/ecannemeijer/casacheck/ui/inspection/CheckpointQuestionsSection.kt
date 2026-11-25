package nl.ecannemeijer.casacheck.ui.inspection

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointQuestion
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointQuestions
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.Speaker
import nl.ecannemeijer.casacheck.ui.common.rememberSpeaker
import nl.ecannemeijer.casacheck.ui.detail.copyText

/** One question: Italian (large) with the Dutch translation below, plus a speak button. */
@Composable
fun QuestionRow(q: CheckpointQuestion, speaker: Speaker) {
    val ctx = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(q.it, style = MaterialTheme.typography.titleMedium)
            Text(q.nl, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { if (!speaker.speak(q.it)) Toast.makeText(ctx, R.string.tts_unavailable, Toast.LENGTH_LONG).show() }) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.speak))
        }
    }
}

/** Expandable "questions to ask (Italian)" block on a checkpoint page. Data comes from [CheckpointQuestions]. */
@Composable
fun CheckpointQuestionsSection(templateCode: String, title: String, itemId: Long) {
    val questions = CheckpointQuestions.forCode(templateCode)
    if (questions.isEmpty()) return
    val ctx = LocalContext.current
    val speaker = rememberSpeaker()
    var open by rememberSaveable(itemId) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.cp_questions_title) + " (${questions.size})", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
    }
    if (open) SectionCard {
        Text(stringResource(R.string.cp_questions_hint), style = MaterialTheme.typography.bodySmall)
        questions.forEach { q -> QuestionRow(q, speaker) }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { copyText(ctx, CheckpointQuestions.asText(title, questions)) }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy)) }
            IconButton(onClick = { Intents.shareText(ctx, CheckpointQuestions.asText(title, questions)) }) { Icon(Icons.Filled.Share, stringResource(R.string.share)) }
        }
    }
}