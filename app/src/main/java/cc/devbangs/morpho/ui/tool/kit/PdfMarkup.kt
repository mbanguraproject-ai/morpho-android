package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

fun hasMarkupTool(id: String): Boolean = id in setOf("pdf-annotator", "pdf-editor")

private enum class Mode(val label: String) { PEN("Draw"), HIGHLIGHT("Highlight"), TEXT("Text") }

/** Positions are normalised 0..1 so the preview and the export agree at any size. */
private data class InkStroke(
    val page: Int, val pts: List<Offset>, val argb: Int,
    val widthFrac: Float, val alpha: Int
)

private data class TextMark(
    val page: Int, val at: Offset, val text: String, val argb: Int
)

private val SWATCHES = listOf(
    Color(0xFF0B0D12), Color(0xFF1A46E5), Color(0xFFB4231E), Color(0xFF15803D)
)
private val HIGHLIGHT_SWATCHES = listOf(
    Color(0xFFFFE066), Color(0xFFA7F3D0), Color(0xFFBFDBFE), Color(0xFFFBCFE8)
)

/**
 * PDF markup - draw, highlight and place text on a page.
 *
 * Serves both pdf-annotator and pdf-editor. They were separate registry
 * entries with separate placeholders, but they describe one capability with
 * different emphasis, so this is one tool that opens on the mode each id
 * implies. Building them separately would mean two of nearly the same screen.
 *
 * Marks are held per page as fractions of the page, so the same numbers drive
 * the preview and the output. Pages are rasterised for the preview only; the
 * export appends real vector content to the original document, which keeps its
 * text selectable and its size unchanged.
 */
