package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt

fun hasImageTool(id: String): Boolean = id in setOf(
    "image-compressor","image-resizer","image-cropper","image-rotator","image-blur",
    "sharpen-image","watermark-image","exif-remover","image-metadata-viewer",
    "batch-image-converter","thumbnail-creator"
)

@Composable
fun ImageTool(id: String, accent: Color) {
    // Batch has a different shape from every other image tool: many files in,
    // no single preview, no per-image controls. It gets its own screen rather
    // than being bent through the one-file picker, which is what made it a
    // single-image converter wearing a batch name.
    if (id == "batch-image-converter") {
        BatchConvertTool(accent)
        return
    }
    val ctx = LocalContext.current
    var src by remember { mutableStateOf<Bitmap?>(null) }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            picked = uri
            val decoded = decodeBitmap(ctx, uri)
            src = decoded
            // decodeBitmap returns null on any failure; without this the user
            // picked a file and the screen simply did nothing.
            loadFailed = decoded == null
        }
    }
    val pick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    // Receive a file handed over by a previous tool. Only PDF tools consumed
    // the bus before, so an image handed to an image tool was silently dropped
    // and the user arrived at an empty picker with their file gone.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        cc.devbangs.morpho.workflow.WorkflowBus.consume()?.let { handed ->
            val decoded = decodeBitmapBytes(handed.bytes)
            src = decoded
            loadFailed = decoded == null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        // picker / input preview
        ImagePickPreview(
            bitmap = src,
            accent = accent,
            onPick = { loadFailed = false; pick() },
            onClear = { src = null; picked = null; loadFailed = false }
        )
        if (loadFailed) {
            ToolErrorCard(
                title = "Couldn't open that image",
                body = "It may be in a format Morpho can't read, or the file may be damaged. " +
                    "Try another image.",
                accent = accent,
                actionLabel = "Choose another",
                onAction = { loadFailed = false; pick() }
            )
        }
        val bmp = src
        if (bmp != null) {
            when (id) {
                "image-metadata-viewer" -> MetadataBody(bmp, picked, accent)
                "image-cropper" -> CropBody(bmp, accent)
                "watermark-image" -> WatermarkBody(bmp, accent)
                else -> TransformBody(id, bmp, picked, accent)
            }
        }
    }
}

@Composable
internal fun ImagePickPreview(
    bitmap: Bitmap?,
    accent: Color,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    if (bitmap == null) {
        // EMPTY — the drop-zone
        Column(
            Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.07f))
                .border(1.5.dp, accent.copy(alpha = 0.22f), Shape.card)
                .clickable(onClick = onPick).padding(vertical = 30.dp, horizontal = Space.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(52.dp).clip(Shape.chip).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { MorphoIcon("image-add", tint = accent, size = 26.dp) }
            Spacer(Modifier.height(12.dp))
            Text("Choose an image", color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text("Tap to select", color = InkFaint, fontSize = 12.sp)
        }
    } else {
        // LOADED — the image becomes the preview with Change + Clear controls
        Box(
            Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 340.dp)
                .clip(Shape.card).background(PaperSunk)
                .border(1.5.dp, accent.copy(alpha = 0.22f), Shape.card)
        ) {
            Image(
                bitmap.asImageBitmap(), null,
                Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 340.dp),
                contentScale = ContentScale.Fit
            )
            // control pills, top-right
            Row(
                Modifier.align(Alignment.TopEnd).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Change
                Row(
                    Modifier.clip(Shape.pill).background(accent)
                        .clickable(onClick = onPick).padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MorphoIcon("image-add", tint = Paper, size = 14.dp)
                    Spacer(Modifier.width(5.dp))
                    Text("Change", color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                // Clear
                Box(
                    Modifier.clip(Shape.pill).background(Ink.copy(alpha = 0.55f))
                        .clickable(onClick = onClear).padding(horizontal = 11.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MorphoIcon("close", tint = Paper, size = 14.dp)
                }
            }
        }
    }
}

/** For transform tools: controls + preview + save/share. */
@Composable
private fun TransformBody(id: String, src: Bitmap, srcUri: Uri?, accent: Color) {
    val ctx = LocalContext.current
    // per-tool parameters
    // Shrinking is the compressor's whole job, so it starts lower; the rest
    // should not quietly degrade an image the user only asked to rotate.
    var quality by remember(id) { mutableStateOf(if (id == "image-compressor") 80 else 95) }
    var scalePct by remember { mutableStateOf(100) }
    var rotation by remember { mutableStateOf(0) }
    var strength by remember { mutableStateOf(50) }
    var sharpRadius by remember { mutableStateOf(2) }

    // Output format and quality are the user's choice now. Every tool but the
    // compressor hard-coded JPEG 92, so a PNG fed to the resizer came back
    // lossy with no way to say otherwise; and exif-remover forced PNG, turning
    // a 2 MB photo into a far larger file just to strip a few bytes of
    // metadata. Both are defaults here, not rules.
    var fmtKey by remember(id) { mutableStateOf(if (id == "exif-remover") "PNG" else "JPEG") }
    val fmt = compressFormatOf(fmtKey)
    val q = if (fmtKey == "PNG") 100 else quality

    // Both the transform and the size readout are expensive on a large photo:
    // the readout is a full re-encode. Running them in composition meant every
    // control tap - and every watermark keystroke - blocked the main thread.
    // LaunchedEffect cancels on each parameter change, so the leading delay
    // debounces held taps and typing instead of queueing work per character.
    var out by remember(src) { mutableStateOf(src) }
    var outSize by remember(src) { mutableStateOf(0L) }
    var srcSize by remember(src) { mutableStateOf(0L) }
    var working by remember(src) { mutableStateOf(true) }

    // Was a JPEG 100 re-encode of the decoded bitmap - a size the file never
    // had. Reads the real one now, and stays 0 for a bitmap handed over from
    // another tool, where there is no file to compare against.
    LaunchedEffect(src, srcUri) {
        srcSize = withContext(Dispatchers.IO) { sourceFileSize(ctx, srcUri) }
    }
    LaunchedEffect(id, src, quality, scalePct, rotation, strength, sharpRadius, fmt, q) {
        working = true
        delay(140)
        val result = withContext(Dispatchers.Default) {
            // Transforms allocate pixel buffers proportional to the image, and
            // the app declares no largeHeap, so a big photo can exhaust the
            // heap here. Unguarded, that was an outright crash.
            val bmp = try {
                applyTransform(id, src, scalePct, rotation, strength, sharpRadius)
            } catch (e: Exception) { src } catch (e: OutOfMemoryError) { src }
            bmp to (
                try { bitmapBytes(bmp, fmt, q) }
                catch (e: Exception) { 0L } catch (e: OutOfMemoryError) { 0L }
            )
        }
        out = result.first
        outSize = result.second
        working = false
    }

    // controls
    when (id) {
        "image-resizer" -> StepControl("SCALE %", scalePct, listOf(25,50,75,100), accent) { scalePct = it }
        "thumbnail-creator" -> StepControl("SIZE %", scalePct, listOf(10,25,40,60), accent) { scalePct = it }
        "image-rotator" -> StepControl("ROTATE°", rotation, listOf(0,90,180,270), accent) { rotation = it }
        "image-blur" -> StepControl("STRENGTH", strength, listOf(25,50,75,100), accent) { strength = it }
        "sharpen-image" -> Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            StepControl("AMOUNT", strength, listOf(25,50,75,100), accent) { strength = it }
            StepControl("RADIUS PX", sharpRadius, listOf(1,2,4,8), accent) { sharpRadius = it }
        }
    }

    // Every other tool file in the app shows ProcessingCard while it works;
    // this one never did, which is why the image tools had no working state
    // and never registered with ToolWork, whose busy flag gates the
    // leave-confirmation in ToolScreen.
    //
    // Most taps finish well under a second, and flashing the card for those
    // would be worse than nothing, so it appears only once work has run long
    // enough to be worth reporting - which is also when the confirmation
    // actually matters.
    var showBusy by remember(src) { mutableStateOf(false) }
    LaunchedEffect(working) {
        if (working) { delay(260); showBusy = true } else showBusy = false
    }
    var saving by remember(src) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutputControls(fmtKey, quality, accent, { fmtKey = it }, { quality = it })

    // preview
    if (showBusy || saving) ProcessingCard(
        if (saving) "Saving your image" else "Processing image", accent
    )
    else Box(
        Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 320.dp)
            .clip(Shape.card).background(PaperSunk),
        contentAlignment = Alignment.Center
    ) {
        Image(out.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
    }

    // stats
    //
    // The comparison rows only appear when there is a real file to compare
    // against. Showing "Saved 75%" against a figure the file never had was
    // worse than showing nothing.
    val stats = mutableListOf(
        "Dimensions" to (out.width.toString() + "\u00d7" + out.height),
        (if (id == "image-compressor") "New size" else "Output") to sizeLabel(working, outSize)
    )
    if (srcSize > 0L) {
        stats.add("Original" to sizeLabel(false, srcSize))
        stats.add(
            "Saved" to if (working || outSize <= 0L) "\u2026"
            else ((100 - outSize * 100 / srcSize).coerceAtLeast(0).toString() + "%")
        )
    } else {
        stats.add("Format" to fmtKey)
        stats.add("Quality" to if (fmtKey == "PNG") "Lossless" else quality.toString())
    }
    StatGrid(stats, accent)

    // actions
    SaveShareRow(
        accent, enabled = !working && !saving,
        onSave = { runSaveAsync(scope, ctx, "morpho", fmt, q, { saving = it }) { out } },
        onShare = { runShareAsync(scope, ctx, "morpho", fmt, q, { saving = it }) { out } }
    )
}

