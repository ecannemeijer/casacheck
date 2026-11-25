package nl.ecannemeijer.casacheck.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.combine
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Appointment
import nl.ecannemeijer.casacheck.data.db.InspectionSession
import nl.ecannemeijer.casacheck.data.repo.PropertyCard
import nl.ecannemeijer.casacheck.ui.common.BaseVm
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.Nav
import nl.ecannemeijer.casacheck.ui.properties.PropertyCardView
import nl.ecannemeijer.casacheck.ui.theme.StatusColors
import java.text.DateFormat
import java.util.Date

data class DashboardState(
    val cards: List<PropertyCard> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val openSessions: List<InspectionSession> = emptyList(),
    val lastOpenedId: Long? = null,
    val loaded: Boolean = false,
)

class DashboardVm(c: AppContainer) : BaseVm() {
    val state = combine(c.properties.observeCards(), c.appointments.observeUpcoming(), c.inspections.observeOpenSessions(), c.properties.observeLastOpened()) { cards, ap, s, last ->
        DashboardState(cards, ap, s, last?.id, true)
    }.state(DashboardState())
}

@Composable
fun DashboardScreen(c: AppContainer, nav: Nav) {
    val vm = vm { DashboardVm(c) }
    val st by vm.state.collectAsStateWithLifecycle()
    val active = st.cards.filter { !it.property.archived }
    val now = System.currentTimeMillis()
    val viewings = st.appointments.size + active.count { (it.property.viewingAt ?: 0) > now && st.appointments.none { a -> a.propertyId == it.property.id } }
    val ongoing = active.count { it.stats != null && it.stats.checkedItems in 1 until it.stats.totalItems }
    val finished = active.count { it.stats != null && it.stats.totalItems > 0 && it.stats.checkedItems == it.stats.totalItems }
    val serious = active.count { (it.stats?.serious ?: 0) > 0 }
    val favorites = active.count { it.property.favorite }
    val last = st.cards.firstOrNull { it.property.id == st.lastOpenedId }
    val resume = st.openSessions.firstOrNull { s -> st.cards.any { it.property.id == s.propertyId } }

    LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize().statusBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(stringResource(R.string.app_full_name), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.dashboard_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { nav.add() }, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.add_property))
                }
                FilledTonalButton(onClick = { resume?.let { nav.wizard(it.propertyId, it.mode, it.roomId, it.categoryCode, it.lastItemId) } },
                    enabled = resume != null, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.resume_inspection))
                }
            }
        }
        item { StatTile(stringResource(R.string.stat_saved), active.size, Icons.Filled.HomeWork, MaterialTheme.colorScheme.primary) { nav.tab("properties") } }
        item { StatTile(stringResource(R.string.stat_viewings), viewings, Icons.Filled.CalendarMonth, MaterialTheme.colorScheme.tertiary) { nav.tab("properties") } }
        item { StatTile(stringResource(R.string.stat_ongoing), ongoing, Icons.Filled.Timelapse, StatusColors.minor) { nav.tab("properties") } }
        item { StatTile(stringResource(R.string.stat_finished), finished, Icons.Filled.CheckCircle, StatusColors.ok) { nav.tab("properties") } }
        item { StatTile(stringResource(R.string.stat_serious), serious, Icons.Filled.Error, StatusColors.serious) { nav.tab("properties") } }
        item { StatTile(stringResource(R.string.stat_favorites), favorites, Icons.Filled.Favorite, MaterialTheme.colorScheme.secondary) { nav.tab("properties") } }
        if (st.appointments.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.upcoming_viewings), style = MaterialTheme.typography.titleMedium) }
            st.appointments.take(5).forEach { a ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val p = st.cards.firstOrNull { it.property.id == a.propertyId }?.property
                    Card(Modifier.fillMaxWidth().clickable { nav.property(a.propertyId) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(12.dp))
                            Column {
                                Text(p?.title ?: "", style = MaterialTheme.typography.titleSmall)
                                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(a.startsAt)), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        if (last != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.last_opened), style = MaterialTheme.typography.titleMedium) }
            item(span = { GridItemSpan(maxLineSpan) }) { PropertyCardView(last, onClick = { nav.property(last.property.id) }, onFavorite = null) }
        }
        if (st.loaded && st.cards.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                nl.ecannemeijer.casacheck.ui.common.EmptyState(Icons.Filled.HomeWork, stringResource(R.string.empty_properties_title), stringResource(R.string.empty_properties_text))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
