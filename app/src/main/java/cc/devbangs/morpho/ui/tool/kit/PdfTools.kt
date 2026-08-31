package cc.devbangs.morpho.ui.tool.kit

import android.content.Context
import java.io.File
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import cc.devbangs.morpho.workflow.WorkflowBus
import cc.devbangs.morpho.workflow.WorkflowGraph
import java.io.ByteArrayOutputStream

fun hasPdfTool(id: String): Boolean = id in setOf(
    "jpg-to-pdf","image-to-pdf","pdf-to-jpg","merge-pdf","pdf-page-rotator",
    "pdf-page-numbering","pdf-watermark","pdf-splitter","pdf-page-extractor", "scan-to-pdf", "word-to-pdf", "pdf-to-word", "pdf-to-excel", "excel-to-pdf", "pdf-to-powerpoint", "ppt-to-pdf", "pdf-to-html", "svg-to-png", "mp3-converter", "video-compressor"
)

@Composable
fun PdfTool(id: String, accent: Color, onOpenTool: (String) -> Unit = {}) {
    when (id) {
        "jpg-to-pdf","image-to-pdf" -> ImagesToPdf(accent, onOpenTool)
        "scan-to-pdf" -> ScanToPdf(accent, onOpenTool)
        "word-to-pdf" -> WordToPdf(accent)
        "pdf-to-word" -> ConvertTool(accent, "pdf", "docx", "PDF", "Word", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        "pdf-to-excel" -> ConvertTool(accent, "pdf", "xlsx", "PDF", "Excel", "application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        "excel-to-pdf" -> ConvertTool(accent, "xlsx", "pdf", "Excel", "PDF", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/pdf")
        "pdf-to-powerpoint" -> ConvertTool(accent, "pdf", "pptx", "PDF", "PowerPoint", "application/pdf", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
        "ppt-to-pdf" -> ConvertTool(accent, "pptx", "pdf", "PowerPoint", "PDF", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/pdf")
        "pdf-to-html" -> ConvertTool(accent, "pdf", "html", "PDF", "HTML", "application/pdf", "text/html")
        "svg-to-png" -> ConvertTool(accent, "svg", "png", "SVG", "PNG", "image/svg+xml", "image/png")
        "mp3-converter" -> ConvertTool(accent, "", "mp3", "Audio", "MP3", "audio/*", "audio/mpeg")
        "video-compressor" -> VideoCompressor(accent)
        "merge-pdf" -> MergePdf(accent, onOpenTool)
        else -> PdfFromSingle(id, accent, onOpenTool)
    }
}

// ---- Images -> PDF ----
@Composable
private fun ImagesToPdf(accent: Color, onOpenTool: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var output by remember { mutableStateOf<ByteArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris = it; output = null }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose images", "image-add", accent) {
            picker.launch(androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        if (uris.isNotEmpty()) {
            Text("${uris.size} image(s) selected", color = InkSoft, fontSize = 13.sp)
            ActionRow(accent,
                onSave = {
                    val bytes = buildImagesPdf(ctx, uris) ?: return@ActionRow
                    output = bytes; savePdfToDownloads(ctx, bytes, "morpho_${System.currentTimeMillis()}")
                },
                onShare = {
                    val bytes = buildImagesPdf(ctx, uris) ?: return@ActionRow
                    output = bytes; sharePdf(ctx, bytes, "morpho_${System.currentTimeMillis()}")
                })
            output?.let { bytes ->
                NextStepSuggestions(WorkflowGraph.nextSteps("jpg-to-pdf")) { step ->
                    WorkflowBus.handOff(bytes, "application/pdf"); onOpenTool(step.toolId)
                }
            }
        }
    }
}

// ---- Merge PDFs ----
@Composable
private fun MergePdf(accent: Color, onOpenTool: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var output by remember { mutableStateOf<ByteArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris = it; output = null }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose PDF files", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uris.isNotEmpty()) {
            Text("${uris.size} PDF(s) selected", color = InkSoft, fontSize = 13.sp)
            if (busy) {
                ProcessingCard("Merging your PDFs...", accent)
            } else {
                ActionRow(accent,
                    onSave = {
                        busy = true
                        scope.launch {
                            val b = withContext(Dispatchers.Default) { mergePdfs(ctx, uris) }
                            if (b != null) { output = b; savePdfToDownloads(ctx, b, "merged_${System.currentTimeMillis()}") }
                            busy = false
                        }
                    },
                    onShare = {
                        busy = true
                        scope.launch {
                            val b = withContext(Dispatchers.Default) { mergePdfs(ctx, uris) }
                            if (b != null) { output = b; sharePdf(ctx, b, "merged_${System.currentTimeMillis()}") }
                            busy = false
                        }
                    })
            }
            output?.let { bytes ->
                NextStepSuggestions(WorkflowGraph.nextSteps("merge-pdf")) { step ->
                    WorkflowBus.handOff(bytes, "application/pdf"); onOpenTool(step.toolId)
                }
            }
        }
    }
}

// ---- Single-PDF tools: to-jpg, rotate, numbering, watermark, split, extract ----
@Composable
private fun PdfFromSingle(id: String, accent: Color, onOpenTool: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var rotation by remember { mutableStateOf(90) }
    var wm by remember { mutableStateOf("DRAFT") }
    var range by remember { mutableStateOf("1") }
    var loadError by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { u ->
        if (u != null) {
            uri = u
            pages = renderPdf(ctx, u, 900)
            loadError = if (pages.isEmpty()) pdfFailureReason(ctx, u) else ""
        }
    }

    // Receive a handed-off file from a previous tool
    LaunchedEffect(Unit) {
        WorkflowBus.consume()?.let { pf ->
            val u = bytesToTempUri(ctx, pf.bytes)
            uri = u
            pages = renderPdf(ctx, u, 900)
            loadError = if (pages.isEmpty()) pdfFailureReason(ctx, u) else ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose a PDF", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }
        if (loadError.isNotEmpty()) ToolErrorCard(
            title = "Couldn't open this PDF",
            body = loadError,
            accent = accent,
            actionLabel = "Choose another",
            onAction = { loadError = ""; picker.launch(arrayOf("application/pdf")) }
        )
        if (pages.isNotEmpty()) {
            Text("${pages.size} page(s)", color = InkSoft, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(pages.take(8)) { pg ->
                    Image(pg.asImageBitmap(), null,
                        Modifier.height(140.dp).clip(Shape.tile).background(PaperSunk),
                        contentScale = ContentScale.Fit)
                }
            }
            when (id) {
                "pdf-page-rotator" -> StepControl("ROTATE°", rotation, listOf(90,180,270), accent) { rotation = it }
                "pdf-watermark" -> Column { FieldLabel("WATERMARK"); ToolInput(wm, { wm = it }, "DRAFT", minLines = 1) }
                "pdf-splitter","pdf-page-extractor" -> Column { FieldLabel("PAGES (e.g. 1,3,5)"); ToolInput(range, { range = it }, "1,2", minLines = 1) }
            }
            val u = uri!!
            ActionRow(accent,
                onSave = { buildSingle(ctx, id, u, pages, rotation, wm, range)?.let {
                    savePdfToDownloads(ctx, it, "morpho_${System.currentTimeMillis()}") } },
                onShare = { buildSingle(ctx, id, u, pages, rotation, wm, range)?.let {
                    sharePdf(ctx, it, "morpho_${System.currentTimeMillis()}") } },
                saveLabel = if (id == "pdf-to-jpg") "Save pages to gallery" else "Save PDF",
                pdfToJpg = id == "pdf-to-jpg")
            val steps = WorkflowGraph.nextSteps(id)
            if (steps.isNotEmpty() && id != "pdf-to-jpg") {
                NextStepSuggestions(steps) { step ->
                    buildSingle(ctx, id, u, pages, rotation, wm, range)?.let { bytes ->
                        WorkflowBus.handOff(bytes, "application/pdf"); onOpenTool(step.toolId)
                    }
                }
            }
        }
    }
}

// ---------- shared UI ----------
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
private fun ActionRow(
    accent: Color, onSave: () -> Unit, onShare: () -> Unit,
    saveLabel: String = "Save PDF", pdfToJpg: Boolean = false
) {
    val ctx = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Box(Modifier.weight(1f)) { ToolButton(saveLabel, accent) { onSave() } }
        if (!pdfToJpg) Box(Modifier.weight(1f)) { OutlineBtn("Share", accent) { onShare() } }
    }
}

@Composable
private fun OutlineBtn(text: String, accent: Color, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
        .clickable(onClick = onClick).padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
        Text(text, color = accent, fontSize = 15.sp)
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

// ---------- PDF builders ----------
private fun bitmapToPdfPage(doc: PdfDocument, bmp: Bitmap) {
    val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, doc.pages.size + 1).create()
    val page = doc.startPage(info)
    page.canvas.drawBitmap(bmp, 0f, 0f, null)
    doc.finishPage(page)
}

private fun docBytes(doc: PdfDocument): ByteArray {
    val s = ByteArrayOutputStream(); doc.writeTo(s); doc.close(); return s.toByteArray()
}

private fun buildImagesPdf(ctx: android.content.Context, uris: List<Uri>): ByteArray? {
    if (uris.isEmpty()) return null
    val doc = PdfDocument()
    uris.forEach { u -> decodeBitmap(ctx, u, 1600)?.let { bitmapToPdfPage(doc, it) } }
    return docBytes(doc)
}

private fun mergePdfs(ctx: android.content.Context, uris: List<Uri>): ByteArray? {
    if (uris.isEmpty()) return null
    val doc = PdfDocument()
    uris.forEach { u -> renderPdf(ctx, u, 1240).forEach { bitmapToPdfPage(doc, it) } }
    return docBytes(doc)
}

private fun parseRange(s: String, max: Int): List<Int> =
    s.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..max }.map { it - 1 }

