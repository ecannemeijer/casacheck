package nl.ecannemeijer.casacheck.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.ui.theme.StatusColors
import java.io.File

data class StatusStyle(@StringRes val label: Int, val color: Color, val icon: ImageVector)

fun statusStyle(s: CheckStatus): StatusStyle = when (s) {
    CheckStatus.NOT_CHECKED -> StatusStyle(R.string.status_not_checked, StatusColors.neutral, Icons.Filled.RadioButtonUnchecked)
    CheckStatus.OK -> StatusStyle(R.string.status_ok, StatusColors.ok, Icons.Filled.CheckCircle)
    CheckStatus.MINOR -> StatusStyle(R.string.status_minor, StatusColors.minor, Icons.Filled.Info)
    CheckStatus.MAJOR -> StatusStyle(R.string.status_major, StatusColors.major, Icons.Filled.Warning)
    CheckStatus.SERIOUS -> StatusStyle(R.string.status_serious, StatusColors.serious, Icons.Filled.Error)
    CheckStatus.NOT_APPLICABLE -> StatusStyle(R.string.status_na, StatusColors.neutral, Icons.Filled.Block)
    CheckStatus.UNSURE -> StatusStyle(R.string.status_unsure, StatusColors.unsure, Icons.Filled.HelpOutline)
}

fun severityStyle(s: Severity) = when (s) {
    Severity.MINOR -> statusStyle(CheckStatus.MINOR); Severity.MAJOR -> statusStyle(CheckStatus.MAJOR); Severity.SERIOUS -> statusStyle(CheckStatus.SERIOUS)
}

val specialistStyle = StatusStyle(R.string.specialist_needed, StatusColors.specialist, Icons.Filled.Engineering)

@Composable
fun StatusBadge(style: StatusStyle, modifier: Modifier = Modifier, compact: Boolean = false) {
    Surface(color = style.color.copy(alpha = 0.14f), shape = RoundedCornerShape(50), modifier = modifier) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(16.dp))
            if (!compact) { Spacer(Modifier.width(4.dp)); Text(stringResource(style.label), style = MaterialTheme.typography.labelMedium, color = style.color) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
        actions = actions,
    )
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun WarningCard(text: String, color: Color = StatusColors.serious, icon: ImageVector = Icons.Filled.Report) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun InfoNote(text: String) = WarningCard(text, MaterialTheme.colorScheme.tertiary, Icons.Filled.Info)

@Composable
fun LabeledValue(label: String, value: String?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.45f))
        Text(value?.takeIf { it.isNotBlank() } ?: "–", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f))
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (text != null) Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable
fun Thumb(path: File?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (path != null) AsyncImage(model = path, contentDescription = null, contentScale = contentScale, modifier = Modifier.fillMaxSize())
    }
}

/**
 * Text field with autosave: changes are saved 600 ms after typing stops and when the field leaves the screen.
 * External updates are only applied while the field is not focused, so typing is never overwritten.
 */
@Composable
fun AutoSaveField(label: String, value: String?, onSave: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true,
                  keyboardType: KeyboardType = KeyboardType.Text, minLines: Int = 1, supporting: String? = null) {
    var text by remember { mutableStateOf(value ?: "") }
    var lastSaved by remember { mutableStateOf(value ?: "") }
    var focused by remember { mutableStateOf(false) }
    val latestSave = rememberUpdatedState(onSave)
    LaunchedEffect(value) { if (!focused && (value ?: "") != text) { text = value ?: ""; lastSaved = text } }
    LaunchedEffect(text) {
        if (text != lastSaved) { delay(600); latestSave.value(text); lastSaved = text }
    }
    DisposableEffect(Unit) { onDispose { if (text != lastSaved) latestSave.value(text) } }
    OutlinedTextField(
        value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = singleLine, minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        supportingText = supporting?.let { { Text(it) } },
    )
}

/** Yes / No / Unknown selector (unknown stays null, nothing is guessed). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TriStateRow(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = value == true, onClick = { onChange(true) }, label = { Text(stringResource(R.string.yes)) })
            FilterChip(selected = value == false, onClick = { onChange(false) }, label = { Text(stringResource(R.string.no)) })
            FilterChip(selected = value == null, onClick = { onChange(null) }, label = { Text(stringResource(R.string.unknown)) })
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun VSpace(h: Int = 8) = Spacer(Modifier.height(h.dp))
