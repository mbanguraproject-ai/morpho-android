package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AColor
import android.graphics.Paint as AndroidPaint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun PdfSignerTool(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val strokes = remember { mutableStateListOf<MutableList<Offset>>() }
    // normalized tap position on the page (0..1, 0..1) where the signature center goes
    var placeNorm by remember { mutableStateOf<Offset?>(null) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        uri = u; msg = ""; strokes.clear(); placeNorm = null; pageBitmap = null
        if (u != null) {
            // preview the LAST page (where signatures usually go)
            val pages = renderPdf(ctx, u, 1000)
            pageBitmap = pages.lastOrNull()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected \u2713", accent) {
            picker.launch(arrayOf("application/pdf"))
        }

        val bmp = pageBitmap
        if (bmp != null) {
            Text("1. Draw your signature.  2. Tap where it should go on the page.", color = InkSoft, fontSize = 13.sp)

            // signature pad
            Box(
                Modifier.fillMaxWidth().height(160.dp).clip(Shape.card)
                    .background(PaperSunk).border(1.5.dp, accent.copy(alpha = 0.3f), Shape.card)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { strokes.add(mutableListOf(it)) },
                            onDrag = { change, _ -> strokes.lastOrNull()?.add(change.position); change.consume() }
                        )
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    strokes.forEach { pts ->
                        if (pts.size > 1) {
                            val path = Path().apply {
                                moveTo(pts[0].x, pts[0].y)
                                pts.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, accent, style = Stroke(width = 5f, cap = StrokeCap.Round))
                        }
                    }
                }
                if (strokes.isEmpty()) {
                    Text("Sign here", color = InkFaint, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
            Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
                .clickable { strokes.clear() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text("Clear signature", color = accent, fontSize = 14.sp)
            }

            // page preview with tap-to-place
            Text("Tap the page to place your signature:", color = InkSoft, fontSize = 13.sp)
            Box(
                Modifier.fillMaxWidth()
                    .onGloballyPositioned { previewSize = it.size }
                    .pointerInput(bmp) {
                        detectTapGestures { off ->
                            if (previewSize.width > 0 && previewSize.height > 0) {
                                placeNorm = Offset(off.x / previewSize.width, off.y / previewSize.height)
                            }
                        }
                    }
            ) {
                Image(bmp.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                // marker at the tapped spot
                placeNorm?.let { pn ->
                    val mx = pn.x * previewSize.width
                    val my = pn.y * previewSize.height
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(accent, radius = 14f, center = Offset(mx, my))
                        drawCircle(Color.White, radius = 6f, center = Offset(mx, my))
                    }
                }
            }

            if (busy) ProcessingCard("Signing your PDF...", accent)
            else ToolButton("Sign & save", accent) {
                if (strokes.isEmpty()) { msg = "Draw a signature first."; return@ToolButton }
                if (placeNorm == null) { msg = "Tap the page to place your signature."; return@ToolButton }
                busy = true
                val u = uri!!
                val padStrokes = strokes.map { it.toList() }
                val pos = placeNorm!!
                scope.launch {
                    val bytes = withContext(Dispatchers.Default) { signPdf(ctx, u, padStrokes, pos) }
                    if (bytes != null) { savePdfToDownloads(ctx, bytes, "signed_${System.currentTimeMillis()}"); msg = "Saved to Download/Morpho \u2713" }
                    else msg = "\u26a0 Could not sign this PDF."
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

// composite signature at normalized position on the LAST page, rebuild PDF
private fun signPdf(ctx: android.content.Context, uri: Uri, strokes: List<List<Offset>>, posNorm: Offset): ByteArray? {
    return try {
        val pages = renderPdf(ctx, uri, 1240)
        if (pages.isEmpty()) return null
        val lastIdx = pages.size - 1
        val doc = PdfDocument()
        pages.forEachIndexed { idx, page ->
            val out = page.copy(Bitmap.Config.ARGB_8888, true)
            if (idx == lastIdx) {
                val c = AndroidCanvas(out)
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AColor.rgb(11, 13, 18)  // ink
                    strokeWidth = out.width * 0.005f
                    style = AndroidPaint.Style.STROKE
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                // signature box: 32% page width, centered on the tapped point
                val boxW = out.width * 0.32f
                val boxH = out.height * 0.10f
                val cx = posNorm.x * out.width
                val cy = posNorm.y * out.height
                val boxX = (cx - boxW / 2f).coerceIn(0f, out.width - boxW)
                val boxY = (cy - boxH / 2f).coerceIn(0f, out.height - boxH)
                val allPts = strokes.flatten()
                if (allPts.isNotEmpty()) {
                    val minX = allPts.minOf { it.x }; val maxX = allPts.maxOf { it.x }
                    val minY = allPts.minOf { it.y }; val maxY = allPts.maxOf { it.y }
                    val spanX = (maxX - minX).coerceAtLeast(1f); val spanY = (maxY - minY).coerceAtLeast(1f)
                    strokes.forEach { pts ->
                        if (pts.size > 1) {
                            val path = android.graphics.Path()
                            pts.forEachIndexed { i, p ->
                                val nx = boxX + (p.x - minX) / spanX * boxW
                                val ny = boxY + (p.y - minY) / spanY * boxH
                                if (i == 0) path.moveTo(nx, ny) else path.lineTo(nx, ny)
                            }
                            c.drawPath(path, paint)
                        }
                    }
                }
            }
            val info = PdfDocument.PageInfo.Builder(out.width, out.height, doc.pages.size + 1).create()
            val p = doc.startPage(info)
            p.canvas.drawBitmap(out, 0f, 0f, null)
            doc.finishPage(p)
        }
        val bos = ByteArrayOutputStream(); doc.writeTo(bos); doc.close()
        bos.toByteArray()
    } catch (e: Exception) { null }
}

@Composable
private fun PickRow(label: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.08f))
            .clickable { onClick() }.padding(Space.lg),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(label, color = accent, fontSize = 15.sp)
    }
}