/**
 * Image Cropper.
 *
 * This tool was registered, routed and rendered, but had no branch in either
 * the control block or applyTransform, so it fell through to `else -> src` -
 * the same no-op branch the compressor takes. That is why cropping and
 * compressing produced identical output: they were running identical code.
 *
 * The crop rect is held in normalised 0..1 coordinates so it survives the
 * preview being laid out at any size, and is only converted to pixels at the
 * moment a bitmap is produced. Nothing here is shared with TransformBody, so
 * the other ten image tools cannot be affected by it.
 */
@Composable
private fun CropBody(src: Bitmap, accent: Color) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val ratios = listOf("Free", "1:1", "4:3", "3:2", "16:9", "9:16")
    var ratio by remember(src) { mutableStateOf("Free") }

    // Crop rect, normalised to the source. Starts as a small inset so the
    // handles are visible and grabbable rather than pinned to the edges.
    var cl by remember(src) { mutableStateOf(0.06f) }
    var ct by remember(src) { mutableStateOf(0.06f) }
    var cr by remember(src) { mutableStateOf(0.94f) }
    var cb by remember(src) { mutableStateOf(0.94f) }
    var active by remember(src) { mutableStateOf(0) }
    var fmtKey by remember(src) { mutableStateOf("JPEG") }
    var quality by remember(src) { mutableStateOf(95) }
    var saving by remember(src) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val fmt = compressFormatOf(fmtKey)
    val q = if (fmtKey == "PNG") 100 else quality

    val k = aspectK(ratio, src)
    // Recomposes on every drag frame, so don't rewrap the bitmap each time.
    val img = remember(src) { src.asImageBitmap() }

    // Snap to the largest centred rect of the chosen ratio.
    LaunchedEffect(ratio, src) {
        val kk = aspectK(ratio, src) ?: return@LaunchedEffect
        var h = 1f
        var w = kk * h
        if (w > 1f) { w = 1f; h = w / kk }
        cl = (1f - w) / 2f; cr = cl + w
        ct = (1f - h) / 2f; cb = ct + h
    }

    val outW = ((cr - cl) * src.width).roundToInt().coerceAtLeast(1)
    val outH = ((cb - ct) * src.height).roundToInt().coerceAtLeast(1)

    // Re-encoding to measure bytes is expensive, so it is debounced and run
    // off the main thread - a drag must not re-encode per frame.
    var outSize by remember(src) { mutableStateOf(0L) }
    var measuring by remember(src) { mutableStateOf(true) }
    LaunchedEffect(src, cl, ct, cr, cb, fmt, q) {
        measuring = true
        delay(260)
        outSize = withContext(Dispatchers.Default) {
            try {
                bitmapBytes(cropOf(src, cl, ct, cr, cb), fmt, q)
            } catch (e: Exception) { 0L } catch (e: OutOfMemoryError) { 0L }
        }
        measuring = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column {
            FieldLabel("ASPECT RATIO")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ratios.forEach { name ->
                    val on = name == ratio
                    Box(
                        Modifier.weight(1f).clip(Shape.field)
                            .background(if (on) accent else accent.copy(alpha = 0.12f))
                            .clickable { ratio = name }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name,
                            color = if (on) Paper else InkSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Preview sized to the image so normalised coords map 1:1 onto it.
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(340.dp).clip(Shape.card).background(PaperSunk),
            contentAlignment = Alignment.Center
        ) {
            val maxWpx = constraints.maxWidth.toFloat()
            val maxHpx = with(density) { 340.dp.toPx() }
            val fit = minOf(maxWpx / src.width, maxHpx / src.height)
            val wDp = with(density) { (src.width * fit).toDp() }
            val hDp = with(density) { (src.height * fit).toDp() }

            Box(Modifier.width(wDp).height(hDp)) {
                Image(img, null, Modifier.matchParentSize())
                Canvas(
                    Modifier.matchParentSize().pointerInput(src, ratio) {
                        val grab = 30.dp.toPx()
                        detectDragGestures(
                            onDragStart = { off ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val xs = floatArrayOf(cl * w, cr * w, cl * w, cr * w)
                                val ys = floatArrayOf(ct * h, ct * h, cb * h, cb * h)
                                var best = -1
                                var bestD = grab
                                for (i in 0..3) {
                                    val d = hypot(off.x - xs[i], off.y - ys[i])
                                    if (d < bestD) { bestD = d; best = i }
                                }
                                active = when {
                                    best >= 0 -> best + 1
                                    off.x >= cl * w && off.x <= cr * w &&
                                        off.y >= ct * h && off.y <= cb * h -> 5
                                    else -> 0
                                }
                            },
                            onDragEnd = { active = 0 },
                            onDragCancel = { active = 0 }
                        ) { change, drag ->
                            change.consume()
                            val dx = drag.x / size.width.toFloat()
                            val dy = drag.y / size.height.toFloat()
                            val minS = 0.06f
                            when (active) {
                                1 -> if (k == null) {
                                    cl = (cl + dx).coerceIn(0f, cr - minS)
                                    ct = (ct + dy).coerceIn(0f, cb - minS)
                                } else {
                                    var w = (cr - (cl + dx)).coerceIn(minS, cr)
                                    var h = w / k
                                    if (h > cb) { h = cb; w = h * k }
                                    cl = cr - w; ct = cb - h
                                }
                                2 -> if (k == null) {
                                    cr = (cr + dx).coerceIn(cl + minS, 1f)
                                    ct = (ct + dy).coerceIn(0f, cb - minS)
                                } else {
                                    var w = ((cr + dx) - cl).coerceIn(minS, 1f - cl)
                                    var h = w / k
                                    if (h > cb) { h = cb; w = h * k }
                                    cr = cl + w; ct = cb - h
                                }
                                3 -> if (k == null) {
                                    cl = (cl + dx).coerceIn(0f, cr - minS)
                                    cb = (cb + dy).coerceIn(ct + minS, 1f)
                                } else {
                                    var w = (cr - (cl + dx)).coerceIn(minS, cr)
                                    var h = w / k
                                    if (ct + h > 1f) { h = 1f - ct; w = h * k }
                                    cl = cr - w; cb = ct + h
                                }
                                4 -> if (k == null) {
                                    cr = (cr + dx).coerceIn(cl + minS, 1f)
                                    cb = (cb + dy).coerceIn(ct + minS, 1f)
                                } else {
                                    var w = ((cr + dx) - cl).coerceIn(minS, 1f - cl)
                                    var h = w / k
                                    if (ct + h > 1f) { h = 1f - ct; w = h * k }
                                    cr = cl + w; cb = ct + h
                                }
                                5 -> {
                                    val mx = dx.coerceIn(-cl, 1f - cr)
                                    val my = dy.coerceIn(-ct, 1f - cb)
                                    cl += mx; cr += mx; ct += my; cb += my
                                }
                            }
                        }
                    }
                ) {
                    val w = size.width
                    val h = size.height
                    val rl = cl * w; val rt = ct * h
                    val rr = cr * w; val rb = cb * h
                    val scrim = Color.Black.copy(alpha = 0.46f)
                    drawRect(scrim, size = GSize(w, rt))
                    drawRect(scrim, topLeft = Offset(0f, rb), size = GSize(w, h - rb))
                    drawRect(scrim, topLeft = Offset(0f, rt), size = GSize(rl, rb - rt))
                    drawRect(scrim, topLeft = Offset(rr, rt), size = GSize(w - rr, rb - rt))

                    // Rule-of-thirds guides, the standard framing aid.
                    for (i in 1..2) {
                        val gx = rl + (rr - rl) * i / 3f
                        val gy = rt + (rb - rt) * i / 3f
                        drawLine(Color.White.copy(alpha = 0.34f), Offset(gx, rt), Offset(gx, rb), 1f)
                        drawLine(Color.White.copy(alpha = 0.34f), Offset(rl, gy), Offset(rr, gy), 1f)
                    }
                    drawRect(
                        Color.White, topLeft = Offset(rl, rt), size = GSize(rr - rl, rb - rt),
                        style = Stroke(width = 2f)
                    )
                    val armPx = 18f
                    listOf(
                        Triple(rl, rt, 1), Triple(rr, rt, 2),
                        Triple(rl, rb, 3), Triple(rr, rb, 4)
                    ).forEach { (hx, hy, corner) ->
                        val sx = if (corner == 1 || corner == 3) 1f else -1f
                        val sy = if (corner == 1 || corner == 2) 1f else -1f
                        drawLine(Color.White, Offset(hx, hy), Offset(hx + armPx * sx, hy), 5f)
                        drawLine(Color.White, Offset(hx, hy), Offset(hx, hy + armPx * sy), 5f)
                    }
                }
            }
        }

        OutputControls(fmtKey, quality, accent, { fmtKey = it }, { quality = it })

        if (saving) ProcessingCard("Saving your image", accent)
        else StatGrid(listOf(
            "Crop size" to "${outW}\u00d7${outH}",
            "Output" to if (measuring || outSize <= 0L) "\u2026" else bytesHuman(outSize),
            "Source" to "${src.width}\u00d7${src.height}",
            "Ratio" to ratio
        ), accent)

        // The crop itself now happens on the IO thread too - it was building a
        // full-size bitmap in the click handler.
        SaveShareRow(
            accent, enabled = !saving,
            onSave = {
                runSaveAsync(scope, ctx, "morpho_crop", fmt, q, { saving = it }) {
                    cropOf(src, cl, ct, cr, cb)
                }
            },
            onShare = {
                runShareAsync(scope, ctx, "morpho_crop", fmt, q, { saving = it }) {
                    cropOf(src, cl, ct, cr, cb)
                }
            }
        )
        Box(Modifier.fillMaxWidth()) {
            OutlineButton("Reset crop", accent) {
                ratio = "Free"
                cl = 0.06f; ct = 0.06f; cr = 0.94f; cb = 0.94f
            }
        }
    }
}

/**
 * Normalised aspect factor: width = k * height in 0..1 space. The source
 * aspect has to be folded in, because a 3:2 crop of a portrait photo is not
 * 3:2 of the normalised square.
 */
private fun aspectK(name: String, src: Bitmap): Float? {
    val a = when (name) {
        "1:1" -> 1f
        "4:3" -> 4f / 3f
        "3:2" -> 3f / 2f
        "16:9" -> 16f / 9f
        "9:16" -> 9f / 16f
        else -> return null
    }
    return a * src.height / src.width
}

/** Normalised rect to real pixels, clamped so it can never leave the source. */
private fun cropOf(src: Bitmap, l: Float, t: Float, r: Float, b: Float): Bitmap {
    val x = (l * src.width).roundToInt().coerceIn(0, src.width - 1)
    val y = (t * src.height).roundToInt().coerceIn(0, src.height - 1)
    val w = ((r - l) * src.width).roundToInt().coerceIn(1, src.width - x)
    val h = ((b - t) * src.height).roundToInt().coerceIn(1, src.height - y)
    return Bitmap.createBitmap(src, x, y, w, h)
}

/**
 * Watermark Image.
 *
 * The upgrade in ed8c3b5 went to pdf-watermark and never reached this tool,
 * which kept its single control: the text. Colour, opacity, size, angle and
 * placement were all hardcoded - white, alpha 150, width/14, bottom-right.
 *
 * One placement mode cannot serve everyone. A store or photographer marking
 * product shots wants a specific corner; a firm or an office marking a scanned
 * document wants DRAFT or CONFIDENTIAL running diagonally, repeated so it
 * cannot be cropped off. So placement is a 3x3 grid plus a repeat mode, and
 * angle is a control rather than an assumption.
 */
@Composable
private fun WatermarkBody(src: Bitmap, accent: Color) {
    val ctx = LocalContext.current
    var mode by remember(src) { mutableStateOf("Text") }
    var text by remember(src) { mutableStateOf("") }
    var wmColor by remember(src) { mutableStateOf(AColor.WHITE) }
    var opacity by remember(src) { mutableStateOf(50) }
    var sizePct by remember(src) { mutableStateOf(8) }
    var angle by remember(src) { mutableStateOf(0) }
    var position by remember(src) { mutableStateOf(8) }   // bottom-right
    var tile by remember(src) { mutableStateOf(false) }

    // Logo mode. Capped at 1024 on intake because it is scaled down to a
    // fraction of the base image anyway, and it inherits the orientation fix.
    var logo by remember(src) { mutableStateOf<Bitmap?>(null) }
    var logoScale by remember(src) { mutableStateOf(20) }
    val logoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) logo = decodeBitmap(ctx, uri, 1024) }
    val pickLogo = {
        logoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val ready = if (mode == "Logo") logo != null else text.isNotBlank()
    var fmtKey by remember(src) { mutableStateOf("JPEG") }
    var quality by remember(src) { mutableStateOf(95) }
    var saving by remember(src) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val fmt = compressFormatOf(fmtKey)
    val q = if (fmtKey == "PNG") 100 else quality

    var out by remember(src) { mutableStateOf(src) }
    var outSize by remember(src) { mutableStateOf(0L) }
    var working by remember(src) { mutableStateOf(false) }
    // Every other tool file in the app shows ProcessingCard while it works;
    // this one never did, which is why the image tools had no working state
    // and never registered with ToolWork, whose busy flag gates the
    // leave-confirmation in ToolScreen.
    //
    // Most taps finish well under a second, and flashing the card for those
    // would be worse than nothing, so it appears only once work has run long
    // enough to be worth reporting - which is also when the confirmation
    // actually matters.
    var showBusy by remember(src) { mutableStateOf(false) }
    LaunchedEffect(working) {
        if (working) { delay(260); showBusy = true } else showBusy = false
    }


    // Rendering and measuring both cost a full pass over the bitmap, so they
    // are debounced off the main thread - typing must not re-render per key.
    LaunchedEffect(
        src, mode, text, wmColor, opacity, sizePct, angle, position, tile,
        logo, logoScale, fmt, q
    ) {
        working = true
        delay(170)
        val r = withContext(Dispatchers.Default) {
            val bmp = try {
                val lg = logo
                if (mode == "Logo") {
                    if (lg == null) src
                    else logoWatermarkOf(src, lg, opacity, logoScale, position, tile, angle.toFloat())
                } else {
                    watermarkOf(src, text, wmColor, opacity, sizePct, position, tile, angle.toFloat())
                }
            } catch (e: Exception) { src } catch (e: OutOfMemoryError) { src }
            bmp to (try { bitmapBytes(bmp, fmt, q) } catch (e: Exception) { 0L })
        }
        out = r.first
        outSize = r.second
        working = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column {
            FieldLabel("MARK WITH")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                listOf("Text", "Logo").forEach { m ->
                    val on = m == mode
                    Box(
                        Modifier.weight(1f).clip(Shape.field)
                            .background(if (on) accent else accent.copy(alpha = 0.12f))
                            .clickable { mode = m }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            m, color = if (on) Paper else InkSoft,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (mode == "Text") {
            Column {
                FieldLabel("WATERMARK TEXT")
                ToolInput(text, { text = it }, "\u00a9 Your name, DRAFT, CONFIDENTIAL\u2026", minLines = 1)
            }

            Column {
                FieldLabel("COLOUR")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AColor.WHITE, AColor.BLACK, AColor.rgb(200, 0, 0),
                        AColor.rgb(26, 70, 229), AColor.rgb(214, 138, 15)
                    ).forEach { swatch ->
                        Box(
                            Modifier.size(38.dp).clip(Shape.chip).background(Color(swatch))
                                .border(
                                    if (swatch == wmColor) 3.dp else 1.dp,
                                    if (swatch == wmColor) accent else PaperLine,
                                    Shape.chip
                                )
                                .clickable { wmColor = swatch }
                        )
                    }
                }
            }
        } else {
            Column {
                FieldLabel("LOGO")
                val lg = logo
                if (lg == null) {
                    Row(
                        Modifier.fillMaxWidth().clip(Shape.field)
                            .background(accent.copy(alpha = 0.09f))
                            .border(1.5.dp, accent.copy(alpha = 0.22f), Shape.field)
                            .clickable { pickLogo() }
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MorphoIcon("image-add", tint = accent, size = 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Choose a logo", color = accent,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "A PNG with a transparent background works best",
                                color = InkFaint, fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            lg.asImageBitmap(), null,
                            Modifier.size(46.dp).clip(Shape.chip),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${lg.width}\u00d7${lg.height}",
                            color = InkSoft, fontSize = 13.sp, modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier.clip(Shape.pill).background(accent)
                                .clickable { pickLogo() }
                                .padding(horizontal = 13.dp, vertical = 7.dp)
                        ) {
                            Text(
                                "Change", color = Paper,
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Box(
                            Modifier.clip(Shape.pill).background(Ink.copy(alpha = 0.55f))
                                .clickable { logo = null }
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) { MorphoIcon("close", tint = Paper, size = 13.dp) }
                    }
                }
            }
        }

        StepControl("OPACITY %", opacity, listOf(15, 30, 50, 75), accent) { opacity = it }
        if (mode == "Text") {
            StepControl("SIZE %", sizePct, listOf(3, 5, 8, 12), accent) { sizePct = it }
        } else {
            StepControl("LOGO WIDTH %", logoScale, listOf(10, 20, 30, 45), accent) { logoScale = it }
        }
        StepControl("ANGLE\u00b0", angle, listOf(0, 15, 30, 45), accent) { angle = it }

        Column {
            FieldLabel(if (tile) "PLACEMENT: REPEATING" else "PLACEMENT")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in 0..2) {
                            val idx = row * 3 + col
                            val on = !tile && position == idx
                            Box(
                                Modifier.weight(1f).height(34.dp).clip(Shape.chip)
                                    .background(
                                        when {
                                            on -> accent
                                            tile -> PaperSunk
                                            else -> accent.copy(alpha = 0.10f)
                                        }
                                    )
                                    .clickable { position = idx; tile = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier.size(if (on) 9.dp else 6.dp).clip(Shape.pill)
                                        .background(if (on) Paper else InkFaint)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(Shape.field)
                .background(if (tile) accent.copy(alpha = 0.12f) else PaperSunk)
                .clickable { tile = !tile }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MorphoIcon(
                if (tile) "check" else "tab-grid",
                tint = if (tile) accent else InkFaint, size = 17.dp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Repeat across the image",
                color = if (tile) accent else InkSoft, fontSize = 14.sp
            )
        }

        if (showBusy || saving) ProcessingCard(
            if (saving) "Saving your image" else "Applying watermark", accent
        )
        else Box(
            Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 320.dp)
                .clip(Shape.card).background(PaperSunk),
            contentAlignment = Alignment.Center
        ) {
            Image(out.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        }

        OutputControls(fmtKey, quality, accent, { fmtKey = it }, { quality = it })

        StatGrid(listOf(
            "Dimensions" to "${out.width}\u00d7${out.height}",
            "Output" to if (working || outSize <= 0L) "\u2026" else bytesHuman(outSize),
            "Placement" to if (tile) "Repeating" else PLACEMENT_NAMES[position],
            "Angle" to "$angle\u00b0"
        ), accent)

        SaveShareRow(
            accent, enabled = ready && !working && !saving,
            onSave = { runSaveAsync(scope, ctx, "morpho_wm", fmt, q, { saving = it }) { out } },
            onShare = { runShareAsync(scope, ctx, "morpho_wm", fmt, q, { saving = it }) { out } }
        )
    }
}

/**
 * Draw a logo watermark. Pure, safe off the main thread.
 *
 * The logo is scaled once with filtering rather than per tile, so a repeated
 * mark costs one resample instead of hundreds and keeps its edges clean.
 * Alpha is applied through the paint, so a transparent PNG composites over
 * the photo instead of arriving on a white block.
 */
private fun logoWatermarkOf(
    b: Bitmap,
    logo: Bitmap,
    opacityPct: Int,
    scalePct: Int,
    position: Int,
    tile: Boolean,
    angle: Float
): Bitmap {
    if (logo.width <= 0 || logo.height <= 0) return b
    val out = b.copy(Bitmap.Config.ARGB_8888, true) ?: return b
    val targetW = (out.width * scalePct / 100f).coerceAtLeast(8f)
    val sw = targetW.roundToInt().coerceAtLeast(1)
    val sh = (logo.height * (targetW / logo.width)).roundToInt().coerceAtLeast(1)
    val scaled = try {
        Bitmap.createScaledBitmap(logo, sw, sh, true)
    } catch (e: Exception) { return out } catch (e: OutOfMemoryError) { return out }

    val c = Canvas(out)
    val p = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        alpha = (opacityPct * 255 / 100).coerceIn(8, 255)
    }
    val fw = sw.toFloat()
    val fh = sh.toFloat()

    if (tile) {
        val diag = hypot(out.width.toFloat(), out.height.toFloat())
        val stepX = fw + out.width * 0.10f
        val stepY = fh + out.height * 0.06f
        val cx = out.width / 2f
        val cy = out.height / 2f
        c.save()
        c.rotate(angle, cx, cy)
        var y = cy - diag
        while (y < cy + diag) {
            var x = cx - diag
            while (x < cx + diag) {
                c.drawBitmap(scaled, x, y, p)
                x += stepX
            }
            y += stepY
        }
        c.restore()
        return out
    }

    val margin = minOf(out.width, out.height) * 0.04f
    val x = when (position % 3) {
        0 -> margin
        1 -> (out.width - fw) / 2f
        else -> out.width - fw - margin
    }
    val y = when (position / 3) {
        0 -> margin
        1 -> (out.height - fh) / 2f
        else -> out.height - fh - margin
    }
    c.save()
    c.rotate(angle, x + fw / 2f, y + fh / 2f)
    c.drawBitmap(scaled, x, y, p)
    c.restore()
    return out
}

/**
 * Output format and quality, shared by every image tool that writes a file.
 *
 * Fields of work want different things out of the same picture: a store wants
 * WebP for its site and JPEG for a marketplace, an office wants PNG so text in
 * a scan stays lossless, a photographer wants JPEG near the top of the range.
 * Hard-coding one of those serves one of them.
 */
@Composable
private fun OutputControls(
    fmtKey: String,
    quality: Int,
    accent: Color,
    onFmt: (String) -> Unit,
    onQuality: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column {
            FieldLabel("OUTPUT FORMAT")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "JPEG" to "Photos",
                    "PNG" to "Lossless",
                    "WEBP" to "For web"
                ).forEach { (key, hint) ->
                    val on = key == fmtKey
                    Column(
                        Modifier.weight(1f).clip(Shape.field)
                            .background(if (on) accent else accent.copy(alpha = 0.12f))
                            .clickable { onFmt(key) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            key, color = if (on) Paper else InkSoft,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            hint, color = if (on) Paper.copy(alpha = 0.82f) else InkFaint,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        // PNG ignores the quality argument, so showing the control would be a
        // lie about what it does.
        if (fmtKey != "PNG") {
            StepControl("QUALITY", quality, listOf(40, 60, 80, 95), accent, onQuality)
        }
    }
}

/**
 * Save, off the main thread.
 *
 * saveToGallery compresses the whole bitmap, which takes seconds on a large
 * photo, and it was being called straight from the button's onClick - on the
 * main thread. That is what froze the UI and produced the "isn't responding"
 * dialog: an ANR, not a crash.
 *
 * reportSave shows toasts and posts a notification, so it has to stay on the
 * main thread; saveToGallery's existing report flag gives exactly that split.
 * markUsed still fires from reportSave, so the completion counter behind the
 * interstitial is unchanged.
 *
 * The scope belongs to the calling composable body, not to the button row, so
 * a save is not cancelled when the preview swaps to the processing card.
 */
private fun runSaveAsync(
    scope: CoroutineScope,
    ctx: android.content.Context,
    baseName: String,
    fmt: Bitmap.CompressFormat,
    quality: Int,
    onBusy: (Boolean) -> Unit,
    provide: () -> Bitmap
) {
    onBusy(true)
    scope.launch {
        val ok = withContext(Dispatchers.IO) {
            try {
                saveToGallery(
                    ctx, provide(), "${baseName}_${System.currentTimeMillis()}",
                    fmt, quality, report = false
                )
            } catch (e: Exception) { false } catch (e: OutOfMemoryError) { false }
        }
        reportSave(
            ctx, ok, "Image ready", "Your image was saved to your gallery.",
            "Saved to Pictures/Morpho", "Couldn't save the image"
        )
        onBusy(false)
    }
}

/** Share, off the main thread. Only the chooser runs on the main thread. */
private fun runShareAsync(
    scope: CoroutineScope,
    ctx: android.content.Context,
    baseName: String,
    fmt: Bitmap.CompressFormat,
    quality: Int,
    onBusy: (Boolean) -> Unit,
    provide: () -> Bitmap
) {
    onBusy(true)
    scope.launch {
        val uri = withContext(Dispatchers.IO) {
            try {
                writeShareFile(
                    ctx, provide(), "${baseName}_${System.currentTimeMillis()}", fmt, quality
                )
            } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
        }
        if (uri != null) launchShareIntent(ctx, uri, imageMime(fmt))
        else Toast.makeText(ctx, "Couldn't prepare the image", Toast.LENGTH_SHORT).show()
        onBusy(false)
    }
}

/**
 * Save and Share, disabled while anything is in flight.
 *
 * Both used to stay tappable while the tool was still rendering, so Share
 * could hand over a stale bitmap and a second Save could start on top of the
 * first.
 */
@Composable
private fun SaveShareRow(accent: Color, enabled: Boolean, onSave: () -> Unit, onShare: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Box(Modifier.weight(1f)) { ToolButton("Save", accent, enabled = enabled, onClick = onSave) }
        Box(Modifier.weight(1f)) { OutlineButton("Share", accent, enabled = enabled, onClick = onShare) }
    }
}

private val PLACEMENT_NAMES = listOf(
    "Top left", "Top", "Top right",
    "Left", "Centre", "Right",
    "Bottom left", "Bottom", "Bottom right"
)

/**
 * Draw the watermark. Pure, safe off the main thread.
 *
 * The shadow is picked against the mark's own luminance - a light mark gets a
 * dark halo and a dark mark a light one - because a fixed black shadow leaves
 * black text unreadable on a dark photo, which is half of what people mark.
 */
private fun watermarkOf(
    b: Bitmap,
    text: String,
    colorInt: Int,
    opacityPct: Int,
    sizePct: Int,
    position: Int,
    tile: Boolean,
    angle: Float
): Bitmap {
    if (text.isBlank()) return b
    val out = b.copy(Bitmap.Config.ARGB_8888, true) ?: return b
    val c = Canvas(out)
    val lum = (0.299f * AColor.red(colorInt) +
        0.587f * AColor.green(colorInt) +
        0.114f * AColor.blue(colorInt)) / 255f
    val p = Paint().apply {
        color = colorInt
        isAntiAlias = true
        textSize = (out.width * sizePct / 100f).coerceAtLeast(8f)
        alpha = (opacityPct * 255 / 100).coerceIn(8, 255)
        setShadowLayer(
            (out.width * sizePct / 100f * 0.09f).coerceAtLeast(2f), 0f, 0f,
            if (lum > 0.5f) AColor.argb(150, 0, 0, 0) else AColor.argb(150, 255, 255, 255)
        )
    }
    val tw = p.measureText(text)
    if (tw <= 0f) return out
    val fm = p.fontMetrics
    val th = fm.descent - fm.ascent

    if (tile) {
        val diag = hypot(out.width.toFloat(), out.height.toFloat())
        val stepX = tw + out.width * 0.10f
        val stepY = (th * 2.4f).coerceAtLeast(4f)
        val cx = out.width / 2f
        val cy = out.height / 2f
        c.save()
        c.rotate(angle, cx, cy)
        var y = cy - diag
        while (y < cy + diag) {
            var x = cx - diag
            while (x < cx + diag) {
                c.drawText(text, x, y, p)
                x += stepX
            }
            y += stepY
        }
        c.restore()
        return out
    }

    val margin = minOf(out.width, out.height) * 0.04f
    val x = when (position % 3) {
        0 -> margin
        1 -> (out.width - tw) / 2f
        else -> out.width - tw - margin
    }
    val baseline = when (position / 3) {
        0 -> margin - fm.ascent
        1 -> (out.height - th) / 2f - fm.ascent
        else -> out.height - margin - fm.descent
    }
    c.save()
    c.rotate(angle, x + tw / 2f, baseline + (fm.ascent + fm.descent) / 2f)
    c.drawText(text, x, baseline, p)
    c.restore()
    return out
}

/** Photo picker cap. The system may clamp lower; it will not go higher. */
private const val BATCH_MAX = 20

/**
 * Batch Image Converter.
 *
 * The registry promised "Convert many images at once, any format" and the tool
 * did neither: it used the single-image picker and fell through applyTransform
 * to `else -> src`, so it was a one-file no-op. Format became real in the
 * output-controls change; this makes "many at once" real too.
 *
 * Files are decoded one at a time rather than up front. Twenty photos at
 * 4096px would be well over a gigabyte held together, and this app declares no
 * largeHeap - each one is decoded, written and released before the next.
 *
 * reportSave fires once for the run, not once per file. A batch is one tool
 * completion, so markUsed must count it once; saveToGallery's report flag is
 * there for exactly this.
 */
@Composable
private fun BatchConvertTool(accent: Color) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var fmtKey by remember { mutableStateOf("JPEG") }
    var quality by remember { mutableStateOf(90) }
    var scalePct by remember { mutableStateOf(100) }
    var running by remember { mutableStateOf(false) }
    var doneCount by remember { mutableStateOf(0) }
    var failCount by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    val fmt = compressFormatOf(fmtKey)
    val q = if (fmtKey == "PNG") 100 else quality

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(BATCH_MAX)
    ) { list ->
        if (list.isNotEmpty()) {
            uris = list
            finished = false
            doneCount = 0
            failCount = 0
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(
            Modifier.fillMaxWidth().clip(Shape.field)
                .background(accent.copy(alpha = 0.09f))
                .border(1.5.dp, accent.copy(alpha = 0.22f), Shape.field)
                .clickable(enabled = !running) {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MorphoIcon("image-add", tint = accent, size = 20.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    if (uris.isEmpty()) "Choose images"
                    else (uris.size.toString() + " images selected"),
                    color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Up to " + BATCH_MAX + " at a time",
                    color = InkFaint, fontSize = 12.sp
                )
            }
        }

        if (uris.isNotEmpty()) {
            StepControl("RESIZE %", scalePct, listOf(25, 50, 75, 100), accent) { scalePct = it }
            OutputControls(fmtKey, quality, accent, { fmtKey = it }, { quality = it })

            if (running) {
                ProcessingCard(
                    "Converting " + (doneCount + failCount + 1).coerceAtMost(uris.size) +
                        " of " + uris.size,
                    accent
                )
            } else {
                ToolButton("Convert " + uris.size + " images", accent) {
                    running = true
                    finished = false
                    doneCount = 0
                    failCount = 0
                    scope.launch {
                        val list = uris
                        var ok = 0
                        var bad = 0
                        withContext(Dispatchers.IO) {
                            val stamp = System.currentTimeMillis()
                            list.forEachIndexed { i, u ->
                                val decoded = try {
                                    decodeBitmap(ctx, u)
                                } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
                                val outBmp = when {
                                    decoded == null -> null
                                    scalePct != 100 -> try {
                                        scale(decoded, scalePct / 100f)
                                    } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
                                    else -> decoded
                                }
                                val saved = if (outBmp == null) false else try {
                                    saveToGallery(
                                        ctx, outBmp,
                                        "morpho_batch_" + stamp + "_" + (i + 1),
                                        fmt, q, report = false
                                    )
                                } catch (e: Exception) { false } catch (e: OutOfMemoryError) { false }
                                if (saved) { ok++; doneCount++ } else { bad++; failCount++ }
                            }
                        }
                        reportSave(
                            ctx, ok > 0, "Batch ready",
                            ok.toString() + " images converted and saved.",
                            "", "Couldn't convert those images"
                        )
                        running = false
                        finished = true
                    }
                }
            }
        }

        if (finished) {
            StatGrid(
                listOf(
                    "Converted" to doneCount.toString(),
                    "Failed" to failCount.toString(),
                    "Format" to fmtKey,
                    "Saved to" to "Pictures/Morpho"
                ),
                accent
            )
        }
    }
}

@Composable
private fun MetadataBody(bmp: Bitmap, uri: Uri?, accent: Color) {
    val ctx = LocalContext.current
    var report by remember(uri, bmp) { mutableStateOf("") }
    var working by remember(uri, bmp) { mutableStateOf(true) }

    LaunchedEffect(uri, bmp) {
        working = true
        report = withContext(Dispatchers.IO) {
            try { exifReport(ctx, uri, bmp) }
            catch (e: Exception) { "Could not read this file's metadata." }
        }
        working = false
    }

    Box(
        Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(Shape.card).background(PaperSunk),
        contentAlignment = Alignment.Center
    ) {
        Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
    }
    if (working) ProcessingCard("Reading metadata", accent)
    else ToolResult(report, accent, label = "METADATA")
}

/**
 * Real EXIF, not bitmap fields.
 *
 * This tool is registered as "Inspect EXIF and hidden photo data" but only
 * ever printed the decoded bitmap's width, height and config - none of which
 * is metadata, and all of which the picker already shows. It now reads the
 * file itself.
 *
 * Location is reported deliberately and prominently. Someone opening this tool
 * is usually asking what a photo gives away before they send it, and the
 * honest answer is the whole point.
 */
private fun exifReport(ctx: android.content.Context, uri: Uri?, bmp: Bitmap): String {
    val sb = StringBuilder()
    sb.append("IMAGE\n")
    sb.append(pad("Dimensions")).append(bmp.width).append(" x ").append(bmp.height).append("\n")
    sb.append(pad("Megapixels"))
        .append("%.1f".format(bmp.width.toLong() * bmp.height / 1_000_000f)).append("\n")
    val fileSize = sourceFileSize(ctx, uri)
    if (fileSize > 0) sb.append(pad("File size")).append(bytesHuman(fileSize)).append("\n")

    val exif = if (uri == null) null else try {
        ctx.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
    } catch (e: Exception) { null }

    if (exif == null) {
        sb.append("\nNo metadata could be read from this file.")
        return sb.toString()
    }

    var found = false
    listOf(
        "CAMERA" to listOf(
            "Make" to ExifInterface.TAG_MAKE,
            "Model" to ExifInterface.TAG_MODEL,
            "Software" to ExifInterface.TAG_SOFTWARE
        ),
        "EXPOSURE" to listOf(
            "Aperture" to ExifInterface.TAG_F_NUMBER,
            "Shutter" to ExifInterface.TAG_EXPOSURE_TIME,
            "ISO" to ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            "Focal length" to ExifInterface.TAG_FOCAL_LENGTH,
            "Flash" to ExifInterface.TAG_FLASH,
            "White bal." to ExifInterface.TAG_WHITE_BALANCE
        ),
        "WHEN" to listOf(
            "Taken" to ExifInterface.TAG_DATETIME_ORIGINAL,
            "Digitised" to ExifInterface.TAG_DATETIME_DIGITIZED,
            "Modified" to ExifInterface.TAG_DATETIME
        ),
        "AUTHORING" to listOf(
            "Artist" to ExifInterface.TAG_ARTIST,
            "Copyright" to ExifInterface.TAG_COPYRIGHT,
            "Description" to ExifInterface.TAG_IMAGE_DESCRIPTION
        )
    ).forEach { (title, tags) ->
        val rows = tags.mapNotNull { (label, tag) ->
            val v = exif.getAttribute(tag)
            if (v.isNullOrBlank()) null else label to v
        }
        if (rows.isNotEmpty()) {
            found = true
            sb.append("\n").append(title).append("\n")
            rows.forEach { (l, v) -> sb.append(pad(l)).append(v).append("\n") }
        }
    }

    val orient = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED
    )
    if (orient != ExifInterface.ORIENTATION_UNDEFINED) {
        found = true
        sb.append("\nORIENTATION\n").append(pad("Stored as"))
            .append(orientationLabel(orient)).append("\n")
    }

    val ll = exif.latLong
    if (ll != null && ll.size >= 2) {
        found = true
        sb.append("\nLOCATION\n")
        sb.append(pad("Latitude")).append("%.6f".format(ll[0])).append("\n")
        sb.append(pad("Longitude")).append("%.6f".format(ll[1])).append("\n")
        exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.takeIf { it.isNotBlank() }?.let {
            sb.append(pad("Altitude")).append(it).append("\n")
        }
        sb.append("\nThis photo carries where it was taken. ")
        sb.append("Remove EXIF Data → strips it before you share.")
    }

    if (!found) sb.append("\nThis file carries no camera, date or location metadata.")
    return sb.toString()
}

private fun pad(label: String): String = label.padEnd(14)

private fun orientationLabel(v: Int): String = when (v) {
    ExifInterface.ORIENTATION_NORMAL -> "Upright"
    ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90 clockwise"
    ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180"
    ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 90 anticlockwise"
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Mirrored"
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flipped vertically"
    ExifInterface.ORIENTATION_TRANSPOSE -> "Mirrored and rotated 90"
    ExifInterface.ORIENTATION_TRANSVERSE -> "Mirrored and rotated 270"
    else -> "Unspecified"
}

@Composable
private fun StepControl(label: String, value: Int, opts: List<Int>, accent: Color, onChange: (Int) -> Unit) {
    Column {
        FieldLabel("$label: $value")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            opts.forEach { n ->
                Box(Modifier.weight(1f)) {
                    ToolButton("$n", if (value==n) accent else accent.copy(alpha=0.35f)) { onChange(n) }
                }
            }
        }
    }
}

@Composable
private fun OutlineButton(
    text: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().clip(Shape.field)
            .background(accent.copy(alpha = if (enabled) 0.10f else 0.04f))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = accent, fontSize = 15.sp) }
}