private fun buildSingle(
    ctx: android.content.Context, id: String, uri: Uri, pages: List<Bitmap>,
    rotation: Int, wm: String, range: String
): ByteArray? {
    val doc = PdfDocument()
    when (id) {
        "pdf-page-rotator" -> pages.forEach { p ->
            val m = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
            bitmapToPdfPage(doc, Bitmap.createBitmap(p, 0, 0, p.width, p.height, m, true))
        }
        "pdf-watermark" -> pages.forEach { p ->
            val out = p.copy(Bitmap.Config.ARGB_8888, true)
            val c = Canvas(out)
            val paint = Paint().apply {
                color = AColor.argb(60, 200, 0, 0); isAntiAlias = true
                textSize = out.width / 8f
            }
            c.save(); c.rotate(-30f, out.width/2f, out.height/2f)
            c.drawText(wm, out.width*0.15f, out.height*0.55f, paint); c.restore()
            bitmapToPdfPage(doc, out)
        }
        "pdf-page-numbering" -> pages.forEachIndexed { i, p ->
            val out = p.copy(Bitmap.Config.ARGB_8888, true)
            val c = Canvas(out)
            val paint = Paint().apply { color = AColor.DKGRAY; isAntiAlias = true; textSize = out.width/28f }
            val label = "${i+1} / ${pages.size}"
            c.drawText(label, out.width - paint.measureText(label) - out.width*0.05f,
                out.height - out.height*0.03f, paint)
            bitmapToPdfPage(doc, out)
        }
        "pdf-splitter", "pdf-page-extractor" -> {
            val sel = parseRange(range, pages.size).ifEmpty { listOf(0) }
            sel.forEach { idx -> pages.getOrNull(idx)?.let { bitmapToPdfPage(doc, it) } }
        }
        "pdf-to-jpg" -> {
            // Save each page to the gallery instead of building a PDF. Each page
            // reports quietly so a 20-page document produces one notification
            // and one toast rather than twenty of each.
            var saved = 0
            pages.forEachIndexed { i, p ->
                if (saveToGallery(ctx, p, "pdf_page_${i+1}_${System.currentTimeMillis()}",
                        Bitmap.CompressFormat.JPEG, 92, report = false)) saved++
            }
            val total = pages.size
            reportSave(
                ctx, saved > 0, "Pages ready",
                if (saved == total) "All $total pages were saved to your gallery."
                else "$saved of $total pages were saved to your gallery.",
                if (saved == total) "Saved $total pages to Pictures/Morpho"
                else "Saved $saved of $total pages",
                "Couldn't save any pages"
            )
            doc.close(); return ByteArray(0)
        }
        else -> pages.forEach { bitmapToPdfPage(doc, it) }
    }
    return docBytes(doc)
}

