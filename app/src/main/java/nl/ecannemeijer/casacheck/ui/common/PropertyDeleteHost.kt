package nl.ecannemeijer.casacheck.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.repo.PropertyDeletion

/** App-wide "Woning verwijderd — Ongedaan maken" snackbar, shown above the bottom navigation on every screen. */
@Composable
fun PropertyDeleteHost(deletion: PropertyDeletion) {
    val cur by deletion.current.collectAsState()
    val host = remember { SnackbarHostState() }
    val undo = stringResource(R.string.undo)
    val untitled = stringResource(R.string.untitled_property)
    val msg = cur?.let { stringResource(R.string.property_deleted, it.title.ifBlank { untitled }) }
    // When the pending deletion ends (undone or committed), this effect is cancelled and the snackbar disappears.
    LaunchedEffect(cur) {
        val p = cur ?: return@LaunchedEffect
        if (host.showSnackbar(msg ?: "", undo, duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) deletion.undo(p.id)
    }
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(host, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp))
    }
}