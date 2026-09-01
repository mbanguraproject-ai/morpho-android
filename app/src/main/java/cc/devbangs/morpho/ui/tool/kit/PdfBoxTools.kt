package cc.devbangs.morpho.ui.tool.kit

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import cc.devbangs.morpho.workflow.WorkflowBus
import cc.devbangs.morpho.workflow.WorkflowGraph
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Tasks
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.cos.COSName
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

fun hasPdfBoxTool(id: String): Boolean = id in setOf(
    "pdf-text-extractor","pdf-password-protector","pdf-unlocker","pdf-compressor",
    "pdf-page-deleter","pdf-metadata-editor","html-to-pdf","pdf-image-extractor","pdf-ocr-scanner", "pdf-metadata-remover")

@Composable
fun PdfBoxTool(id: String, accent: Color, onOpenTool: (String) -> Unit = {}) {
    when (id) {
        "pdf-text-extractor" -> TextExtractor(accent)
        "pdf-password-protector" -> PasswordProtect(accent, onOpenTool)
        "pdf-unlocker" -> Unlock(accent)
        "pdf-compressor" -> Compress(accent, onOpenTool)
        "pdf-page-deleter" -> PageDeleter(accent)
        "pdf-metadata-editor" -> MetadataEditor(accent)
        "pdf-metadata-remover" -> MetadataRemover(accent)
        "html-to-pdf" -> HtmlToPdf(accent)
        "pdf-image-extractor" -> ImageExtractor(accent)
        "pdf-ocr-scanner" -> PdfOcrScanner(accent)
    }
}

@Composable
private fun TextExtractor(accent: Color) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            busy = true
            result = try {
                readBytes(ctx, u)?.let { bytes ->
                    PDDocument.load(bytes).use { doc ->
                        if (doc.isEncrypted) "⚠ This PDF is password-protected. Unlock it first."
                        else PDFTextStripper().getText(doc).ifBlank { "No selectable text found (may be a scanned PDF — try OCR)." }
                    }
                } ?: "⚠ Could not read file"
            } catch (e: Exception) { "⚠ ${e.message}" }
            busy = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose a PDF", accent) { picker.launch(arrayOf("application/pdf")) }
        if (busy) Text("Extracting…", color = InkSoft, fontSize = 14.sp)
        if (result.isNotEmpty()) ToolResult(result, accent, mono = false, label = "EXTRACTED TEXT")
    }
}

@Composable
private fun PasswordProtect(accent: Color, onOpenTool: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var pw by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<ByteArray?>(null) }
    var outName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; output = null }
    LaunchedEffect(Unit) { WorkflowBus.consume()?.let { pf -> uri = bytesToTempUri(ctx, pf.bytes) } }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected ✓", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Column { FieldLabel("PASSWORD"); ToolInput(pw, { pw = it }, "Set a password", minLines = 1) }
            if (pw.isNotBlank()) {
                val u = uri!!
                if (busy) ProcessingCard("Protecting your PDF...", accent)
                else ToolButton("Protect PDF", accent) {
                    busy = true
                    scope.launch {
                        // Encrypted once; both actions use the same result.
                        val r = withContext(Dispatchers.Default) { protectPdf(ctx, u, pw) }
                        if (r != null) {
                            output = r
                            outName = "protected_${System.currentTimeMillis()}"
                        }
                        busy = false
                    }
                }
                output?.let { bytes ->
                    ToolResultCard(
                        fileName = "$outName.pdf",
                        sizeBytes = bytes.size.toLong(),
                        accent = accent,
                        detail = "password set",
                        onSave = { savePdfToDownloads(ctx, bytes, outName) },
                        onShare = { sharePdf(ctx, bytes, outName) }
                    )
                }
                output?.let { bytes ->
                    NextStepSuggestions(WorkflowGraph.nextSteps("pdf-password-protector")) { step ->
                        WorkflowBus.handOff(bytes, "application/pdf"); onOpenTool(step.toolId)
                    }
                }
            }
        }
    }
}

