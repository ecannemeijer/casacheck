package nl.ecannemeijer.casacheck.ui.properties

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.repo.PropertyCard
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.score.ScoreCalculator
import nl.ecannemeijer.casacheck.ui.common.BaseVm
import nl.ecannemeijer.casacheck.ui.common.ConfirmDialog
import nl.ecannemeijer.casacheck.ui.common.EmptyState
import nl.ecannemeijer.casacheck.ui.common.Fmt
import nl.ecannemeijer.casacheck.ui.common.StatusBadge
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.common.statusStyle
import nl.ecannemeijer.casacheck.ui.common.vm
import nl.ecannemeijer.casacheck.ui.nav.Nav
import nl.ecannemeijer.casacheck.data.db.CheckStatus

enum class SortBy(val label: Int) {
    ADDED(R.string.sort_added), PRICE(R.string.sort_price), TOTAL(R.string.sort_total), AREA(R.string.sort_area), PRICE_SQM(R.string.sort_price_sqm),
    SCORE(R.string.sort_score), DEFECTS(R.string.sort_defects), RENOVATION(R.string.sort_renovation), VIEWING(R.string.sort_viewing)
}
enum class ListFilter(val label: Int) { ALL(R.string.filter_all), FAVORITES(R.string.filter_favorites), SERIOUS(R.string.filter_serious), NOT_STARTED(R.string.filter_not_started), ARCHIVED(R.string.filter_archived) }

@OptIn(ExperimentalCoroutinesApi::class)
class PropertyListVm(private val c: AppContainer) : BaseVm() {
    val query = MutableStateFlow(""); val sort = MutableStateFlow(SortBy.ADDED); val filter = MutableStateFlow(ListFilter.ALL)
    private val scores = c.properties.observeCards().mapLatest { cards ->
        val w = c.settings.current().weightMap()
        cards.associate { it.property.id to (c.snapshots.snapshot(it.property.id)?.let { s -> ScoreCalculator.calculate(s, w, CheckpointLibrary.byCode).total } ) }
    }.state(emptyMap())
    val scoreMap = scores
    val cards = combine(c.properties.observeCards(), query, sort, filter, scores) { cards, q, s, f, sc ->
        cards.filter { card ->
            val p = card.property
            val match = q.isBlank() || listOfNotNull(p.title, p.city, p.address, p.province, p.referenceNumber, p.notes).any { it.contains(q, true) }
            val flt = when (f) {
                ListFilter.ALL -> !p.archived; ListFilter.FAVORITES -> p.favorite && !p.archived
                ListFilter.SERIOUS -> !p.archived && (card.stats?.serious ?: 0) > 0
                ListFilter.NOT_STARTED -> !p.archived && (card.stats?.checkedItems ?: 0) == 0
                ListFilter.ARCHIVED -> p.archived
            }
            match && flt
        }.let { list ->
            when (s) {
                SortBy.ADDED -> list.sortedByDescending { it.property.createdAt }
                SortBy.PRICE -> list.sortedBy { it.property.askingPrice ?: Long.MAX_VALUE }
                SortBy.TOTAL -> list.sortedBy { it.costs.totalMax ?: Long.MAX_VALUE }
                SortBy.AREA -> list.sortedByDescending { it.property.livingArea ?: -1.0 }
                SortBy.PRICE_SQM -> list.sortedBy { it.property.pricePerSqm ?: it.property.askingPrice?.let { pr -> it.property.livingArea?.takeIf { a -> a > 0 }?.let { a -> (pr / a).toLong() } } ?: Long.MAX_VALUE }
                SortBy.SCORE -> list.sortedByDescending { sc[it.property.id] ?: -1 }
                SortBy.DEFECTS -> list.sortedByDescending { (it.stats?.serious ?: 0) * 10000 + (it.stats?.major ?: 0) * 100 + (it.stats?.minor ?: 0) }
                SortBy.RENOVATION -> list.sortedBy { it.costs.renovation.max }
                SortBy.VIEWING -> list.sortedBy { it.property.viewingAt ?: Long.MAX_VALUE }
            }
        }
    }.state(null)

