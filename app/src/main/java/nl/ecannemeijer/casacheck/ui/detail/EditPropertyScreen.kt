package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.domain.checklist.FeatureKeys
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.InfoNote
import nl.ecannemeijer.casacheck.ui.common.Labels
import nl.ecannemeijer.casacheck.ui.common.PropertyForm
import nl.ecannemeijer.casacheck.ui.common.SectionCard
import nl.ecannemeijer.casacheck.ui.common.TriStateRow
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.Nav
import kotlinx.coroutines.launch

@Composable
fun EditPropertyScreen(c: AppContainer, nav: Nav, id: Long) {
    val vm = vm("detail$id") { DetailVm(c, id) }
    val p = vm.property.collectAsStateWithLifecycle().value
    val f by vm.features.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.edit_property), nav::back) }) { pad ->
        if (p == null) return@Scaffold
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoNote(stringResource(R.string.autosave_note))
            PropertyForm(p, null) { field, t -> vm.update(field, t) }
            SectionCard(stringResource(R.string.extra_features)) {
                FeatureKeys.BOOL_KEYS.forEach { k -> TriStateRow(stringResource(Labels.feature(k)), f[k]?.toBooleanStrictOrNull()) { vm.setFeature(k, it) } }
            }
            OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.delete_property_title)) }
        }
    }
    if (confirmDelete && p != null) ConfirmDialog(stringResource(R.string.delete_property_title), stringResource(R.string.delete_property_text, p.title), stringResource(R.string.delete),
        onConfirm = { c.scope.launch { c.properties.delete(id) }; nav.tab("properties") }, onDismiss = { confirmDelete = false })
}

