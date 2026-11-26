package nl.ecannemeijer.casacheck.data.recording

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live state of the (single) running recording, shared between the service, the mini bar and the screens. */
data class RecState(
    val recordingId: Long,
    val propertyId: Long,
    val propertyTitle: String,
    /** elapsedRealtime when the current (unpaused) part started. */
    val segmentStart: Long,
    /** Recorded time before the current part. */
    val accumulatedMs: Long,
    val paused: Boolean = false,
    /** Paused automatically because of a phone call. */
    val autoPaused: Boolean = false,
    val markers: Int = 0,
) {
    fun elapsedMs(now: Long = SystemClock.elapsedRealtime()): Long = accumulatedMs + if (paused) 0 else (now - segmentStart).coerceAtLeast(0)
}

/** The inspection checkpoint that is open on screen, so a marker can be linked to it. */
data class CurrentCheckpoint(val propertyId: Long, val itemId: Long, val title: String)

object Recorder {
    internal val mutable = MutableStateFlow<RecState?>(null)
    val state: StateFlow<RecState?> = mutable.asStateFlow()
    /** One-shot user message (string resource id) from the service, e.g. "recording saved" or "stopped: storage full". */
    val messages = MutableStateFlow<Int?>(null)
    val checkpoint = MutableStateFlow<CurrentCheckpoint?>(null)

    fun start(ctx: Context, propertyId: Long) =
        ContextCompat.startForegroundService(ctx, Intent(ctx, RecordingService::class.java).setAction(RecordingService.ACTION_START).putExtra(RecordingService.EXTRA_PID, propertyId))
    fun pause(ctx: Context) = send(ctx, RecordingService.ACTION_PAUSE)
    fun resume(ctx: Context) = send(ctx, RecordingService.ACTION_RESUME)
    fun stop(ctx: Context) = send(ctx, RecordingService.ACTION_STOP)
    fun mark(ctx: Context) = send(ctx, RecordingService.ACTION_MARK)

    private fun send(ctx: Context, action: String) {
        if (state.value == null) return
        ctx.startService(Intent(ctx, RecordingService::class.java).setAction(action))
    }
}