package nl.ecannemeijer.casacheck.data.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.ecannemeijer.casacheck.CasaApp
import nl.ecannemeijer.casacheck.MainActivity
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.audio.AudioMath
import java.io.File

/**
 * Foreground service (type microphone) that records a viewing. Keeps running while the user navigates through the app,
 * switches apps or turns the screen off. Audio: AAC-LC mono 16 kHz 32 kbit/s in an ADTS stream (crash-safe), remuxed to .m4a on stop.
 */
class RecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recorder: MediaRecorder? = null
    private var wake: PowerManager.WakeLock? = null
    private var modeListener: Any? = null
    private var watchJob: Job? = null
    private var starting = false
    private val container get() = (application as CasaApp).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent.getLongExtra(EXTRA_PID, -1))
            ACTION_PAUSE -> pause(auto = false)
            ACTION_RESUME -> resume()
            ACTION_STOP -> stop(R.string.rec_saved)
            ACTION_MARK -> mark()
            else -> if (Recorder.state.value == null && !starting) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(pid: Long) {
        // startForeground must be called promptly after startForegroundService, even when we refuse.
        startFg(baseBuilder(getString(R.string.rec_notif_starting)).build())
        if (Recorder.state.value != null || starting || pid < 0) { if (Recorder.state.value == null && !starting) finish(); return }
        starting = true
        scope.launch {
            try {
                if (container.recordings.freeBytes() < RecordingRepository.MIN_FREE_BYTES) { fail(R.string.rec_low_storage); return@launch }
                val title = container.db.propertyDao().get(pid)?.title ?: ""
                val (rec, file) = container.recordings.begin(pid, AudioMath.defaultName(getString(R.string.rec_default_name), System.currentTimeMillis()))
                val r = try { newRecorder(file) } catch (e: Exception) {
                    container.recordings.delete(rec.id); fail(R.string.rec_error); return@launch
                }
                recorder = r
                Recorder.mutable.value = RecState(rec.id, pid, title, SystemClock.elapsedRealtime(), 0L)
                container.player.pause()
                wake = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CasaCheck:recording").apply { setReferenceCounted(false); acquire(12 * 3600_000L) }
                registerCallWatcher()
                watchJob = scope.launch { watchStorage() }
                updateNotification()
            } finally { starting = false }
        }
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(file: File): MediaRecorder {
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(AudioMath.SAMPLE_RATE)
            r.setAudioEncodingBitRate(AudioMath.BIT_RATE)
            r.setOutputFile(file.absolutePath)
            r.setOnErrorListener { _, _, _ -> stop(R.string.rec_error) }
            r.prepare()
            r.start()
            return r
        } catch (e: Exception) { r.release(); file.delete(); throw e }
    }

    private fun pause(auto: Boolean) {
        val s = Recorder.state.value ?: return
        if (s.paused) return
        runCatching { recorder?.pause() }.onFailure { return }
        Recorder.mutable.value = s.copy(paused = true, autoPaused = auto, accumulatedMs = s.elapsedMs())
        updateNotification()
    }

    private fun resume() {
        val s = Recorder.state.value ?: return
        if (!s.paused) return
        runCatching { recorder?.resume() }.onFailure { return }
        Recorder.mutable.value = s.copy(paused = false, autoPaused = false, segmentStart = SystemClock.elapsedRealtime())
        updateNotification()
    }

    private fun mark() {
        val s = Recorder.state.value ?: return
        val pos = s.elapsedMs()
        val cp = Recorder.checkpoint.value?.takeIf { it.propertyId == s.propertyId }
        Recorder.mutable.value = s.copy(markers = s.markers + 1)
        scope.launch { container.recordings.addMarker(s.recordingId, pos, cp?.title, cp?.itemId) }
        Toast.makeText(this, getString(R.string.rec_marked, AudioMath.formatTime(pos)), Toast.LENGTH_SHORT).show()
        updateNotification()
    }

    private fun stop(message: Int) {
        val s = Recorder.state.value ?: run { if (!starting) finish(); return }
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        Recorder.mutable.value = null
        cleanupWatchers()
        scope.launch {
            withContext(NonCancellable) { runCatching { container.recordings.finalizeRecording(s.recordingId) } }
            Recorder.messages.value = message
            Toast.makeText(this@RecordingService, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun fail(message: Int) {
        Recorder.messages.value = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun watchStorage() {
        while (scope.isActive && Recorder.state.value != null) {
            delay(15_000)
            if (container.recordings.freeBytes() < RecordingRepository.STOP_FREE_BYTES) { stop(R.string.rec_low_storage_stopped); return }
            // Keep the notification text fresh while paused (the chronometer only runs while recording).
        }
    }

    /** Pauses automatically during a phone/VoIP call and continues afterwards (no phone permission needed). */
    private fun registerCallWatcher() {
        if (Build.VERSION.SDK_INT < 31) return
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val l = AudioManager.OnModeChangedListener { mode ->
            val inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE
            val s = Recorder.state.value ?: return@OnModeChangedListener
            if (inCall && !s.paused) pause(auto = true)
            else if (!inCall && s.paused && s.autoPaused) resume()
        }
        am.addOnModeChangedListener(mainExecutor, l)
        modeListener = l
    }

    private fun cleanupWatchers() {
        watchJob?.cancel(); watchJob = null
        if (Build.VERSION.SDK_INT >= 31) (modeListener as? AudioManager.OnModeChangedListener)?.let { (getSystemService(Context.AUDIO_SERVICE) as AudioManager).removeOnModeChangedListener(it) }
        modeListener = null
        runCatching { if (wake?.isHeld == true) wake?.release() }
        wake = null
    }

    override fun onDestroy() {
        // The system is killing us while recording: finalise what we have.
        Recorder.state.value?.let { s ->
            runCatching { recorder?.stop() }; runCatching { recorder?.release() }; recorder = null
            Recorder.mutable.value = null
            container.scope.launch { runCatching { container.recordings.finalizeRecording(s.recordingId) } }
        }
        cleanupWatchers()
        scope.cancel()
        super.onDestroy()
    }

    private fun startFg(n: Notification) {
        ensureChannel(this)
        ServiceCompat.startForeground(this, NOTIFICATION_ID, n, if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)
    }

    private fun pi(action: String, code: Int) = PendingIntent.getService(this, code, Intent(this, RecordingService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun baseBuilder(text: String): NotificationCompat.Builder {
        val open = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        Recorder.state.value?.let { open.putExtra(EXTRA_OPEN_PROPERTY, it.propertyId) }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_mic)
            .setContentTitle(getString(R.string.rec_notif_title))
            .setContentText(text)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun updateNotification() {
        val s = Recorder.state.value ?: return
        val elapsed = s.elapsedMs()
        val text = when {
            s.autoPaused -> getString(R.string.rec_notif_paused_call, AudioMath.formatTime(elapsed))
            s.paused -> getString(R.string.rec_notif_paused, AudioMath.formatTime(elapsed))
            else -> s.propertyTitle
        }
        val b = baseBuilder(text).setSubText(if (s.markers > 0) getString(R.string.rec_markers_count, s.markers) else null)
        if (!s.paused) b.setUsesChronometer(true).setShowWhen(true).setWhen(System.currentTimeMillis() - elapsed) else b.setShowWhen(false)
        if (s.paused) b.addAction(0, getString(R.string.rec_resume), pi(ACTION_RESUME, 11)) else b.addAction(0, getString(R.string.rec_pause), pi(ACTION_PAUSE, 12))
        b.addAction(0, getString(R.string.rec_mark), pi(ACTION_MARK, 13))
        b.addAction(0, getString(R.string.rec_stop), pi(ACTION_STOP, 14))
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, b.build())
    }

    companion object {
        const val ACTION_START = "nl.ecannemeijer.casacheck.rec.START"
        const val ACTION_PAUSE = "nl.ecannemeijer.casacheck.rec.PAUSE"
        const val ACTION_RESUME = "nl.ecannemeijer.casacheck.rec.RESUME"
        const val ACTION_STOP = "nl.ecannemeijer.casacheck.rec.STOP"
        const val ACTION_MARK = "nl.ecannemeijer.casacheck.rec.MARK"
        const val EXTRA_PID = "pid"
        const val EXTRA_OPEN_PROPERTY = "open_recordings_property"
        const val CHANNEL = "recording"
        const val NOTIFICATION_ID = 4711

        fun ensureChannel(ctx: Context) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null)
                nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.rec_channel), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        }
    }
}