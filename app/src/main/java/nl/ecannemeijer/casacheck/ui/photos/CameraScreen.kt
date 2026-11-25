package nl.ecannemeijer.casacheck.ui.photos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.ui.common.Thumb
import nl.ecannemeijer.casacheck.ui.nav.CameraTarget
import nl.ecannemeijer.casacheck.ui.nav.Nav

/** CameraX capture straight into permanent app storage, linked to the checkpoint/room/defect it was opened from. */
@Composable
fun CameraScreen(c: AppContainer, nav: Nav, target: CameraTarget) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) perm.launch(Manifest.permission.CAMERA) }
    var flash by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }
    var lastThumb by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        scope.launch {
            uris.forEach { u ->
                runCatching { c.photos.addFromUri(target.propertyId, u, target.kind, target.itemId, target.roomId, target.defectId, target.moistureId, target.crackId) }
                    .onSuccess { id -> count++; lastThumb = c.photos.get(id)?.thumbPath }
            }
        }
    }
    LaunchedEffect(flash) { capture.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            val previewView = remember { PreviewView(ctx) }
            AndroidView({ previewView }, Modifier.fillMaxSize())
            DisposableEffect(Unit) {
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    try {
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                        provider.unbindAll()
                        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    } catch (e: Exception) { error = e.message ?: "camera" }
                }, ContextCompat.getMainExecutor(ctx))
                onDispose { runCatching { future.get().unbindAll() } }
            }
        } else Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.camera_permission_needed), color = Color.White)
            Button(onClick = { perm.launch(Manifest.permission.CAMERA) }) { Text(stringResource(R.string.allow_camera)) }
            OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(stringResource(R.string.from_gallery), color = Color.White) }
        }
        Row(Modifier.statusBarsPadding().padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
            Text(if (count > 0) stringResource(R.string.photos_saved_n, count) else "", Modifier.weight(1f), color = Color.White)
            IconButton(onClick = { flash = !flash }) { Icon(if (flash) Icons.Filled.FlashOn else Icons.Filled.FlashOff, stringResource(R.string.flash), tint = Color.White) }
        }
        error?.let { Text(it, Modifier.align(Alignment.Center), color = Color.White) }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(64.dp)) { lastThumb?.let { Thumb(CasaFiles.file(it), Modifier.fillMaxSize()) } }
            FilledIconButton(enabled = granted && !busy, onClick = {
                busy = true
                val f = c.files.cameraTemp()
                capture.takePicture(ImageCapture.OutputFileOptions.Builder(f).build(), ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        scope.launch {
                            runCatching { c.photos.addFromFile(target.propertyId, f, target.kind, target.itemId, target.roomId, target.defectId, target.moistureId, target.crackId) }
                                .onSuccess { id -> count++; lastThumb = c.photos.get(id)?.thumbPath }
                                .onFailure { e -> error = e.message }
                            busy = false
                        }
                    }
                    override fun onError(exception: ImageCaptureException) { error = exception.message; busy = false; f.delete() }
                })
            }, modifier = Modifier.size(84.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)) {
                Box(Modifier.size(64.dp).background(if (busy) Color.Gray else MaterialTheme.colorScheme.primary, CircleShape))
            }
            IconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.size(64.dp)) {
                Icon(Icons.Filled.PhotoLibrary, stringResource(R.string.from_gallery), tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        if (count > 0) Button(onClick = nav::back, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp)) { Text(stringResource(R.string.done)) }
    }
}
