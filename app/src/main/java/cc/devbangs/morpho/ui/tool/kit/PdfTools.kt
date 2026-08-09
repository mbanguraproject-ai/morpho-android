package cc.devbangs.morpho.ui.tool.kit

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
import java.io.ByteArrayOutputStream

fun hasPdfTool(id: String): Boolean = id in setOf(
    "jpg-to-pdf","image-to-pdf","pdf-to-jpg","merge-pdf","pdf-page-rotator",
    "pdf-page-numbering","pdf-watermark","pdf-splitter","pdf-page-extractor"
)

@Composable
fun PdfTool(id: String, accent: Color) {
    when (id) {
        "jpg-to-pdf","image-to-pdf" -> ImagesToPdf(accent)
        "merge-pdf" -> MergePdf(accent)
        else -> PdfFromSingle(id, accent)
    }
}

// ---- Images -> PDF ----
@Composable
private fun ImagesToPdf(accent: Color) {
    val ctx = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris = it }

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
                    savePdfToDownloads(ctx, bytes, "morpho_${System.currentTimeMillis()}")
                },
                onShare = {
                    val bytes = buildImagesPdf(ctx, uris) ?: return@ActionRow
                    sharePdf(ctx, bytes, "morpho_${System.currentTimeMillis()}")
                })
        }
    }
}

// ---- Merge PDFs ----
@Composable
private fun MergePdf(accent: Color) {
    val ctx = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris = it }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose PDF files", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uris.isNotEmpty()) {
            Text("${uris.size} PDF(s) selected", color = InkSoft, fontSize = 13.sp)
            ActionRow(accent,
                onSave = { val b = mergePdfs(ctx, uris) ?: return@ActionRow
                    savePdfToDownloads(ctx, b, "merged_${System.currentTimeMillis()}") },
                onShare = { val b = mergePdfs(ctx, uris) ?: return@ActionRow
                    sharePdf(ctx, b, "merged_${System.currentTimeMillis()}") })
        }
    }
}

// ---- Single-PDF tools: to-jpg, rotate, numbering, watermark, split, extract ----
@Composable
private fun PdfFromSingle(id: String, accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var rotation by remember { mutableStateOf(90) }
    var wm by remember { mutableStateOf("DRAFT") }
    var range by remember { mutableStateOf("1") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { u -> if (u != null) { uri = u; pages = renderPdf(ctx, u, 900) } }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose a PDF", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }
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
            // save each page to gallery instead of building a PDF
            pages.forEachIndexed { i, p ->
                saveToGallery(ctx, p, "pdf_page_${i+1}_${System.currentTimeMillis()}",
                    Bitmap.CompressFormat.JPEG, 92)
            }
            doc.close(); return ByteArray(0)
        }
        else -> pages.forEach { bitmapToPdfPage(doc, it) }
    }
    return docBytes(doc)
}
