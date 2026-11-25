package nl.ecannemeijer.casacheck.ui.photos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import java.io.File

/** Zoom/pan state; sharing one instance between two images gives synchronised zoom (photo compare). */
@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    fun reset() { scale = 1f; offset = Offset.Zero }
}

@Composable
fun ZoomableImage(file: File?, state: ZoomState, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(modifier
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            // Only consume gestures when zooming (2 fingers) or already zoomed in, so a pager can still swipe at scale 1.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val multi = event.changes.count { it.pressed } > 1
                    if (multi || state.scale > 1f) {
                        val zoom = event.calculateZoom(); val pan = event.calculatePan()
                        state.scale = (state.scale * zoom).coerceIn(1f, 8f)
                        state.offset = if (state.scale == 1f) Offset.Zero else state.offset + pan
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
        .pointerInput(enabled) { if (enabled) detectTapGestures(onDoubleTap = { if (state.scale > 1f) state.reset() else state.scale = 2.5f }) }) {
        AsyncImage(file, null, Modifier.matchParentSize().graphicsLayer {
            scaleX = state.scale; scaleY = state.scale; translationX = state.offset.x; translationY = state.offset.y
        }, contentScale = ContentScale.Fit)
    }
}


/** Decodes an image downscaled to [maxSide] and rotated according to EXIF. */
fun loadBitmap(file: File, maxSide: Int): Bitmap? {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, o)
    if (o.outWidth <= 0) return null
    var sample = 1
    while (o.outWidth / (sample * 2) >= maxSide || o.outHeight / (sample * 2) >= maxSide) sample *= 2
    val bmp = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val deg = try {
        when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f
        }
    } catch (_: Exception) { 0f }
    return if (deg == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
}
