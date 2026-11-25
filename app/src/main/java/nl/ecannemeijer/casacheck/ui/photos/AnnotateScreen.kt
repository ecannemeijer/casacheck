package nl.ecannemeijer.casacheck.ui.photos

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.ecannemeijer.casacheck.AppContainer
import nl.ecannemeijer.casacheck.CasaFiles
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.ui.common.BackTopBar
import nl.ecannemeijer.casacheck.ui.nav.Nav
import java.io.ByteArrayOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class ShapeType { ARROW, CIRCLE, RECT, FREEHAND, TEXT }

/** One annotation shape; coordinates are normalised (0..1) to the image so they survive any resolution. */
@Serializable
data class Shape(val type: ShapeType, val color: Int, val width: Float = 0.008f, val pts: List<Float>, val text: String? = null)

@Serializable
data class AnnotationDoc(val version: Int = 1, val shapes: List<Shape>)

val annotationJson = Json { ignoreUnknownKeys = true }

/** Draws a shape onto an Android canvas into the image rectangle (ox, oy, w, h). Used for screen and export. */
fun drawShape(canvas: android.graphics.Canvas, s: Shape, ox: Float, oy: Float, w: Float, h: Float, highlight: Boolean = false) {
    val stroke = (s.width * w).coerceAtLeast(2f)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.color; strokeWidth = stroke; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    fun x(i: Int) = ox + s.pts[i] * w
    fun y(i: Int) = oy + s.pts[i] * h
    if (highlight) {
        val hp = Paint(p).apply { color = android.graphics.Color.WHITE; strokeWidth = stroke + 6f; alpha = 160 }
        drawShapeWith(canvas, s, hp, ::x, ::y, stroke + 6f)
    }
    drawShapeWith(canvas, s, p, ::x, ::y, stroke)
}

private fun drawShapeWith(canvas: android.graphics.Canvas, s: Shape, p: Paint, x: (Int) -> Float, y: (Int) -> Float, stroke: Float) {
    if (s.pts.size < 2) return
    when (s.type) {
        ShapeType.ARROW -> if (s.pts.size >= 4) {
            val x0 = x(0); val y0 = y(1); val x1 = x(2); val y1 = y(3)
            canvas.drawLine(x0, y0, x1, y1, p)
            val a = atan2((y1 - y0).toDouble(), (x1 - x0).toDouble()); val len = stroke * 5f
            for (d in listOf(Math.toRadians(150.0), Math.toRadians(-150.0))) canvas.drawLine(x1, y1, x1 + (len * cos(a + d)).toFloat(), y1 + (len * sin(a + d)).toFloat(), p)
        }
        ShapeType.CIRCLE -> if (s.pts.size >= 4) canvas.drawOval(RectF(minOf(x(0), x(2)), minOf(y(1), y(3)), maxOf(x(0), x(2)), maxOf(y(1), y(3))), p)
        ShapeType.RECT -> if (s.pts.size >= 4) canvas.drawRect(RectF(minOf(x(0), x(2)), minOf(y(1), y(3)), maxOf(x(0), x(2)), maxOf(y(1), y(3))), p)
        ShapeType.FREEHAND -> {
            val path = Path(); path.moveTo(x(0), y(1))
            var i = 2; while (i + 1 < s.pts.size) { path.lineTo(x(i), y(i + 1)); i += 2 }
            canvas.drawPath(path, p)
        }
        ShapeType.TEXT -> {
            val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = stroke * 7f; setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK) }
            canvas.drawText(s.text ?: "", x(0), y(1), tp)
        }
    }
}

private fun Shape.distanceTo(nx: Float, ny: Float): Float {
    val xs = pts.filterIndexed { i, _ -> i % 2 == 0 }; val ys = pts.filterIndexed { i, _ -> i % 2 == 1 }
    val cx = nx.coerceIn(xs.min(), xs.max()); val cy = ny.coerceIn(ys.min(), ys.max())
    return hypot(nx - cx, ny - cy)
}

val ANNOT_COLORS = listOf(Color(0xFFE53935), Color(0xFFFFD600), Color(0xFF1E88E5), Color(0xFF43A047), Color.White, Color.Black)

