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
                    if (saveToGallery(ctx, scaled, "favicon_${sz}", Bitmap.CompressFormat.PNG, 100)) ok++
                }
            }
        }
    }
}

// ---- PDF crop: trim margins by % on all pages ----
@Composable
private fun PdfCropTool(accent: Color) {
    val ctx = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var margin by remember { mutableStateOf(8) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) pages = renderPdf(ctx, u, 900)
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
        if (cropped.isNotEmpty()) {
            StepControl("MARGIN %", margin, listOf(4,8,12,16), accent) { margin = it }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(cropped.take(6)) { pg ->
                    Image(pg.asImageBitmap(), null, Modifier.height(140.dp).clip(Shape.tile).background(PaperSunk),
                        contentScale = ContentScale.Fit)
                }
            }
            ActionRow(accent,
                { pagesToPdf(cropped)?.let { savePdfToDownloads(ctx, it, "cropped_${System.currentTimeMillis()}") } },
                { pagesToPdf(cropped)?.let { sharePdf(ctx, it, "cropped_${System.currentTimeMillis()}") } })
        }
    }
}

// ---- PDF reorder: type new page order ----
@Composable
private fun PdfReorderTool(accent: Color) {
    val ctx = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var order by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) { pages = renderPdf(ctx, u, 900); order = (1..pages.size).joinToString(",") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose a PDF", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }
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
            ActionRow(accent,
                { pagesToPdf(reordered)?.let { savePdfToDownloads(ctx, it, "reordered_${System.currentTimeMillis()}") } },
                { pagesToPdf(reordered)?.let { sharePdf(ctx, it, "reordered_${System.currentTimeMillis()}") } })
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

@Composable
private fun ActionRow(accent: Color, onSave: () -> Unit, onShare: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Box(Modifier.weight(1f)) { ToolButton("Save PDF", accent) { onSave() } }
        Box(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
                .clickable { onShare() }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                Text("Share", color = accent, fontSize = 15.sp)
            }
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
