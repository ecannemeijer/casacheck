package nl.ecannemeijer.casacheck.data.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Deleting a property with a short "undo" window: the property is hidden at once, and only deleted for real
 * (rows + files) when the window has passed without "Ongedaan maken". If the app is killed inside the window,
 * nothing is lost: the property simply stays.
 */
class PropertyDeletion(
    private val scope: CoroutineScope,
    private val hide: (Long) -> Unit,
    private val unhide: (Long) -> Unit,
    private val deleteNow: suspend (Long) -> Unit,
    private val undoWindowMs: Long = UNDO_WINDOW_MS,
) {
    data class Pending(val id: Long, val title: String)

    private val _current = MutableStateFlow<Pending?>(null)
    /** The most recent deletion that can still be undone (shown as snackbar). */
    val current: StateFlow<Pending?> = _current.asStateFlow()
    private val jobs = mutableMapOf<Long, Job>()
    private val lock = Any()

    fun request(id: Long, title: String) {
        hide(id)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            delay(undoWindowMs)
            val mine = synchronized(lock) { jobs.remove(id) != null }
            if (!mine) return@launch
            try {
                withContext(NonCancellable) { deleteNow(id) }
            } catch (e: Exception) {
                unhide(id)
            } finally {
                _current.update { if (it?.id == id) null else it }
            }
        }
        synchronized(lock) { jobs.put(id, job) }?.cancel()
        _current.value = Pending(id, title)
        job.start()
    }

    /** Cancels a pending deletion; returns false when it is too late (already being deleted). */
    fun undo(id: Long): Boolean {
        val job = synchronized(lock) { jobs.remove(id) } ?: return false
        job.cancel()
        unhide(id)
        _current.update { if (it?.id == id) null else it }
        return true
    }

    fun isPending(id: Long): Boolean = synchronized(lock) { id in jobs }

    companion object {
        const val UNDO_WINDOW_MS = 8_000L
    }
}