@androidx.compose.runtime.Composable
private fun ScanToPdf(accent: Color, onOpenTool: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    var pages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var output by remember { mutableStateOf<ByteArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendingUri != null) { pages = pages + pendingUri!!; output = null }
        pendingUri = null
    }

    fun capture() {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val u = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        pendingUri = u
        camera.launch(u)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (pages.isEmpty()) "Scan a page" else "Scan another page", "image-add", accent) { capture() }
        if (pages.isNotEmpty()) {
            Text("${pages.size} page(s) scanned", color = InkSoft, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(pages) { u ->
                    decodeBitmap(ctx, u, 400)?.let { bmp ->
                        Image(bmp.asImageBitmap(), null,
                            Modifier.height(120.dp).clip(Shape.tile).background(PaperSunk),
                            contentScale = ContentScale.Fit)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
                .clickable { pages = emptyList(); output = null }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center) {
                Text("Clear all", color = accent, fontSize = 14.sp)
            }
            if (busy) {
                ProcessingCard("Building your PDF...", accent)
            } else {
                ActionRow(accent,
                    onSave = {
                        busy = true
                        scope.launch {
                            val bytes = withContext(Dispatchers.Default) { buildImagesPdf(ctx, pages) }
                            if (bytes != null) { output = bytes; savePdfToDownloads(ctx, bytes, "scan_${System.currentTimeMillis()}") }
                            busy = false
                        }
                    },
                    onShare = {
                        busy = true
                        scope.launch {
                            val bytes = withContext(Dispatchers.Default) { buildImagesPdf(ctx, pages) }
                            if (bytes != null) { output = bytes; sharePdf(ctx, bytes, "scan_${System.currentTimeMillis()}") }
                            busy = false
                        }
                    })
            }
            output?.let { bytes ->
                NextStepSuggestions(WorkflowGraph.nextSteps("scan-to-pdf")) { step ->
                    WorkflowBus.handOff(bytes, "application/pdf"); onOpenTool(step.toolId)
                }
            }
        }
    }
}

