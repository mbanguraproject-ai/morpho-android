package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

fun hasExtraTool(id: String): Boolean = id in setOf(
    "favicon-generator","pdf-crop","pdf-reorder-pages"
)

@Composable
fun ExtraTool(id: String, accent: Color) {
    when (id) {
        "favicon-generator" -> FaviconTool(accent)
        "pdf-crop" -> PdfCropTool(accent)
        "pdf-reorder-pages" -> PdfReorderTool(accent)
    }
}

// ---- Favicon: export standard sizes from one square image ----
@Composable
private fun FaviconTool(accent: Color) {
    val ctx = LocalContext.current
    var src by remember { mutableStateOf<Bitmap?>(null) }
    val sizes = listOf(16, 32, 48, 64, 128, 180, 192, 512)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u != null) src = decodeBitmap(ctx, u, 1024)
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        cc.devbangs.morpho.workflow.WorkflowBus.consume()?.let { src = decodeBitmapBytes(it.bytes, 1024) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        ImagePickPreview(
            bitmap = src,
            accent = accent,
            onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onClear = { src = null }
        )
        src?.let { bmp ->
            Text("Exports: ${sizes.joinToString("×, ") { "$it" }}× px", color = InkSoft, fontSize = 13.sp)
            ToolButton("Save all sizes to gallery", accent) {
                var ok = 0
                sizes.forEach { sz ->
                    val scaled = Bitmap.createScaledBitmap(bmp, sz, sz, true)
                    if (saveToGallery(ctx, scaled, "favicon_${sz}",
                            Bitmap.CompressFormat.PNG, 100, report = false)) ok++
                }
                // The count was previously computed and then discarded, so a
                // partial failure looked identical to a complete success.
                reportSave(
                    ctx, ok > 0, "Icons ready",
                    if (ok == sizes.size) "All ${sizes.size} icon sizes were saved."
                    else "$ok of ${sizes.size} icon sizes were saved.",
                    if (ok == sizes.size) "Saved ${sizes.size} sizes to Pictures/Morpho"
                    else "Saved $ok of ${sizes.size} sizes",
                    "Couldn't save the icons"
                )
            }
        }
    }
}

// ---- PDF crop: trim margins by % on all pages ----
@Composable
private fun PdfCropTool(accent: Color) {
    val ctx = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var margin by remember { mutableStateOf(8) }
    var loadError by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<ByteArray?>(null) }
    var outName by remember { mutableStateOf("") }
    fun load(u: Uri) {
        pages = renderPdf(ctx, u, 900)
        loadError = if (pages.isEmpty()) pdfFailureReason(ctx, u) else ""
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) load(u)
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        cc.devbangs.morpho.workflow.WorkflowBus.consume()?.let { load(bytesToTempUri(ctx, it.bytes, it.mime)) }
    }
    val cropped = remember(pages, margin) {
        pages.map { p ->
            val mx = (p.width * margin / 100f).toInt(); val my = (p.height * margin / 100f).toInt()
            if (p.width - 2*mx > 0 && p.height - 2*my > 0)
                Bitmap.createBitmap(p, mx, my, p.width - 2*mx, p.height - 2*my) else p
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose a PDF", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }
        if (loadError.isNotEmpty()) ToolErrorCard(
            "Couldn't open this PDF", loadError, accent,
            "Choose another", { loadError = ""; picker.launch(arrayOf("application/pdf")) }
        )
        if (cropped.isNotEmpty()) {
            StepControl("MARGIN %", margin, listOf(4,8,12,16), accent) { margin = it }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(cropped.take(6)) { pg ->
                    Image(pg.asImageBitmap(), null, Modifier.height(140.dp).clip(Shape.tile).background(PaperSunk),
                        contentScale = ContentScale.Fit)
                }
            }
            if (busy) ProcessingCard("Cropping your PDF...", accent)
            else ToolButton("Crop pages", accent) {
                busy = true
                val src = cropped.toList()
                scope.launch {
                    val r = withContext(Dispatchers.Default) { pagesToPdf(src) }
                    if (r != null) { output = r; outName = "cropped_${System.currentTimeMillis()}" }
                    busy = false
                }
            }
            output?.let { bytes ->
                ToolResultCard(
                    fileName = "$outName.pdf",
                    sizeBytes = bytes.size.toLong(),
                    accent = accent,
                    detail = "${cropped.size} page(s)",
                    onSave = { savePdfToDownloads(ctx, bytes, outName) },
                    onShare = { sharePdf(ctx, bytes, outName) }
                )
            }
        }
    }
}