@Composable
private fun Unlock(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var pw by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a locked PDF" else "PDF selected ✓", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Column { FieldLabel("CURRENT PASSWORD"); ToolInput(pw, { pw = it }, "Enter the PDF's password", minLines = 1) }
            val u = uri!!
            if (busy) {
                ProcessingCard("Unlocking your PDF...", accent)
            } else {
                ActionRow(accent,
                    {
                        busy = true
                        scope.launch {
                            val r = withContext(Dispatchers.Default) { unlockPdf(ctx, u, pw) }
                            if (r != null) { savePdfToDownloads(ctx, r, "unlocked_${System.currentTimeMillis()}"); msg = "" } else msg = "\u26a0 Wrong password or not encrypted."
                            busy = false
                        }
                    },
                    {
                        busy = true
                        scope.launch {
                            val r = withContext(Dispatchers.Default) { unlockPdf(ctx, u, pw) }
                            if (r != null) sharePdf(ctx, r, "unlocked_${System.currentTimeMillis()}") else msg = "\u26a0 Wrong password."
                            busy = false
                        }
                    })
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Compress(accent: Color, onOpenTool: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var origSize by remember { mutableStateOf(0L) }
    var output by remember { mutableStateOf<ByteArray?>(null) }
    var outName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        uri = u; origSize = u?.let { readBytes(ctx, it)?.size?.toLong() } ?: 0L; output = null
    }
    // Receive a handed-off file from a previous tool in the chain
    LaunchedEffect(Unit) {
        WorkflowBus.consume()?.let { pf ->
            val u = bytesToTempUri(ctx, pf.bytes)
            uri = u; origSize = pf.bytes.size.toLong()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected ✓", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Text("Original: ${bytesHuman(origSize)}", color = InkSoft, fontSize = 13.sp)
            Text("Re-renders pages at reduced resolution to shrink size.", color = InkFaint, fontSize = 12.sp)
            val u = uri!!
            if (busy) ProcessingCard("Compressing your PDF...", accent)
            else ToolButton("Compress PDF", accent) {
                busy = true
                scope.launch {
                    val r = withContext(Dispatchers.Default) { compressPdf(ctx, u) }
                    if (r != null) {
                        output = r
                        outName = "compressed_${System.currentTimeMillis()}"
                    }
                    busy = false
                }
            }
            output?.let { bytes ->
                // The saving is the whole point of this tool, so it leads.
                val saved = if (origSize > 0)
                    (100 - bytes.size * 100L / origSize).coerceAtLeast(0) else 0
                ToolResultCard(
                    fileName = "$outName.pdf",
                    sizeBytes = bytes.size.toLong(),
                    accent = accent,
                    detail = if (origSize > 0) "was ${bytesHuman(origSize)}, $saved% smaller" else null,
                    onSave = { savePdfToDownloads(ctx, bytes, outName) },
                    onShare = { sharePdf(ctx, bytes, outName) }
                )
            }

            output?.let { bytes ->
                NextStepSuggestions(WorkflowGraph.nextSteps("pdf-compressor")) { step ->
                    WorkflowBus.handOff(bytes, "application/pdf"); onOpenTool(step.toolId)
                }
            }
        }
    }
}

// ---- PDFBox operations ----
private fun protectPdf(ctx: Context, uri: Uri, password: String): ByteArray? = try {
    readBytes(ctx, uri)?.let { bytes ->
        PDDocument.load(bytes).use { doc ->
            val ap = AccessPermission()
            val policy = StandardProtectionPolicy(password, password, ap)
            policy.encryptionKeyLength = 128
            doc.protect(policy)
            val out = ByteArrayOutputStream(); doc.save(out); out.toByteArray()
        }
    }
} catch (e: Exception) { null }

private fun unlockPdf(ctx: Context, uri: Uri, password: String): ByteArray? = try {
    readBytes(ctx, uri)?.let { bytes ->
        PDDocument.load(bytes, password).use { doc ->
            doc.setAllSecurityToBeRemoved(true)
            val out = ByteArrayOutputStream(); doc.save(out); out.toByteArray()
        }
    }
} catch (e: Exception) { null }

private fun compressPdf(ctx: Context, uri: Uri): ByteArray? =
    // reuse the native renderer at lower res, rebuild — genuine size reduction
    renderPdf(ctx, uri, 800).takeIf { it.isNotEmpty() }?.let { pages ->
        val doc = android.graphics.pdf.PdfDocument()
        pages.forEach { bmp ->
            val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, doc.pages.size + 1).create()
            val pg = doc.startPage(info); pg.canvas.drawBitmap(bmp, 0f, 0f, null); doc.finishPage(pg)
        }
        val s = ByteArrayOutputStream(); doc.writeTo(s); doc.close(); s.toByteArray()
    }