// ---- bitmap ops ----
/** Pure transform, safe to call off the main thread. */
private fun applyTransform(
    id: String,
    src: Bitmap,
    scalePct: Int,
    rotation: Int,
    strength: Int,
    sharpRadius: Int
): Bitmap = when (id) {
    "image-resizer", "thumbnail-creator" -> scale(src, scalePct / 100f)
    "image-rotator" -> rotate(src, rotation.toFloat())
    "image-blur" -> boxBlur(src, (strength / 100f * 12).toInt().coerceAtLeast(1))
    "sharpen-image" -> sharpen(src, strength / 100f, sharpRadius)
    else -> src // compressor, exif-remover, batch-convert: pixels unchanged, output re-encoded
}

private fun sizeLabel(pending: Boolean, bytes: Long): String =
    if (pending || bytes <= 0L) "…" else bytesHuman(bytes)

private fun scale(b: Bitmap, f: Float): Bitmap {
    val w = (b.width * f).roundToInt().coerceAtLeast(1)
    val h = (b.height * f).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(b, w, h, true)
}
private fun rotate(b: Bitmap, deg: Float): Bitmap {
    if (deg == 0f) return b
    val m = Matrix().apply { postRotate(deg) }
    return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
}
private fun boxBlur(b: Bitmap, radius: Int): Bitmap {
    if (radius < 1) return b
    // cheap blur: downscale then upscale (fast, no RenderScript)
    val small = Bitmap.createScaledBitmap(b, (b.width / (radius+1)).coerceAtLeast(1),
        (b.height / (radius+1)).coerceAtLeast(1), true)
    return Bitmap.createScaledBitmap(small, b.width, b.height, true)
}

