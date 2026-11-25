package nl.ecannemeijer.casacheck.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import nl.ecannemeijer.casacheck.data.importer.OcrEngine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** On-device OCR with the bundled ML Kit Latin model (no Google Play Services, no network). */
class MlKitOcrEngine(private val context: Context) : OcrEngine {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognize(imagePath: String): String {
        val uri = if (imagePath.startsWith("content:") || imagePath.startsWith("file:")) Uri.parse(imagePath) else Uri.fromFile(File(imagePath))
        val image = InputImage.fromFilePath(context, uri)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
