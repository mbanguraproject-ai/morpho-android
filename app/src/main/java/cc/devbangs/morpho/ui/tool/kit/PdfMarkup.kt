package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
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
 * Marks are held per page in normalised coordinates and composited at export
 * resolution, so what is drawn on a preview lands in the same place on a
 * full-size page. The approach follows PdfSigner: render, draw onto a mutable
 * copy, write the pages back out through PdfDocument.
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

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { u ->
        if (u == null) return@rememberLauncherForActivityResult
        uri = u
        msg = ""
        strokes.clear(); texts.clear(); live = emptyList(); page = 0
        pages = renderPdf(ctx, u, 1000)
        loadError = if (pages.isEmpty()) pdfFailureReason(ctx, u) else ""
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

/** Composite the marks onto full-resolution pages and write a new document. */
private fun markupPdf(
    ctx: android.content.Context,
    uri: Uri,
    strokes: List<InkStroke>,
    texts: List<TextMark>
): ByteArray? = try {
    val pages = renderPdf(ctx, uri, 1240)
    if (pages.isEmpty()) null else {
        val doc = PdfDocument()
        pages.forEachIndexed { index, source ->
            val out = source.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = AndroidCanvas(out)

            strokes.filter { it.page == index }.forEach { s ->
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = s.argb
                    alpha = s.alpha
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = s.widthFrac * out.width
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                if (s.pts.size > 1) {
                    val path = android.graphics.Path()
                    s.pts.forEachIndexed { i, p ->
                        val x = p.x * out.width
                        val y = p.y * out.height
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    canvas.drawPath(path, paint)
                }
            }

            texts.filter { it.page == index }.forEach { t ->
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = t.argb
                    textSize = out.width * 0.028f
                    isFakeBoldText = true
                }
                canvas.drawText(t.text, t.at.x * out.width, t.at.y * out.height, paint)
            }

            val info = PdfDocument.PageInfo
                .Builder(out.width, out.height, doc.pages.size + 1).create()
            val p = doc.startPage(info)
            p.canvas.drawBitmap(out, 0f, 0f, null)
            doc.finishPage(p)
        }
        val bos = ByteArrayOutputStream()
        doc.writeTo(bos)
        doc.close()
        bos.toByteArray()
    }
} catch (e: Exception) {
    null
}