private fun extractDocxText(ctx: Context, uri: Uri): String? {
    return try {
        val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes() ?: return null
        val zis = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes))
        var doc = ""
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") { doc = zis.readBytes().toString(Charsets.UTF_8); break }
            entry = zis.nextEntry
        }
        zis.close()
        doc.replace(Regex("</w:p>"), "\n").replace(Regex("<[^>]+>"), "")
            .replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").trim()
    } catch (e: Exception) { null }
}

private fun textToPdfBytes(text: String): ByteArray {
    val doc = PdfDocument()
    val pageW = 595; val pageH = 842
    val margin = 48f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.BLACK; textSize = 12f }
    val lineH = paint.fontSpacing
    val maxW = pageW - 2 * margin
    val lines = mutableListOf<String>()
    text.split("\n").forEach { para ->
        if (para.isBlank()) { lines.add(""); return@forEach }
        var line = ""
        para.split(" ").forEach { word ->
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxW) { lines.add(line); line = word }
            else line = test
        }
        if (line.isNotEmpty()) lines.add(line)
    }
    var i = 0
    while (true) {
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1).create()
        val page = doc.startPage(info)
        val c = page.canvas
        var y = margin + lineH
        while (i < lines.size && y < pageH - margin) {
            c.drawText(lines[i], margin, y, paint); y += lineH; i++
        }
        doc.finishPage(page)
        if (i >= lines.size) break
    }
    val out = ByteArrayOutputStream(); doc.writeTo(out); doc.close()
    return out.toByteArray()
}

