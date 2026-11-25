package nl.ecannemeijer.casacheck.ui.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BaseVm : ViewModel() {
    protected fun <T> Flow<T>.state(initial: T): StateFlow<T> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
    fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}

@Composable
inline fun <reified VM : ViewModel> vm(key: String? = null, noinline create: () -> VM): VM = viewModel(key = key) { create() }