@Composable
fun AnnotateScreen(c: AppContainer, nav: Nav, photoId: Long) {
    var photo by remember { mutableStateOf<Photo?>(null) }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var shapes by remember { mutableStateOf(listOf<Shape>()) }
    val undo = remember { ArrayDeque<List<Shape>>() }
    val redo = remember { ArrayDeque<List<Shape>>() }
    var tool by remember { mutableStateOf(ShapeType.ARROW as ShapeType?) }
    var color by remember { mutableStateOf(ANNOT_COLORS[0]) }
    var drawing by remember { mutableStateOf<Shape?>(null) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var textAt by remember { mutableStateOf<Offset?>(null) }
    var saving by remember { mutableStateOf(false) }
    var premark by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(photoId) {
        val p = c.photos.get(photoId) ?: return@LaunchedEffect
        photo = p
        bmp = withContext(Dispatchers.IO) { CasaFiles.file(p.filePath)?.let { loadBitmap(it, 2048) } }
        c.photos.annotation(photoId)?.let { a -> runCatching { shapes = annotationJson.decodeFromString<AnnotationDoc>(a.shapesJson).shapes } }
    }
    fun commit(n: List<Shape>) { undo.addLast(shapes); if (undo.size > 100) undo.removeFirst(); redo.clear(); shapes = n }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.annotate), nav::back) {
        IconButton(enabled = undo.isNotEmpty(), onClick = { redo.addLast(shapes); shapes = undo.removeLast(); selected = null }) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo)) }
        IconButton(enabled = redo.isNotEmpty(), onClick = { undo.addLast(shapes); shapes = redo.removeLast(); selected = null }) { Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.redo)) }
        IconButton(enabled = !saving && photo != null && bmp != null, onClick = {
            val p = photo!!; val b = bmp!!
            saving = true
            scope.launch {
                if (shapes.isEmpty()) c.photos.clearAnnotation(p) else {
                    val bytes = withContext(Dispatchers.Default) {
                        val out = b.copy(Bitmap.Config.ARGB_8888, true)
                        val cv = android.graphics.Canvas(out)
                        shapes.forEach { drawShape(cv, it, 0f, 0f, out.width.toFloat(), out.height.toFloat()) }
                        ByteArrayOutputStream().also { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
                    }
                    c.photos.saveAnnotation(p, annotationJson.encodeToString(AnnotationDoc(shapes = shapes)), bytes)
                }
                saving = false
                nav.back()
            }
        }) { Icon(Icons.Filled.Save, stringResource(R.string.save)) }
    } }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(null to Icons.Filled.TouchApp, ShapeType.ARROW to Icons.Filled.NorthEast, ShapeType.CIRCLE to Icons.Filled.PanoramaFishEye, ShapeType.RECT to Icons.Filled.CheckBoxOutlineBlank,
                    ShapeType.FREEHAND to Icons.Filled.Draw, ShapeType.TEXT to Icons.Filled.TextFields).forEach { (t, ic) ->
                    FilledIconToggleButton(checked = tool == t, onCheckedChange = { tool = t; selected = null }) { Icon(ic, t?.name ?: stringResource(R.string.select)) }
                }
                ANNOT_COLORS.forEach { col ->
                    Box(Modifier.size(32.dp).background(col, CircleShape).border(if (col == color) 3.dp else 1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { color = col; selected?.let { i -> commit(shapes.toMutableList().also { it[i] = it[i].copy(color = col.toArgb()) }) } })
                }
                IconButton(enabled = selected != null, onClick = { selected?.let { i -> commit(shapes.filterIndexed { j, _ -> j != i }) }; selected = null }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
            }
            val b = bmp
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                if (b != null) {
                    val img = remember(b) { b.asImageBitmap() }
                    Canvas(Modifier.fillMaxSize()
                        .pointerInput(tool, b) {
                            fun fit(): Pair<Offset, Float> { val s = minOf(size.width / b.width.toFloat(), size.height / b.height.toFloat()); return Offset((size.width - b.width * s) / 2, (size.height - b.height * s) / 2) to s }
                            fun norm(o: Offset): Offset { val (org, s) = fit(); return Offset(((o.x - org.x) / (b.width * s)).coerceIn(0f, 1f), ((o.y - org.y) / (b.height * s)).coerceIn(0f, 1f)) }
                            if (tool == null || tool == ShapeType.TEXT) detectTapGestures { o ->
                                val n = norm(o)
                                if (tool == ShapeType.TEXT) textAt = n
                                else selected = shapes.indices.minByOrNull { shapes[it].distanceTo(n.x, n.y) }?.takeIf { shapes[it].distanceTo(n.x, n.y) < 0.04f }
                            } else detectDragGestures(
                                onDragStart = { o -> val n = norm(o); drawing = Shape(tool!!, color.toArgb(), pts = listOf(n.x, n.y, n.x, n.y)) },
                                onDrag = { ch, _ -> val n = norm(ch.position); drawing = drawing?.let { d -> if (d.type == ShapeType.FREEHAND) d.copy(pts = d.pts + listOf(n.x, n.y)) else d.copy(pts = d.pts.take(2) + listOf(n.x, n.y)) } },
                                onDragEnd = { drawing?.let { d -> commit(shapes + d) }; drawing = null },
                                onDragCancel = { drawing = null })
                        }) {
                        val s = minOf(size.width / b.width, size.height / b.height)
                        val w = b.width * s; val h = b.height * s; val ox = (size.width - w) / 2; val oy = (size.height - h) / 2
                        drawImage(img, dstOffset = IntOffset(ox.toInt(), oy.toInt()), dstSize = IntSize(w.toInt(), h.toInt()))
                        drawIntoCanvas { cv ->
                            shapes.forEachIndexed { i, sh -> drawShape(cv.nativeCanvas, sh, ox, oy, w, h, highlight = i == selected) }
                            drawing?.let { drawShape(cv.nativeCanvas, it, ox, oy, w, h) }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.annotate_note), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(enabled = photo != null, onClick = { premark = true }) { Text(stringResource(R.string.make_attention_point)) }
            }
        }
    }
    textAt?.let { at ->
        var t by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { textAt = null }, title = { Text(stringResource(R.string.add_text)) }, text = { OutlinedTextField(t, { t = it }, singleLine = true) },
            confirmButton = { TextButton(enabled = t.isNotBlank(), onClick = { commit(shapes + Shape(ShapeType.TEXT, color.toArgb(), pts = listOf(at.x, at.y), text = t.trim())); textAt = null }) { Text(stringResource(R.string.add)) } },
            dismissButton = { TextButton(onClick = { textAt = null }) { Text(stringResource(R.string.cancel)) } })
    }
    if (premark) photo?.let { p -> PremarkDialog(c, p, onCreated = { id -> nav.item(id) }) { premark = false } }
}
