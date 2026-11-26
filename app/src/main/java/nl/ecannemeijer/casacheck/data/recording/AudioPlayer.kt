package nl.ecannemeijer.casacheck.data.recording

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/** App-wide audio player for recordings: keeps playing while the user moves between screens. */
class AudioPlayer(private val ctx: Context) {
    data class State(
        val recordingId: Long? = null,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val playing: Boolean = false,
        val speed: Float = 1f,
        val error: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var mp: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs)
        .setOnAudioFocusChangeListener { if (it == AudioManager.AUDIOFOCUS_LOSS || it == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause() }.build()

    /** Prepares [file] for [recordingId] unless it is already loaded. */
    fun load(recordingId: Long, file: File) {
        if (_state.value.recordingId == recordingId && mp != null) return
        release()
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(attrs)
            p.setDataSource(file.absolutePath)
            p.prepare()
            p.setOnCompletionListener { stopTicker(); _state.update { s -> s.copy(playing = false, positionMs = s.durationMs) }; am.abandonAudioFocusRequest(focus) }
            mp = p
            _state.value = State(recordingId = recordingId, durationMs = p.duration.toLong(), speed = _state.value.speed)
        } catch (e: Exception) {
            mp = null
            _state.value = State(recordingId = recordingId, error = true)
        }
    }

    fun play() {
        val p = mp ?: return
        if (Recorder.state.value?.paused == false) return // never play over a running recording
        if (_state.value.durationMs > 0 && p.currentPosition >= _state.value.durationMs - 200) p.seekTo(0)
        am.requestAudioFocus(focus)
        p.start()
        runCatching { p.playbackParams = p.playbackParams.setSpeed(_state.value.speed) }
        _state.update { it.copy(playing = true) }
        startTicker()
    }

    fun pause() {
        val p = mp ?: return
        if (p.isPlaying) p.pause()
        stopTicker()
        _state.update { it.copy(playing = false, positionMs = p.currentPosition.toLong()) }
    }

    fun toggle() { if (_state.value.playing) pause() else play() }

    fun seekTo(ms: Long) {
        val p = mp ?: return
        val t = ms.coerceIn(0, _state.value.durationMs.coerceAtLeast(0))
        p.seekTo(t, MediaPlayer.SEEK_CLOSEST)
        _state.update { it.copy(positionMs = t) }
    }

    fun skip(deltaMs: Long) = seekTo((mp?.currentPosition?.toLong() ?: _state.value.positionMs) + deltaMs)

    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        val p = mp ?: return
        // Setting playback params on a paused MediaPlayer would start it; apply only while playing.
        if (p.isPlaying) runCatching { p.playbackParams = p.playbackParams.setSpeed(speed) }
    }

    /** Releases the player when [recordingId] is deleted (or always when null). */
    fun release(recordingId: Long? = null) {
        if (recordingId != null && _state.value.recordingId != recordingId) return
        stopTicker()
        mp?.let { runCatching { it.stop() }; it.release() }
        mp = null
        am.abandonAudioFocusRequest(focus)
        _state.value = State(speed = _state.value.speed)
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (isActive) {
                mp?.let { p -> _state.update { it.copy(positionMs = p.currentPosition.toLong()) } }
                delay(200)
            }
        }
    }

    private fun stopTicker() { ticker?.cancel(); ticker = null }
}