package nl.ecannemeijer.casacheck.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.Nav
import nl.ecannemeijer.casacheck.ui.reports.ReportsPanel

val DETAIL_TABS = listOf(
    R.string.tab_overview, R.string.tab_listing, R.string.tab_photos, R.string.tab_inspection, R.string.tab_rooms, R.string.tab_defects,
    R.string.tab_measurements, R.string.tab_costs, R.string.tab_documents, R.string.tab_environment, R.string.tab_report,
)
const val TAB_OVERVIEW = 0; const val TAB_LISTING = 1; const val TAB_PHOTOS = 2; const val TAB_INSPECTION = 3; const val TAB_ROOMS = 4
const val TAB_DEFECTS = 5; const val TAB_MEASUREMENTS = 6; const val TAB_COSTS = 7; const val TAB_DOCUMENTS = 8; const val TAB_ENV = 9; const val TAB_REPORT = 10

@Composable
fun PropertyDetailScreen(c: AppContainer, nav: Nav, id: Long, tab: Int) {
    val vm = vm("detail$id") { DetailVm(c, id) }
    val p by vm.property.collectAsStateWithLifecycle()
    var sel by rememberSaveable { mutableIntStateOf(tab.coerceIn(0, DETAIL_TABS.lastIndex)) }
    var confirmDelete by remember { mutableStateOf(false) }
    p?.let { pp ->
        if (confirmDelete) ConfirmDialog(stringResource(R.string.delete_property_title), stringResource(R.string.delete_property_text, pp.title.ifBlank { stringResource(R.string.untitled_property) }),
            stringResource(R.string.delete), onConfirm = { c.deletion.request(id, pp.title); nav.leaveProperty() }, onDismiss = { confirmDelete = false })
    }
    Scaffold(topBar = {
        BackTopBar(p?.title?.ifBlank { null } ?: stringResource(R.string.untitled_property), nav::back) {
            p?.let { pp ->
                IconButton(onClick = { vm.favorite(!pp.favorite) }) {
                    Icon(if (pp.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = { nav.edit(id) }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete_property)) }
            }
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            ScrollableTabRow(selectedTabIndex = sel, edgePadding = androidx.compose.ui.unit.Dp(8f)) {
                DETAIL_TABS.forEachIndexed { i, t -> Tab(selected = sel == i, onClick = { sel = i }, text = { Text(stringResource(t), maxLines = 1, overflow = TextOverflow.Ellipsis) }) }
            }
            if (p == null) return@Column
            when (sel) {
                TAB_OVERVIEW -> OverviewTab(vm, nav) { sel = it }
                TAB_LISTING -> ListingTab(vm, nav)
                TAB_PHOTOS -> PhotosTab(vm, nav)
                TAB_INSPECTION -> InspectionTab(vm, nav)
                TAB_ROOMS -> RoomsTab(vm, nav)
                TAB_DEFECTS -> DefectsTab(vm, nav)
                TAB_MEASUREMENTS -> MeasurementsTab(vm, nav)
                TAB_COSTS -> CostsTab(vm, nav)
                TAB_DOCUMENTS -> DocumentsTab(vm, nav)
                TAB_ENV -> EnvironmentTab(vm, nav)
                TAB_REPORT -> ReportsPanel(c, nav, id)
            }
        }
    }
}