@Composable
private fun PickRow(label: String, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.08f))
        .clickable(onClick = onClick).padding(Space.lg), verticalAlignment = Alignment.CenterVertically) {
        MorphoIcon("file-add", tint = accent, size = 26.dp)
        Spacer(Modifier.width(Space.md)); Text(label, color = accent, fontSize = 15.sp)
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

@androidx.compose.runtime.Composable
private fun PageDeleter(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var toDelete by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        uri = u; msg = ""
        if (u != null) {
            pageCount = try { readBytes(ctx, u)?.let { PDDocument.load(it).use { d -> d.numberOfPages } } ?: 0 } catch (e: Exception) { 0 }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected \u2713 ($pageCount pages)", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null && pageCount > 0) {
            Column { FieldLabel("PAGES TO DELETE (e.g. 1,3,5)"); ToolInput(toDelete, { toDelete = it }, "1,2", minLines = 1, mono = true) }
            if (busy) ProcessingCard("Deleting pages...", accent)
            else ToolButton("Delete pages & save", accent) {
                busy = true; val u = uri!!; val del = toDelete
                scope.launch {
                    val result = withContext(Dispatchers.Default) {
                        try {
                            val nums = del.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..pageCount }.toSet()
                            if (nums.isEmpty()) return@withContext null
                            readBytes(ctx, u)?.let { bytes ->
                                PDDocument.load(bytes).use { doc ->
                                    nums.sortedDescending().forEach { doc.removePage(it - 1) }
                                    val out = ByteArrayOutputStream(); doc.save(out); out.toByteArray()
                                }
                            }
                        } catch (e: Exception) { null }
                    }
                    if (result != null) savePdfToDownloads(ctx, result, "edited_${System.currentTimeMillis()}")
                    else msg = "\u26a0 Check your page numbers and try again."
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun MetadataEditor(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        uri = u; msg = ""
        if (u != null) try { readBytes(ctx, u)?.let { PDDocument.load(it).use { d ->
            title = d.documentInformation.title ?: ""; author = d.documentInformation.author ?: ""; subject = d.documentInformation.subject ?: "" } } } catch (e: Exception) {}
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected \u2713", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Column { FieldLabel("TITLE"); ToolInput(title, { title = it }, "Document title", minLines = 1) }
            Column { FieldLabel("AUTHOR"); ToolInput(author, { author = it }, "Author name", minLines = 1) }
            Column { FieldLabel("SUBJECT"); ToolInput(subject, { subject = it }, "Subject", minLines = 1) }
            if (busy) ProcessingCard("Saving metadata...", accent)
            else ToolButton("Save metadata", accent) {
                busy = true; val u = uri!!
                scope.launch {
                    val result = withContext(Dispatchers.Default) {
                        try { readBytes(ctx, u)?.let { bytes ->
                            PDDocument.load(bytes).use { doc ->
                                doc.documentInformation.title = title
                                doc.documentInformation.author = author
                                doc.documentInformation.subject = subject
                                val out = ByteArrayOutputStream(); doc.save(out); out.toByteArray()
                            } } } catch (e: Exception) { null }
                    }
                    if (result != null) savePdfToDownloads(ctx, result, "metadata_${System.currentTimeMillis()}")
                    else msg = "\u26a0 Could not update this PDF."
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun HtmlToPdf(accent: Color) {
    val ctx = LocalContext.current
    var html by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("HTML"); ToolInput(html, { html = it }, "<h1>Hello</h1><p>My document</p>", minLines = 6, mono = true) }
        if (html.isNotBlank()) {
            Text("Renders your HTML and exports it as a PDF.", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Creating PDF...", accent)
            else ToolButton("Create PDF", accent) {
                busy = true
                val webView = android.webkit.WebView(ctx)
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView, url: String) {
                        val adapter = view.createPrintDocumentAdapter("morpho_${System.currentTimeMillis()}")
                        val attrs = android.print.PrintAttributes.Builder()
                            .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(android.print.PrintAttributes.Resolution("id", "id", 300, 300))
                            .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS).build()
                        try {
                            val pm = ctx.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                            pm.print("Morpho HTML", adapter, attrs)
                        } catch (e: Exception) { msg = "\u26a0 Could not create PDF." }
                        busy = false
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun ImageExtractor(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = ""; found = 0 }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected \u2713", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Text("Pulls embedded images out of the PDF and saves them to your gallery.", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Extracting images...", accent)
            else ToolButton("Extract images", accent) {
                busy = true; val u = uri!!
                scope.launch {
                    val count = withContext(Dispatchers.Default) {
                        try {
                            readBytes(ctx, u)?.let { bytes ->
                                PDDocument.load(bytes).use { doc ->
                                    if (doc.isEncrypted) return@let -1
                                    var n = 0
                                    for (page in doc.pages) {
                                        val res = page.resources ?: continue
                                        for (name in res.xObjectNames) {
                                            try {
                                                val xobj = res.getXObject(name)
                                                if (xobj is PDImageXObject) {
                                                    val bmp: Bitmap? = xobj.image
                                                    if (bmp != null) {
                                                        if (saveToGallery(ctx, bmp, "pdfimg_${System.currentTimeMillis()}_$n", Bitmap.CompressFormat.PNG, 100, report = false)) n++
                                                    }
                                                }
                                            } catch (e: Exception) {}
                                        }
                                    }
                                    n
                                }
                            } ?: -2
                        } catch (e: Exception) { -2 }
                    }
                    found = count
                    // The screen prints its own result below, so the toast is
                    // suppressed; without this every extracted image notified.
                    if (count > 0) reportSave(
                        ctx, true, "Images ready",
                        "$count image(s) were saved to your gallery.", ""
                    )
                    msg = when {
                        count > 0 -> "\u2713 Saved $count image(s) to your gallery."
                        count == 0 -> "No embedded images found in this PDF."
                        count == -1 -> "\u26a0 This PDF is password-protected. Unlock it first."
                        else -> "\u26a0 Could not read this PDF."
                    }
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun PdfOcrScanner(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; result = ""; msg = "" }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a scanned PDF" else "PDF selected \u2713", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Text("Reads text from each page image using on-device OCR. Great for scanned PDFs.", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Reading text from pages...", accent)
            else ToolButton("Extract text (OCR)", accent) {
                busy = true; val u = uri!!
                scope.launch {
                    val text = withContext(Dispatchers.Default) {
                        try {
                            val pages = renderPdf(ctx, u, 1600)
                            if (pages.isEmpty()) return@withContext null
                            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                            val sb = StringBuilder()
                            pages.forEachIndexed { i, bmp ->
                                try {
                                    val visionText = Tasks.await(recognizer.process(InputImage.fromBitmap(bmp, 0)))
                                    val t = visionText.text.trim()
                                    if (t.isNotEmpty()) { sb.append("--- Page ${i+1} ---\n").append(t).append("\n\n") }
                                } catch (e: Exception) {}
                            }
                            sb.toString().trim()
                        } catch (e: Exception) { null }
                    }
                    when {
                        text == null -> msg = "\u26a0 Could not read this PDF."
                        text.isBlank() -> msg = "No text found (the pages may be blank or very low quality)."
                        else -> { result = text; msg = "" }
                    }
                    busy = false
                }
            }
            if (result.isNotEmpty()) ToolResult(result, accent, mono = false, label = "EXTRACTED TEXT")
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun MetadataRemover(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected \u2713", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Text("Removes author, title, creator, and dates \u2014 all hidden document info. Runs on your device.", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Removing metadata...", accent)
            else ToolButton("Remove all metadata", accent) {
                busy = true; val u = uri!!
                scope.launch {
                    val result = withContext(Dispatchers.Default) {
                        try {
                            readBytes(ctx, u)?.let { bytes ->
                                PDDocument.load(bytes).use { doc ->
                                    val info = doc.documentInformation
                                    info.title = null; info.author = null; info.subject = null
                                    info.keywords = null; info.creator = null; info.producer = null
                                    info.creationDate = null; info.modificationDate = null
                                    try { info.setCustomMetadataValue("", null) } catch (e: Exception) {}
                                    // also drop XMP metadata stream if present
                                    try { doc.documentCatalog.metadata = null } catch (e: Exception) {}
                                    val out = java.io.ByteArrayOutputStream()
                                    doc.save(out)
                                    out.toByteArray()
                                }
                            }
                        } catch (e: Exception) { null }
                    }
                    if (result != null) { savePdfToDownloads(ctx, result, "cleaned_${System.currentTimeMillis()}"); msg = "Metadata removed \u2713 Saved to Download/Morpho." }
                    else msg = "\u26a0 Could not process this PDF."
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}