// ---- PDF reorder: type new page order ----
@Composable
private fun PdfReorderTool(accent: Color) {
    val ctx = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var order by remember { mutableStateOf("") }
    var loadError by remember { mutableStateOf("") }
    var reOut by remember { mutableStateOf<ByteArray?>(null) }
    var reName by remember { mutableStateOf("") }
    fun load(u: Uri) {
        pages = renderPdf(ctx, u, 900)
        order = (1..pages.size).joinToString(",")
        loadError = if (pages.isEmpty()) pdfFailureReason(ctx, u) else ""
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) load(u)
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        cc.devbangs.morpho.workflow.WorkflowBus.consume()?.let { load(bytesToTempUri(ctx, it.bytes, it.mime)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose a PDF", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }
        if (loadError.isNotEmpty()) ToolErrorCard(
            "Couldn't open this PDF", loadError, accent,
            "Choose another", { loadError = ""; picker.launch(arrayOf("application/pdf")) }
        )
        if (pages.isNotEmpty()) {
            Text("${pages.size} pages — current order shown below", color = InkSoft, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(pages.size) { idx ->
                    Box(contentAlignment = Alignment.TopStart) {
                        Image(pages[idx].asImageBitmap(), null,
                            Modifier.height(130.dp).clip(Shape.tile).background(PaperSunk),
                            contentScale = ContentScale.Fit)
                        Box(Modifier.padding(4.dp).clip(Shape.pill).background(accent).padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text("${idx+1}", color = Paper, fontSize = 11.sp)
                        }
                    }
                }
            }
            Column { FieldLabel("NEW ORDER (e.g. 3,1,2)"); ToolInput(order, { order = it }, "1,2,3", minLines = 1, mono = true) }
            val reordered = order.split(",").mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..pages.size }.map { pages[it-1] }
            if (busy) ProcessingCard("Reordering your PDF...", accent)
            else ToolButton("Apply new order", accent) {
                busy = true
                val src = reordered.toList()
                scope.launch {
                    val r = withContext(Dispatchers.Default) { pagesToPdf(src) }
                    if (r != null) { reOut = r; reName = "reordered_${System.currentTimeMillis()}" }
                    busy = false
                }
            }
            reOut?.let { bytes ->
                ToolResultCard(
                    fileName = "$reName.pdf",
                    sizeBytes = bytes.size.toLong(),
                    accent = accent,
                    detail = "${reordered.size} page(s)",
                    onSave = { savePdfToDownloads(ctx, bytes, reName) },
                    onShare = { sharePdf(ctx, bytes, reName) }
                )
            }
        }
    }
}

// ---- shared ----
@Composable
private fun PickRow(label: String, icon: String, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.07f))
            .border(1.5.dp, accent.copy(alpha = 0.22f), Shape.card)
            .clickable(onClick = onClick)
            .padding(vertical = 30.dp, horizontal = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(Shape.chip).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(icon, tint = accent, size = 26.dp) }
        Spacer(Modifier.height(12.dp))
        Text(label, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text("Tap to select", color = InkFaint, fontSize = 12.sp)
    }
}

@Composable
private fun StepControl(label: String, value: Int, opts: List<Int>, accent: Color, onChange: (Int) -> Unit) {
    Column {
        FieldLabel("$label: $value")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            opts.forEach { n -> Box(Modifier.weight(1f)) {
                ToolButton("$n", if (value==n) accent else accent.copy(alpha=0.35f)) { onChange(n) } } }
        }
    }
}

private fun pagesToPdf(pages: List<Bitmap>): ByteArray? {
    if (pages.isEmpty()) return null
    val doc = android.graphics.pdf.PdfDocument()
    pages.forEach { bmp ->
        val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, doc.pages.size + 1).create()
        val page = doc.startPage(info); page.canvas.drawBitmap(bmp, 0f, 0f, null); doc.finishPage(page)
    }
    val s = java.io.ByteArrayOutputStream(); doc.writeTo(s); doc.close(); return s.toByteArray()
}
