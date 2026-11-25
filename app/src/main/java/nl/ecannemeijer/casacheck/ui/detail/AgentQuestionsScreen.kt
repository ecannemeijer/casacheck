package nl.ecannemeijer.casacheck.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.AgentQuestion
import nl.ecannemeijer.casacheck.domain.content.ContentLibrary
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Intents
import nl.ecannemeijer.casacheck.ui.common.rememberSpeaker
import nl.ecannemeijer.casacheck.ui.nav.Nav
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

data class QText(val italian: String, val local: String)

@Composable
fun questionText(q: AgentQuestion): QText {
    if (!q.builtIn) return QText(q.customIt ?: q.customNl ?: "", q.customNl ?: "")
    val def = ContentLibrary.agentQuestions.firstOrNull { it.code == q.code } ?: return QText(q.code, q.code)
    return QText(def.italian, stringResource(def.text))
}

@Composable
fun AgentQuestionsScreen(c: AppContainer, nav: Nav, pid: Long) {
    val questions by c.agent.observeQuestions().collectAsState(emptyList())
    val answers by c.agent.observeAnswers(pid).collectAsState(emptyList())
    val ctx = LocalContext.current
    val speaker = rememberSpeaker()
    val scope = rememberCoroutineScope()
    var big by remember { mutableStateOf<QText?>(null) }
    var add by remember { mutableStateOf(false) }
    var onlyOpen by remember { mutableStateOf(false) }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.agent_questions), nav::back) {
        IconButton(onClick = {
            val sb = StringBuilder()
            questions.forEach { q -> val def = ContentLibrary.agentQuestions.firstOrNull { it.code == q.code }; sb.append("• ").append(def?.italian ?: q.customIt ?: q.customNl).append('\n') }
            Intents.shareText(ctx, sb.toString())
        }) { Icon(Icons.Filled.Share, stringResource(R.string.share)) }
        IconButton(onClick = { add = true }) { Icon(Icons.Filled.Add, stringResource(R.string.add_question)) }
    } }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                InfoNote(stringResource(R.string.agent_help))
                Row { FilterChip(onlyOpen, { onlyOpen = !onlyOpen }, { Text(stringResource(R.string.only_unasked)) }) }
            }
            items(questions.filter { q -> !onlyOpen || answers.none { it.questionId == q.id && it.asked } }, key = { it.id }) { q ->
                val t = questionText(q)
                val a = answers.firstOrNull { it.questionId == q.id }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t.italian, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable { big = t })
                        Text(t.local, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (!speaker.speak(t.italian)) Toast.makeText(ctx, R.string.tts_unavailable, Toast.LENGTH_LONG).show() }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.speak)) }
                            IconButton(onClick = { big = t }) { Icon(Icons.Filled.Fullscreen, stringResource(R.string.show_large)) }
                            IconButton(onClick = { copyText(ctx, t.italian) }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy)) }
                            Checkbox(a?.asked == true, { v -> scope.launch { c.agent.saveAnswer(pid, q.id, a?.answer, a?.notes, v) } })
                            Text(stringResource(R.string.asked), Modifier.weight(1f))
                            if (!q.builtIn) IconButton(onClick = { scope.launch { c.agent.deleteCustom(q.id) } }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                        }
                        AutoSaveField(stringResource(R.string.answer), a?.answer, { v -> scope.launch { c.agent.saveAnswer(pid, q.id, v.ifBlank { null }, a?.notes, a?.asked == true || v.isNotBlank()) } }, singleLine = false)
                    }
                }
            }
        }
    }
    big?.let { t ->
        Dialog(onDismissRequest = { big = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).clickable { big = null }.padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(t.italian, fontSize = 40.sp, lineHeight = 48.sp, textAlign = TextAlign.Center)
                    Text(t.local, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    IconButton(onClick = { speaker.speak(t.italian) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.speak)) }
                }
            }
        }
    }
    if (add) {
        var nl by remember { mutableStateOf("") }
        var it by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { add = false }, title = { Text(stringResource(R.string.add_question)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nl, { nl = it }, label = { Text(stringResource(R.string.question_own_language)) })
                OutlinedTextField(it, { v -> it = v }, label = { Text(stringResource(R.string.question_italian)) })
            }
        }, confirmButton = { TextButton(enabled = nl.isNotBlank() || it.isNotBlank(), onClick = { scope.launch { c.agent.addCustom(nl.ifBlank { it }, it.ifBlank { null }) }; add = false }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { add = false }) { Text(stringResource(R.string.cancel)) } })
    }
}