@Composable
fun PdfMarkupTool(id: String, accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var page by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var mode by remember {
        mutableStateOf(if (id == "pdf-editor") Mode.TEXT else Mode.HIGHLIGHT)
    }
    var ink by remember { mutableStateOf(SWATCHES[0]) }
    var highlight by remember { mutableStateOf(HIGHLIGHT_SWATCHES[0]) }
    var pendingText by remember { mutableStateOf("") }

    val strokes = remember { mutableStateListOf<InkStroke>() }
    val texts = remember { mutableStateListOf<TextMark>() }
    var live by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    // One path in, whether the file was picked here or handed over by the
    // master sheet. Previously only the picker loaded anything, so arriving
    // with a file meant choosing it a second time.
    fun load(u: Uri) {
        uri = u
        msg = ""
        strokes.clear(); texts.clear(); live = emptyList(); page = 0
        pages = renderPdf(ctx, u, 1000)
        loadError = if (pages.isEmpty()) pdfFailureReason(ctx, u) else ""
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { u -> if (u != null) load(u) }

    LaunchedEffect(Unit) {
        cc.devbangs.morpho.workflow.WorkflowBus.consume()?.let { handed ->
            load(bytesToTempUri(ctx, handed.bytes, handed.mime))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose a PDF", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }

        if (loadError.isNotEmpty()) ToolErrorCard(
            "Couldn't open this PDF", loadError, accent,
            "Choose another", { loadError = ""; picker.launch(arrayOf("application/pdf")) }
        )

        val current = pages.getOrNull(page)
        if (current != null) {
            FieldLabel("TOOL")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Mode.entries.forEach { m ->
                    Box(Modifier.weight(1f)) {
                        ModeChip(m.label, mode == m, accent) { mode = m }
                    }
                }
            }

            FieldLabel(if (mode == Mode.HIGHLIGHT) "HIGHLIGHT COLOUR" else "COLOUR")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                val palette = if (mode == Mode.HIGHLIGHT) HIGHLIGHT_SWATCHES else SWATCHES
                val selected = if (mode == Mode.HIGHLIGHT) highlight else ink
                palette.forEach { c ->
                    Box(
                        Modifier.size(38.dp).clip(Shape.chip).background(c)
                            .border(
                                if (c == selected) 3.dp else 1.dp,
                                if (c == selected) accent else PaperLine,
                                Shape.chip
                            )
                            .clickable {
                                if (mode == Mode.HIGHLIGHT) highlight = c else ink = c
                            }
                    )
                }
            }

            if (mode == Mode.TEXT) {
                FieldLabel("TEXT TO PLACE")
                ToolInput(pendingText, { pendingText = it }, "Type, then tap the page", minLines = 1)
            }

            Text(
                when (mode) {
                    Mode.TEXT -> "Type above, then tap where it should go."
                    Mode.HIGHLIGHT -> "Drag across the page to highlight."
                    Mode.PEN -> "Draw directly on the page."
                },
                color = InkSoft, fontSize = 13.sp
            )

            Box(
                Modifier.fillMaxWidth()
                    .onGloballyPositioned { previewSize = it.size }
                    .pointerInput(page, mode, ink, highlight, pendingText) {
                        if (mode == Mode.TEXT) {
                            detectTapGestures { off ->
                                val w = previewSize.width; val h = previewSize.height
                                if (w > 0 && h > 0 && pendingText.isNotBlank()) {
                                    texts.add(
                                        TextMark(
                                            page, Offset(off.x / w, off.y / h),
                                            pendingText, ink.toArgb()
                                        )
                                    )
                                    pendingText = ""
                                }
                            }
                        } else {
                            detectDragGestures(
                                onDragStart = { live = listOf(it) },
                                onDrag = { change, _ ->
                                    live = live + change.position; change.consume()
                                },
                                onDragEnd = {
                                    val w = previewSize.width; val h = previewSize.height
                                    if (w > 0 && h > 0 && live.size > 1) {
                                        strokes.add(
                                            InkStroke(
                                                page = page,
                                                pts = live.map { Offset(it.x / w, it.y / h) },
                                                argb = (if (mode == Mode.HIGHLIGHT) highlight else ink).toArgb(),
                                                widthFrac = if (mode == Mode.HIGHLIGHT) 0.035f else 0.006f,
                                                alpha = if (mode == Mode.HIGHLIGHT) 110 else 255
                                            )
                                        )
                                    }
                                    live = emptyList()
                                }
                            )
                        }
                    }
            ) {
                Image(
                    current.asImageBitmap(), null,
                    Modifier.fillMaxWidth().clip(Shape.tile).background(Paper),
                    contentScale = ContentScale.FillWidth
                )
                Canvas(Modifier.matchParentSize()) {
                    val w = size.width; val h = size.height
                    strokes.filter { it.page == page }.forEach { s ->
                        if (s.pts.size > 1) {
                            val path = Path().apply {
                                moveTo(s.pts[0].x * w, s.pts[0].y * h)
                                s.pts.drop(1).forEach { lineTo(it.x * w, it.y * h) }
                            }
                            drawPath(
                                path,
                                Color(s.argb).copy(alpha = s.alpha / 255f),
                                style = Stroke(width = s.widthFrac * w, cap = StrokeCap.Round)
                            )
                        }
                    }
                    if (live.size > 1) {
                        val path = Path().apply {
                            moveTo(live[0].x, live[0].y)
                            live.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        val c = if (mode == Mode.HIGHLIGHT) highlight else ink
                        drawPath(
                            path,
                            c.copy(alpha = if (mode == Mode.HIGHLIGHT) 0.45f else 1f),
                            style = Stroke(
                                width = (if (mode == Mode.HIGHLIGHT) 0.035f else 0.006f) * w,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
                // The preview has to agree with the export or the tool is
                // guessing. Both now derive from the same fractions: size is
                // 2.8% of the page width, and the offset is applied in pixels
                // rather than through a hardcoded density figure, which was
                // only correct on one class of screen.
                //
                // The export draws from the text baseline while Compose draws
                // from the top, so the preview is lifted by roughly one cap
                // height to land in the same place.
                val density = LocalDensity.current
                texts.filter { it.page == page }.forEach { t ->
                    val sizePx = previewSize.width * 0.028f
                    Text(
                        t.text,
                        color = Color(t.argb),
                        fontSize = with(density) { sizePx.toSp() },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.offset {
                            androidx.compose.ui.unit.IntOffset(
                                (t.at.x * previewSize.width).toInt(),
                                (t.at.y * previewSize.height - sizePx * 0.8f).toInt()
                            )
                        }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageStep("chevron-left", page > 0, "Previous page") { page-- }
                Text(
                    "Page ${page + 1} of ${pages.size}",
                    color = InkSoft, fontSize = 13.sp,
                    modifier = Modifier.weight(1f).padding(horizontal = Space.sm)
                )
                PageStep("chevron-right", page < pages.lastIndex, "Next page") { page++ }
            }

            val marksHere = strokes.count { it.page == page } + texts.count { it.page == page }
            if (marksHere > 0) {
                Box(
                    Modifier.fillMaxWidth().clip(Shape.field)
                        .background(accent.copy(alpha = 0.10f))
                        .clickable {
                            val lastText = texts.lastOrNull { it.page == page }
                            val lastInk = strokes.lastOrNull { it.page == page }
                            // Whichever was added last on this page.
                            if (lastText != null && (lastInk == null ||
                                    texts.indexOf(lastText) >= strokes.indexOf(lastInk))
                            ) texts.remove(lastText) else if (lastInk != null) strokes.remove(lastInk)
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Undo last mark on this page", color = accent, fontSize = 14.sp) }
            }

            if (busy) ProcessingCard("Writing your PDF\u2026", accent)
            else ToolButton("Save marked PDF", accent) {
                if (strokes.isEmpty() && texts.isEmpty()) {
                    msg = "Add a mark first."
                    return@ToolButton
                }
                busy = true
                val u = uri!!
                val ink0 = strokes.toList()
                val txt0 = texts.toList()
                scope.launch {
                    val bytes = withContext(Dispatchers.Default) { markupPdf(ctx, u, ink0, txt0) }
                    if (bytes != null && bytes.isNotEmpty()) {
                        savePdfToDownloads(ctx, bytes, "marked_${System.currentTimeMillis()}")
                        msg = "Saved to Download/Morpho \u2713"
                    } else msg = "\u26a0 Could not write this PDF."
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

/** Local copy: every PickRow in the kit is private to its own file. */
@Composable
private fun PickRow(label: String, icon: String, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphoIcon(icon, tint = accent, size = 20.dp)
        Spacer(Modifier.width(Space.md))
        Text(label, color = accent, fontSize = 15.sp)
    }
}

@Composable
private fun ModeChip(label: String, on: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(Shape.field)
            .background(if (on) accent else PaperSunk)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (on) Paper else InkSoft, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PageStep(glyph: String, enabled: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(Shape.chip)
            .background(if (enabled) PaperSunk else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MorphoIcon(glyph, tint = if (enabled) Ink else InkFaint.copy(alpha = 0.4f),
            size = 18.dp, contentDescription = label)
    }
}

/**
 * Draw the marks onto the real PDF rather than rebuilding it from images.
 *
 * The first version rendered every page to a bitmap, drew on that, and wrote a
 * new document from the pictures. It worked, but the output lost all selectable
 * text, could not be searched, and grew several times larger than the original.
 *
 * PDFBox can append to a page's existing content stream, so the document keeps
 * its text, its fonts and its size, and the marks sit on top as real vector
 * content. Positions are stored as fractions of the page, so the same numbers
 * drive the preview and the output.
 */
private fun markupPdf(
    ctx: android.content.Context,
    uri: Uri,
    strokes: List<InkStroke>,
    texts: List<TextMark>
): ByteArray? = try {
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        PDDocument.load(input).use { doc ->
            for (index in 0 until doc.numberOfPages) {
                val pageStrokes = strokes.filter { it.page == index }
                val pageTexts = texts.filter { it.page == index }
                if (pageStrokes.isEmpty() && pageTexts.isEmpty()) continue

                val page = doc.getPage(index)
                val box = page.mediaBox
                val w = box.width
                val h = box.height
                val x0 = box.lowerLeftX
                val y0 = box.lowerLeftY

                // PDF space starts at the bottom left; the marks are stored from
                // the top left, as the preview measures them.
                fun px(fx: Float) = x0 + fx * w
                fun py(fy: Float) = y0 + h - fy * h

                PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true, true
                ).use { cs ->
                    pageStrokes.forEach { s ->
                        if (s.pts.size < 2) return@forEach
                        val gs = PDExtendedGraphicsState().apply {
                            strokingAlphaConstant = s.alpha / 255f
                            nonStrokingAlphaConstant = s.alpha / 255f
                        }
                        cs.setGraphicsStateParameters(gs)
                        // Float overload: the int one is deprecated in PDFBox.
                        cs.setStrokingColor(
                            android.graphics.Color.red(s.argb) / 255f,
                            android.graphics.Color.green(s.argb) / 255f,
                            android.graphics.Color.blue(s.argb) / 255f
                        )
                        cs.setLineWidth(s.widthFrac * w)
                        cs.setLineCapStyle(1)
                        cs.setLineJoinStyle(1)
                        cs.moveTo(px(s.pts[0].x), py(s.pts[0].y))
                        s.pts.drop(1).forEach { cs.lineTo(px(it.x), py(it.y)) }
                        cs.stroke()
                    }

                    if (pageTexts.isNotEmpty()) {
                        val gs = PDExtendedGraphicsState().apply {
                            nonStrokingAlphaConstant = 1f
                        }
                        cs.setGraphicsStateParameters(gs)
                    }
                    pageTexts.forEach { t ->
                        val safe = winAnsi(t.text)
                        if (safe.isEmpty()) return@forEach
                        cs.beginText()
                        cs.setFont(PDType1Font.HELVETICA_BOLD, w * 0.028f)
                        // Float overload: the int one is deprecated in PDFBox.
                        cs.setNonStrokingColor(
                            android.graphics.Color.red(t.argb) / 255f,
                            android.graphics.Color.green(t.argb) / 255f,
                            android.graphics.Color.blue(t.argb) / 255f
                        )
                        cs.newLineAtOffset(px(t.at.x), py(t.at.y))
                        cs.showText(safe)
                        cs.endText()
                    }
                }
            }
            val out = ByteArrayOutputStream()
            doc.save(out)
            out.toByteArray()
        }
    }
} catch (e: Exception) {
    null
}

/**
 * Helvetica in a PDF is WinAnsi encoded, and showText throws on anything it
 * cannot represent - an emoji or non-Latin script would have failed the whole
 * save. Unsupported characters are dropped rather than taking the document
 * down with them.
 */
private fun winAnsi(text: String): String =
    text.filter { it.code in 32..126 || it.code in 160..255 }
