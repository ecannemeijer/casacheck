package nl.ecannemeijer.casacheck.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

/** Result of storing an image: permanent file + thumbnail in app-private storage (never only in cache). */
data class StoredImage(val path: String, val thumbPath: String, val sha256: String, val mime: String, val width: Int, val height: Int)

class InvalidImageException(msg: String) : IOException(msg)

/**
 * App-private permanent file storage:
 * filesDir/photos/<propertyUuid>/..., filesDir/thumbs/..., filesDir/docs/..., filesDir/reports/...
 */
class FileStore(private val context: Context) {
    val root: File get() = context.filesDir
    fun photosDir(propertyUuid: String) = File(root, "photos/$propertyUuid").apply { mkdirs() }
    fun thumbsDir(propertyUuid: String) = File(root, "thumbs/$propertyUuid").apply { mkdirs() }
    fun docsDir(propertyUuid: String) = File(root, "docs/$propertyUuid").apply { mkdirs() }
    fun reportsDir() = File(root, "reports").apply { mkdirs() }
    fun editedDir(propertyUuid: String) = File(root, "edited/$propertyUuid").apply { mkdirs() }
    fun cameraTemp(): File = File(context.cacheDir, "camera").apply { mkdirs() }.let { File(it, "cap_${System.nanoTime()}.jpg") }

    /** Relative path (to filesDir) used in the DB and in backups. */
    fun relative(f: File): String = f.absolutePath.removePrefix(root.absolutePath).trimStart('/')
    fun resolve(rel: String?): File? = rel?.let { if (it.startsWith("/")) File(it) else File(root, it) }

    fun storeImage(propertyUuid: String, bytes: ByteArray): StoredImage {
        if (bytes.size > MAX_IMAGE_BYTES) throw InvalidImageException("too large")
        val mime = sniffImageMime(bytes) ?: throw InvalidImageException("not an image")
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) throw InvalidImageException("cannot decode")
        if (opts.outWidth < 64 || opts.outHeight < 64) throw InvalidImageException("too small (icon/logo)")
        val sha = sha256(bytes)
        val ext = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; "image/gif" -> "gif"; else -> "jpg" }
        val file = File(photosDir(propertyUuid), "${UUID.randomUUID()}.$ext")
        file.writeBytes(bytes)
        val thumb = File(thumbsDir(propertyUuid), file.nameWithoutExtension + ".jpg")
        writeThumbnail(file, thumb)
        return StoredImage(relative(file), relative(thumb), sha, mime, opts.outWidth, opts.outHeight)
    }

    fun storeImage(propertyUuid: String, input: InputStream): StoredImage = storeImage(propertyUuid, readLimited(input, MAX_IMAGE_BYTES))

    fun storeImageFromUri(propertyUuid: String, uri: Uri): StoredImage =
        context.contentResolver.openInputStream(uri)?.use { storeImage(propertyUuid, it) } ?: throw IOException("cannot open $uri")

    fun storeDocument(propertyUuid: String, uri: Uri, displayName: String): Pair<String, String> {
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "document" }
        val f = File(docsDir(propertyUuid), "${System.currentTimeMillis()}_$safeName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = readLimited(input, MAX_DOC_BYTES)
            f.writeBytes(bytes)
            return relative(f) to sha256(bytes)
        } ?: throw IOException("cannot open")
    }

    fun delete(rel: String?) { resolve(rel)?.takeIf { it.exists() && it.absolutePath.startsWith(root.absolutePath) }?.delete() }

    fun writeThumbnail(src: File, dst: File, maxSide: Int = 480) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
        val bmp = BitmapFactory.decodeFile(src.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
        val rotated = rotateByExif(src, bmp)
        val scale = maxSide.toFloat() / maxOf(rotated.width, rotated.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt().coerceAtLeast(1), (rotated.height * scale).toInt().coerceAtLeast(1), true) else rotated
        dst.parentFile?.mkdirs()
        dst.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        if (out !== rotated) out.recycle()
        if (rotated !== bmp) rotated.recycle()
        bmp.recycle()
    }

    private fun rotateByExif(file: File, bmp: Bitmap): Bitmap = try {
        val deg = when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f
        }
        if (deg == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
    } catch (_: Exception) { bmp }

    companion object {
        const val MAX_IMAGE_BYTES = 15L * 1024 * 1024
        const val MAX_DOC_BYTES = 40L * 1024 * 1024

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        fun sha256(file: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { s -> val buf = ByteArray(64 * 1024); while (true) { val n = s.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        /** Checks magic bytes; the server's Content-Type is not trusted. */
        fun sniffImageMime(b: ByteArray): String? = when {
            b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte() -> "image/jpeg"
            b.size >= 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() && b[3] == 'G'.code.toByte() -> "image/png"
            b.size >= 12 && String(b, 0, 4, Charsets.US_ASCII) == "RIFF" && String(b, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
            b.size >= 6 && String(b, 0, 3, Charsets.US_ASCII) == "GIF" -> "image/gif"
            b.size >= 12 && String(b, 4, 4, Charsets.US_ASCII) == "ftyp" && String(b, 8, 4, Charsets.US_ASCII).let { it.startsWith("hei") || it.startsWith("mif1") || it == "avif" } -> "image/heif"
            else -> null
        }

        fun readLimited(input: InputStream, max: Long): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buf); if (n < 0) break
                total += n; if (total > max) throw InvalidImageException("too large")
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }
}
