package nl.ecannemeijer.casacheck.data.importer

import android.content.Context
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.ecannemeijer.casacheck.CasaApp
import nl.ecannemeijer.casacheck.data.db.ImportHistory
import nl.ecannemeijer.casacheck.data.db.ImportStatus
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.data.db.PropertyPhoto
import nl.ecannemeijer.casacheck.data.files.InvalidImageException
import java.io.File
import java.io.IOException

/** Hosts from which listing photos may be downloaded (the sites' own image CDNs). */
object ImageHosts {
    private val allowed = listOf(Regex("^img\\d*\\.idealista\\.(it|com|pt)$"), Regex("^([a-z0-9-]+\\.)?im-cdn\\.it$"), Regex("^(www\\.)?immobiliare\\.it$"))
    fun isAllowed(host: String?): Boolean = host?.lowercase()?.let { h -> allowed.any { it.matches(h) } } ?: false
}

@Serializable
data class PhotoJob(val propertyId: Long, val photos: List<ImportPhoto>, val mainPosition: Int?, val source: String, val historyUrl: String?)

/**
 * Downloads selected listing photos in the background (WorkManager): checks size, magic bytes and decodability,
 * deduplicates by SHA-256, keeps the gallery order and original URL, creates thumbnails and stores everything in filesDir.
 */
class PhotoDownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CasaApp
        val c = app.container
        val jobFile = inputData.getString(KEY_JOB)?.let { File(it) } ?: return Result.failure()
        if (!jobFile.exists()) return Result.failure()
        val job = Json.decodeFromString<PhotoJob>(jobFile.readText())
        val property = c.db.propertyDao().get(job.propertyId) ?: run { jobFile.delete(); return Result.success() }
        val fetcher = HttpFetcher()
        var imported = 0; var failed = 0
        job.photos.sortedBy { it.position }.forEachIndexed { index, ph ->
            if (isStopped) return Result.retry()
            setProgress(workDataOf(KEY_DONE to index, KEY_TOTAL to job.photos.size))
            if (c.db.photoDao().byOriginalUrl(job.propertyId, ph.url) != null) { imported++; return@forEachIndexed }
            try {
                val (bytes, _) = fetcher.fetchBytes(ph.url, HttpFetcher.MAX_IMAGE_BYTES, ImageHosts::isAllowed)
                val stored = c.files.storeImage(property.uuid, bytes)
                c.db.withTransaction {
                    val dup = c.db.photoDao().bySha(job.propertyId, stored.sha256)
                    if (dup != null) { c.files.delete(stored.path); c.files.delete(stored.thumbPath); return@withTransaction }
                    val photoId = c.db.photoDao().insert(Photo(
                        propertyId = job.propertyId, kind = if (ph.isFloorplan) PhotoKind.FLOORPLAN else PhotoKind.LISTING,
                        filePath = stored.path, thumbPath = stored.thumbPath, originalUrl = ph.url, sha256 = stored.sha256,
                        mimeType = stored.mime, width = stored.width, height = stored.height, caption = ph.caption,
                        sortOrder = ph.position, includeInReport = false,
                    ))
                    c.db.photoDao().insertListingPhoto(PropertyPhoto(propertyId = job.propertyId, photoId = photoId, sourceUrl = ph.url,
                        position = ph.position, tag = ph.caption, isFloorplan = ph.isFloorplan))
                    val current = c.db.propertyDao().get(job.propertyId)
                    if (current?.mainPhotoId == null && !ph.isFloorplan && (job.mainPosition == null || job.mainPosition == ph.position)) {
                        c.db.propertyDao().setMainPhoto(job.propertyId, photoId)
                    }
                }
                imported++
            } catch (e: InvalidImageException) { failed++ } catch (e: IOException) { failed++ }
        }
        c.db.listingDao().insertImport(ImportHistory(propertyId = job.propertyId, source = job.source + "-photos", url = job.historyUrl,
            status = if (failed == 0) ImportStatus.SUCCESS else if (imported > 0) ImportStatus.PARTIAL else ImportStatus.FAILED,
            message = if (failed > 0) "$failed failed" else null, photosFound = job.photos.size, photosImported = imported))
        jobFile.delete()
        return Result.success(workDataOf(KEY_DONE to imported, KEY_TOTAL to job.photos.size, KEY_FAILED to failed))
    }

    companion object {
        const val KEY_JOB = "job"; const val KEY_DONE = "done"; const val KEY_TOTAL = "total"; const val KEY_FAILED = "failed"
        fun uniqueName(propertyId: Long) = "photos-$propertyId"

        fun enqueue(context: Context, job: PhotoJob) {
            val dir = File(context.filesDir, "pending").apply { mkdirs() }
            val f = File(dir, "photos_${job.propertyId}_${System.currentTimeMillis()}.json")
            f.writeText(Json.encodeToString(job))
            val req = OneTimeWorkRequestBuilder<PhotoDownloadWorker>()
                .setInputData(workDataOf(KEY_JOB to f.absolutePath))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag("photos")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(uniqueName(job.propertyId), ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }
    }
}
