package nl.ecannemeijer.casacheck.data.transcribe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import nl.ecannemeijer.casacheck.CasaApp
import nl.ecannemeijer.casacheck.MainActivity
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.TxStatus
import nl.ecannemeijer.casacheck.data.recording.RecordingService
import android.app.PendingIntent

/** Background transcription (model download + whisper) with a progress notification. */
class TranscribeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val container get() = (applicationContext as CasaApp).container

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        val lang = inputData.getString(KEY_LANG) ?: "auto"
        val repo = container.recordings
        val rec = repo.get(id) ?: return Result.success()
        val title = rec.name
        runCatching { setForeground(foreground(title, applicationContext.getString(R.string.tx_preparing), null)) }
        repo.setTranscriptStatus(id, TxStatus.RUNNING)
        try {
            val model = WhisperModel(applicationContext)
            if (!model.isPresent()) {
                var lastPct = -1
                model.ensure(container.settings.current().translateWifiOnly) { p ->
                    val pct = (p * 100).toInt()
                    if (pct != lastPct) {
                        lastPct = pct
                        setProgressAsync(workDataOf(KEY_STAGE to STAGE_DOWNLOAD, KEY_PROGRESS to p))
                        notify(title, applicationContext.getString(R.string.tx_downloading_model, pct), pct)
                    }
                }
            }
            setProgress(workDataOf(KEY_STAGE to STAGE_TRANSCRIBE, KEY_PROGRESS to 0f))
            var lastPct = -1
            val result = Transcriber(model.file).transcribe(repo.fileOf(rec), rec.durationMs, lang, Transcriber.defaultThreads(), { p ->
                val pct = (p * 100).toInt()
                if (pct != lastPct) {
                    lastPct = pct
                    setProgressAsync(workDataOf(KEY_STAGE to STAGE_TRANSCRIBE, KEY_PROGRESS to p))
                    notify(title, applicationContext.getString(R.string.tx_transcribing, pct), pct)
                }
            }, { isStopped })
            if (isStopped) { repo.setTranscriptStatus(id, TxStatus.NONE); return Result.failure() }
            repo.saveTranscript(id, result.segments, result.language, model.spec.name)
            android.util.Log.i("CasaWhisper", "transcribed ${result.audioMs} ms audio in ${result.elapsedMs} ms, ${result.segments.size} segments, lang=${result.language}")
            return Result.success(workDataOf(KEY_AUDIO_MS to result.audioMs, KEY_ELAPSED_MS to result.elapsedMs))
        } catch (e: CancellationException) {
            repo.setTranscriptStatus(id, TxStatus.NONE)
            throw e
        } catch (e: WhisperModel.NeedsWifiException) {
            repo.setTranscriptStatus(id, TxStatus.FAILED, ERR_WIFI)
            return Result.failure()
        } catch (e: Exception) {
            repo.setTranscriptStatus(id, TxStatus.FAILED, e.message?.take(200) ?: e.javaClass.simpleName)
            return Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foreground("", applicationContext.getString(R.string.tx_preparing), null)

    private fun builder(title: String, text: String, pct: Int?): NotificationCompat.Builder {
        ensureChannel(applicationContext)
        val open = PendingIntent.getActivity(applicationContext, 2, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_mic)
            .setContentTitle(applicationContext.getString(R.string.tx_notif_title, title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text + "\n" + applicationContext.getString(R.string.tx_takes_a_while)))
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentIntent(open)
            .setProgress(100, pct ?: 0, pct == null)
            .addAction(0, applicationContext.getString(R.string.tx_cancel), WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))
    }

    private fun foreground(title: String, text: String, pct: Int?): ForegroundInfo {
        val n = builder(title, text, pct).build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(NOTIFICATION_ID, n)
    }

    private fun notify(title: String, text: String, pct: Int) {
        runCatching { setForegroundAsync(foreground(title, text, pct)) }
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_LANG = "lang"
        const val KEY_STAGE = "stage"
        const val KEY_PROGRESS = "progress"
        const val KEY_AUDIO_MS = "audioMs"
        const val KEY_ELAPSED_MS = "elapsedMs"
        const val STAGE_DOWNLOAD = "download"
        const val STAGE_TRANSCRIBE = "transcribe"
        const val ERR_WIFI = "wifi"
        const val CHANNEL = "transcription"
        const val NOTIFICATION_ID = 4712

        fun uniqueName(recordingId: Long) = "transcribe_$recordingId"

        fun enqueue(ctx: Context, recordingId: Long, language: String) {
            val req = OneTimeWorkRequestBuilder<TranscribeWorker>()
                .setInputData(workDataOf(KEY_ID to recordingId, KEY_LANG to language))
                .addTag("transcribe")
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(uniqueName(recordingId), ExistingWorkPolicy.KEEP, req)
        }

        fun cancel(ctx: Context, recordingId: Long) = WorkManager.getInstance(ctx).cancelUniqueWork(uniqueName(recordingId))

        fun ensureChannel(ctx: Context) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null)
                nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.tx_channel), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        }
    }
}