    fun favorite(id: Long, f: Boolean) = launch { c.properties.setFavorite(id, f) }
    fun archive(id: Long, a: Boolean) = launch { c.properties.setArchived(id, a) }
    fun delete(id: Long) = launch { c.properties.delete(id) }
    fun duplicate(id: Long, suffix: String) = launch { c.properties.duplicate(id, suffix) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PropertyListScreen(c: AppContainer, nav: Nav) {
    val vm = vm { PropertyListVm(c) }
    val cards by vm.cards.collectAsStateWithLifecycle()
    val q by vm.query.collectAsStateWithLifecycle(); val sort by vm.sort.collectAsStateWithLifecycle(); val filter by vm.filter.collectAsStateWithLifecycle()
    val scores by vm.scoreMap.collectAsStateWithLifecycle()
    var sortMenu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf<PropertyCard?>(null) }
    val copySuffix = stringResource(R.string.copy_suffix)

    Scaffold(floatingActionButton = {
        if (selected.size >= 2) ExtendedFloatingActionButton(onClick = { nav.compare(selected.toList()); selected = emptySet() }, icon = { Icon(Icons.Filled.CompareArrows, null) }, text = { Text(stringResource(R.string.compare_selected, selected.size)) })
        else ExtendedFloatingActionButton(onClick = { nav.add() }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.add_property)) })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(top = pad.calculateTopPadding()).statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(q, { vm.query.value = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.search_properties)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true)
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, stringResource(R.string.sort)) }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        SortBy.entries.forEach { s -> DropdownMenuItem(text = { Text(stringResource(s.label) + if (s == sort) "  ✓" else "") }, onClick = { vm.sort.value = s; sortMenu = false }) }
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListFilter.entries.forEach { f -> FilterChip(selected = filter == f, onClick = { vm.filter.value = f }, label = { Text(stringResource(f.label)) }) }
            }
            if (selected.isNotEmpty()) Text(stringResource(R.string.selected_hint, selected.size), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
            val list = cards
            if (list != null && list.isEmpty()) {
                EmptyState(Icons.Filled.HomeWork, stringResource(R.string.empty_properties_title), stringResource(R.string.empty_properties_text))
            }
            LazyVerticalGrid(GridCells.Adaptive(330.dp), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list.orEmpty(), key = { it.property.id }) { card ->
                    PropertyCardView(card, score = scores[card.property.id], selected = card.property.id in selected,
                        onClick = { if (selected.isNotEmpty()) selected = selected.toggle(card.property.id) else nav.property(card.property.id) },
                        onLongClick = { selected = selected.toggle(card.property.id) },
                        onFavorite = { vm.favorite(card.property.id, !card.property.favorite) },
                        menu = { dismiss ->
                            DropdownMenuItem(text = { Text(stringResource(if (card.property.archived) R.string.unarchive else R.string.archive)) },
                                leadingIcon = { Icon(if (card.property.archived) Icons.Filled.Unarchive else Icons.Filled.Archive, null) },
                                onClick = { vm.archive(card.property.id, !card.property.archived); dismiss() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.duplicate)) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                onClick = { vm.duplicate(card.property.id, copySuffix); dismiss() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.select_for_compare)) }, leadingIcon = { Icon(Icons.Filled.CheckBox, null) },
                                onClick = { selected = selected.toggle(card.property.id); dismiss() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { confirmDelete = card; dismiss() })
                        })
                }
            }
        }
    }
    confirmDelete?.let { card ->
        ConfirmDialog(stringResource(R.string.delete_property_title), stringResource(R.string.delete_property_text, card.property.title), stringResource(R.string.delete),
            onConfirm = { vm.delete(card.property.id) }, onDismiss = { confirmDelete = null })
    }
}

private fun Set<Long>.toggle(id: Long) = if (id in this) this - id else this + id

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PropertyCardView(card: PropertyCard, score: Int? = null, selected: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit = {},
                     onFavorite: (() -> Unit)?, menu: (@Composable (dismiss: () -> Unit) -> Unit)? = null) {
    val p = card.property
    var menuOpen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Box {
            Thumb(card.thumbPath?.let { nl.ecannemeijer.casacheck.CasaFiles.file(it) }, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Row(Modifier.align(Alignment.TopEnd)) {
                if (onFavorite != null) IconButton(onClick = onFavorite) {
                    Icon(if (p.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.secondary)
                }
                if (menu != null) Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                    DropdownMenu(menuOpen, { menuOpen = false }) { menu { menuOpen = false } }
                }
            }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(p.title.ifBlank { stringResource(R.string.untitled_property) }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(p.city, p.province).joinToString(", ").ifBlank { "–" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Fmt.euro(p.askingPrice), style = MaterialTheme.typography.titleSmall)
                Text(Fmt.sqm(p.livingArea), style = MaterialTheme.typography.bodyMedium)
                if (p.plotArea != null) Text(stringResource(R.string.plot_short, Fmt.sqm(p.plotArea)), style = MaterialTheme.typography.bodyMedium)
                p.energyLabel?.let { Text("APE $it", style = MaterialTheme.typography.bodyMedium) }
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { card.progress / 100f }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.inspection_progress, card.progress), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (score != null) Text(stringResource(R.string.score_short, score), style = MaterialTheme.typography.labelMedium)
                p.personalRating?.let { Icon(Icons.Filled.Star, null, Modifier.size(16.dp)); Text("$it/5", style = MaterialTheme.typography.labelMedium) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusBadge(statusStyle(CheckStatus.MINOR), compact = true); Text("${card.stats?.minor ?: 0}")
                StatusBadge(statusStyle(CheckStatus.MAJOR), compact = true); Text("${card.stats?.major ?: 0}")
                StatusBadge(statusStyle(CheckStatus.SERIOUS), compact = true); Text("${card.stats?.serious ?: 0}")
            }
            if (card.costs.lines.any { it.hasAmount }) {
                Text(stringResource(R.string.renovation_short, Fmt.range(card.costs.renovation.min, card.costs.renovation.max)), style = MaterialTheme.typography.bodySmall)
            }
            card.costs.totalMin?.let { Text(stringResource(R.string.total_investment_short, Fmt.range(it, card.costs.totalMax!!)), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
