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
import java.io.ByteArrayOutputStream

fun hasPdfBoxTool(id: String): Boolean = id in setOf(
    "pdf-text-extractor","pdf-password-protector","pdf-unlocker","pdf-compressor"
)

@Composable
fun PdfBoxTool(id: String, accent: Color, onOpenTool: (String) -> Unit = {}) {
    when (id) {
        "pdf-text-extractor" -> TextExtractor(accent)
        "pdf-password-protector" -> PasswordProtect(accent, onOpenTool)
        "pdf-unlocker" -> Unlock(accent)
        "pdf-compressor" -> Compress(accent, onOpenTool)
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
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; output = null }
    LaunchedEffect(Unit) { WorkflowBus.consume()?.let { pf -> uri = pf.uri } }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected ✓", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Column { FieldLabel("PASSWORD"); ToolInput(pw, { pw = it }, "Set a password", minLines = 1) }
            if (pw.isNotBlank()) {
                val u = uri!!
                ActionRow(accent,
                    { protectPdf(ctx, u, pw)?.let { output = it; savePdfToDownloads(ctx, it, "protected_${System.currentTimeMillis()}") } },
                    { protectPdf(ctx, u, pw)?.let { output = it; sharePdf(ctx, it, "protected_${System.currentTimeMillis()}") } })
                output?.let { bytes ->
                    NextStepSuggestions(WorkflowGraph.nextSteps("pdf-password-protector")) { step ->
                        cachePdfForHandoff(ctx, bytes)?.let { h ->
                            WorkflowBus.handOff(h, "application/pdf"); onOpenTool(step.toolId)
                        }
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
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a locked PDF" else "PDF selected ✓", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Column { FieldLabel("CURRENT PASSWORD"); ToolInput(pw, { pw = it }, "Enter the PDF's password", minLines = 1) }
            val u = uri!!
            ActionRow(accent,
                { val r = unlockPdf(ctx, u, pw); if (r != null) { savePdfToDownloads(ctx, r, "unlocked_${System.currentTimeMillis()}"); msg = "" } else msg = "⚠ Wrong password or not encrypted." },
                { val r = unlockPdf(ctx, u, pw); if (r != null) sharePdf(ctx, r, "unlocked_${System.currentTimeMillis()}") else msg = "⚠ Wrong password." })
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
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        uri = u; origSize = u?.let { readBytes(ctx, it)?.size?.toLong() } ?: 0L; output = null
    }
    // Receive a handed-off file from a previous tool in the chain
    LaunchedEffect(Unit) {
        WorkflowBus.consume()?.let { pf ->
            uri = pf.uri; origSize = readBytes(ctx, pf.uri)?.size?.toLong() ?: 0L
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a PDF" else "PDF selected ✓", accent) { picker.launch(arrayOf("application/pdf")) }
        if (uri != null) {
            Text("Original: ${bytesHuman(origSize)}", color = InkSoft, fontSize = 13.sp)
            Text("Re-renders pages at reduced resolution to shrink size.", color = InkFaint, fontSize = 12.sp)
            val u = uri!!
            ActionRow(accent,
                { compressPdf(ctx, u)?.let { output = it; savePdfToDownloads(ctx, it, "compressed_${System.currentTimeMillis()}") } },
                { compressPdf(ctx, u)?.let { output = it; sharePdf(ctx, it, "compressed_${System.currentTimeMillis()}") } })

            output?.let { bytes ->
                NextStepSuggestions(WorkflowGraph.nextSteps("pdf-compressor")) { step ->
                    cachePdfForHandoff(ctx, bytes)?.let { handoffUri ->
                        WorkflowBus.handOff(handoffUri, "application/pdf")
                        onOpenTool(step.toolId)
                    }
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