/**
 * Sharpen, as a real unsharp mask: out = src + amount * (src - blur).
 *
 * The old version was a no-op. It drew the blur with PorterDuff.Mode.DST,
 * which keeps the destination and discards the source, so the blur never
 * landed; then it drew the original over the identical original. Output
 * equalled input at every strength.
 *
 * Radius is a control because it decides what gets sharpened. A 1-2px radius
 * lifts fine detail - text on a scanned document, the edge of a product - and
 * a larger one works on local contrast, which is what a landscape wants. One
 * fixed radius serves one of those and fails the others.
 */
private fun sharpen(b: Bitmap, amt: Float, radius: Int): Bitmap {
    if (amt <= 0f || radius < 1) return b
    val w = b.width
    val h = b.height
    if (w < 4 || h < 4) return b
    return try {
        val px = IntArray(w * h)
        b.getPixels(px, 0, w, 0, 0, w, h)
        val blur = boxBlurPixels(px, w, h, radius)
        val k = amt * 1.2f
        for (i in px.indices) {
            val s = px[i]
            val g = blur[i]
            val sr = (s shr 16) and 0xFF
            val sg = (s shr 8) and 0xFF
            val sb = s and 0xFF
            val nr = (sr + k * (sr - ((g shr 16) and 0xFF))).toInt().coerceIn(0, 255)
            val ng = (sg + k * (sg - ((g shr 8) and 0xFF))).toInt().coerceIn(0, 255)
            val nb = (sb + k * (sb - (g and 0xFF))).toInt().coerceIn(0, 255)
            // in place: px[i] is not read again after this
            px[i] = (s and -0x1000000) or (nr shl 16) or (ng shl 8) or nb
        }
        Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    } catch (e: OutOfMemoryError) {
        b
    } catch (e: Exception) {
        b
    }
}

