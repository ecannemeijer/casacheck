package nl.ecannemeijer.casacheck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.PreferenceType
import nl.ecannemeijer.casacheck.domain.content.ContentLibrary
import nl.ecannemeijer.casacheck.domain.content.PreferenceKind
import nl.ecannemeijer.casacheck.ui.common.AutoSaveField
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.nav.Nav

/** Personal wishes: each can be off, a hard requirement or a soft wish, with an optional limit. */
@Composable
fun PreferencesScreen(c: AppContainer, nav: Nav) {
    val scope = rememberCoroutineScope()
    val prefs by remember { c.preferences.observe() }.collectAsState(emptyList())
    val byKey = prefs.associateBy { it.key }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.my_preferences), nav::back) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { InfoNote(stringResource(R.string.preferences_help)) }
            items(ContentLibrary.preferences, key = { it.code }) { def ->
                val cur = byKey[def.code]
                val type = cur?.type ?: PreferenceType.OFF
                SectionCard(stringResource(def.name)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PreferenceType.entries.forEach { t ->
                            FilterChip(type == t, { scope.launch { c.preferences.set(def.code, t, cur?.numberValue) } }, label = { Text(stringResource(Labels.prefType(t))) })
                        }
                    }
                    if (def.kind != PreferenceKind.BOOL) {
                        val unit = stringResource(when (def.kind) { PreferenceKind.MAX_EUR -> R.string.unit_max_eur; PreferenceKind.MIN_SQM -> R.string.unit_min_sqm; else -> R.string.unit_max_km })
                        AutoSaveField(unit, cur?.numberValue?.let { Fmt.num(it) }, { v -> scope.launch { c.preferences.set(def.code, type, Fmt.parseDouble(v.replace(".", "").takeIf { def.kind == PreferenceKind.MAX_EUR } ?: v)) } },
                            keyboardType = KeyboardType.Decimal)
                        if (type != PreferenceType.OFF && cur?.numberValue == null) Text(stringResource(R.string.pref_needs_value), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