@androidx.compose.runtime.Composable
private fun WordToPdf(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a .docx file" else "File selected \u2713", "file-add", accent) {
            picker.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }
        if (uri != null) {
            Text("Converts the document's text into a clean PDF. Complex layouts and images aren't preserved.", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Creating PDF...", accent)
            else ToolButton("Convert to PDF", accent) {
                busy = true; val u = uri!!
                scope.launch {
                    val bytes = withContext(Dispatchers.Default) {
                        val text = extractDocxText(ctx, u)
                        if (text.isNullOrBlank()) null else textToPdfBytes(text)
                    }
                    if (bytes != null) savePdfToDownloads(ctx, bytes, "converted_${System.currentTimeMillis()}")
                    else msg = "\u26a0 Could not read this .docx file."
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun ConvertTool(
    accent: Color, from: String, to: String,
    fromLabel: String, toLabel: String,
    inputMime: String, outputMime: String
) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    val outExt = to
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a $fromLabel file" else "$fromLabel selected \u2713", "file-add", accent) {
            picker.launch(arrayOf(inputMime))
        }
        if (uri != null) {
            Text("Converts your $fromLabel into $toLabel. Needs internet.", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Converting to $toLabel...", accent)
            else ToolButton("Convert to $toLabel", accent) {
                busy = true; val u = uri!!
                scope.launch {
                    val out = withContext(Dispatchers.IO) {
                        cloudConvert(ctx, u, from, to, "converted_${System.currentTimeMillis()}.$outExt")
                    }
                    if (out != null) {
                        saveBytesToDownloads(ctx, out.readBytes(), out.name, outputMime)
                        msg = "Saved to Download/Morpho \u2713"
                    } else {
                        msg = "\u26a0 Conversion failed. Check your connection and try again."
                    }
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun VideoCompressor(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(1) }  // 0=light,1=balanced,2=strong
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    // crf: higher = more compression / smaller. width caps resolution.
    val presets = listOf(
        Triple("Light", 23, 0),
        Triple("Balanced", 28, 1280),
        Triple("Strong", 32, 854)
    )
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a video" else "Video selected \u2713", "cat-video", accent) {
            picker.launch(arrayOf("video/*"))
        }
        if (uri != null) {
            FieldLabel("COMPRESSION")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                presets.forEachIndexed { i, (lbl, _, _) ->
                    Box(Modifier.weight(1f)) { ToolButton(lbl, if (level==i) accent else accent.copy(alpha=0.35f)) { level = i } }
                }
            }
            Text("Re-encodes the video smaller. Needs internet.", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Compressing video...", accent)
            else ToolButton("Compress video", accent) {
                busy = true; val u = uri!!
                val (_, crf, width) = presets[level]
                val extra = "&crf=$crf" + (if (width > 0) "&width=$width" else "")
                scope.launch {
                    val out = withContext(Dispatchers.IO) {
                        cloudConvert(ctx, u, "", "mp4", "compressed_${System.currentTimeMillis()}.mp4", extra)
                    }
                    if (out != null) {
                        saveMediaToGallery(ctx, out, out.name, true)
                        msg = "Saved to your gallery \u2713"
                    } else msg = "\u26a0 Compression failed. Check your connection and try again."
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}