/**
 * Separable box blur over a pixel array, sliding window, so cost is the same
 * at any radius.
 *
 * The vertical pass runs in place through a single column buffer instead of a
 * second full array. That is deliberate: the naive three-array version costs
 * about 150 MB on a 12 megapixel photo, and this app declares no largeHeap.
 */
private fun boxBlurPixels(src: IntArray, w: Int, h: Int, r: Int): IntArray {
    val dst = IntArray(src.size)
    val div = 2 * r + 1

    for (y in 0 until h) {
        val row = y * w
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        for (i in -r..r) {
            val p = src[row + i.coerceIn(0, w - 1)]
            sa += (p ushr 24) and 0xFF; sr += (p shr 16) and 0xFF
            sg += (p shr 8) and 0xFF; sb += p and 0xFF
        }
        for (x in 0 until w) {
            dst[row + x] = (((sa / div) shl 24) or ((sr / div) shl 16) or
                ((sg / div) shl 8) or (sb / div))
            val add = src[row + (x + r + 1).coerceAtMost(w - 1)]
            val sub = src[row + (x - r).coerceAtLeast(0)]
            sa += ((add ushr 24) and 0xFF) - ((sub ushr 24) and 0xFF)
            sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
            sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
            sb += (add and 0xFF) - (sub and 0xFF)
        }
    }

    val col = IntArray(h)
    for (x in 0 until w) {
        for (y in 0 until h) col[y] = dst[y * w + x]
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        for (i in -r..r) {
            val p = col[i.coerceIn(0, h - 1)]
            sa += (p ushr 24) and 0xFF; sr += (p shr 16) and 0xFF
            sg += (p shr 8) and 0xFF; sb += p and 0xFF
        }
        for (y in 0 until h) {
            dst[y * w + x] = (((sa / div) shl 24) or ((sr / div) shl 16) or
                ((sg / div) shl 8) or (sb / div))
            val add = col[(y + r + 1).coerceAtMost(h - 1)]
            val sub = col[(y - r).coerceAtLeast(0)]
            sa += ((add ushr 24) and 0xFF) - ((sub ushr 24) and 0xFF)
            sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
            sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
            sb += (add and 0xFF) - (sub and 0xFF)
        }
    }
    return dst
}
