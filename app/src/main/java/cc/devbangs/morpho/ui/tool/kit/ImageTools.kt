package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import kotlinx.coroutines.Dispatchers
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
                else -> TransformBody(id, bmp, accent)
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
private fun TransformBody(id: String, src: Bitmap, accent: Color) {
    val ctx = LocalContext.current
    // per-tool parameters
    var quality by remember { mutableStateOf(80) }
    var scalePct by remember { mutableStateOf(100) }
    var rotation by remember { mutableStateOf(0) }
    var strength by remember { mutableStateOf(50) }
    var watermarkText by remember { mutableStateOf("Morpho") }

    val isPng = id in setOf("exif-remover")
    val fmt = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
    val q = if (id == "image-compressor") quality else 92

    // Both the transform and the size readout are expensive on a large photo:
    // the readout is a full re-encode. Running them in composition meant every
    // control tap - and every watermark keystroke - blocked the main thread.
    // LaunchedEffect cancels on each parameter change, so the leading delay
    // debounces held taps and typing instead of queueing work per character.
    var out by remember(src) { mutableStateOf(src) }
    var outSize by remember(src) { mutableStateOf(0L) }
    var srcSize by remember(src) { mutableStateOf(0L) }
    var working by remember(src) { mutableStateOf(true) }

    LaunchedEffect(src) {
        srcSize = withContext(Dispatchers.Default) {
            bitmapBytes(src, Bitmap.CompressFormat.JPEG, 100)
        }
    }
    LaunchedEffect(id, src, quality, scalePct, rotation, strength, watermarkText, fmt, q) {
        working = true
        delay(140)
        val result = withContext(Dispatchers.Default) {
            val bmp = applyTransform(id, src, scalePct, rotation, strength, watermarkText)
            bmp to bitmapBytes(bmp, fmt, q)
        }
        out = result.first
        outSize = result.second
        working = false
    }

    // controls
    when (id) {
        "image-compressor" -> StepControl("QUALITY", quality, listOf(40,60,80,95), accent) { quality = it }
        "image-resizer" -> StepControl("SCALE %", scalePct, listOf(25,50,75,100), accent) { scalePct = it }
        "thumbnail-creator" -> StepControl("SIZE %", scalePct, listOf(10,25,40,60), accent) { scalePct = it }
        "image-rotator" -> StepControl("ROTATE°", rotation, listOf(0,90,180,270), accent) { rotation = it }
        "image-blur","sharpen-image" -> StepControl("STRENGTH", strength, listOf(25,50,75,100), accent) { strength = it }
        "watermark-image" -> Column { FieldLabel("WATERMARK TEXT"); ToolInput(watermarkText, { watermarkText = it }, "Your text", minLines = 1) }
    }

    // preview
    Box(
        Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 320.dp)
            .clip(Shape.card).background(PaperSunk),
        contentAlignment = Alignment.Center
    ) {
        Image(out.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
    }

    // stats
    StatGrid(listOf(
        "Dimensions" to "${out.width}×${out.height}",
        (if (id == "image-compressor") "New size" else "Output") to sizeLabel(working, outSize),
        "Original" to sizeLabel(false, srcSize),
        "Saved" to if (working || srcSize <= 0L || outSize <= 0L) "…"
            else "${(100 - outSize * 100 / srcSize).coerceAtLeast(0)}%"
    ), accent)

    // actions
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Box(Modifier.weight(1f)) { ToolButton("Save", accent) { saveToGallery(ctx, out, "morpho_${System.currentTimeMillis()}", fmt, q) } }
        Box(Modifier.weight(1f)) { OutlineButton("Share", accent) { shareBitmap(ctx, out, "morpho_${System.currentTimeMillis()}", fmt, q) } }
    }
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
    LaunchedEffect(src, cl, ct, cr, cb) {
        measuring = true
        delay(260)
        outSize = withContext(Dispatchers.Default) {
            try {
                bitmapBytes(cropOf(src, cl, ct, cr, cb), Bitmap.CompressFormat.JPEG, 92)
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

        StatGrid(listOf(
            "Crop size" to "${outW}\u00d7${outH}",
            "Output" to if (measuring || outSize <= 0L) "\u2026" else bytesHuman(outSize),
            "Source" to "${src.width}\u00d7${src.height}",
            "Ratio" to ratio
        ), accent)

        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.weight(1f)) {
                ToolButton("Save", accent) {
                    saveToGallery(
                        ctx, cropOf(src, cl, ct, cr, cb),
                        "morpho_crop_${System.currentTimeMillis()}",
                        Bitmap.CompressFormat.JPEG, 92
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                OutlineButton("Share", accent) {
                    shareBitmap(
                        ctx, cropOf(src, cl, ct, cr, cb),
                        "morpho_crop_${System.currentTimeMillis()}",
                        Bitmap.CompressFormat.JPEG, 92
                    )
                }
            }
        }
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

@Composable
private fun MetadataBody(bmp: Bitmap, uri: Uri?, accent: Color) {
    val info = "Width    ${bmp.width}px\nHeight   ${bmp.height}px\nRatio    ${"%.2f".format(bmp.width.toFloat()/bmp.height)}\nConfig   ${bmp.config}\nPixels   ${bmp.width*bmp.height}"
    Box(Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(Shape.card).background(PaperSunk),
        contentAlignment = Alignment.Center) {
        Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
    }
    ToolResult(info, accent, label = "IMAGE INFO")
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
private fun OutlineButton(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(Shape.field)
            .background(accent.copy(alpha = 0.10f)).clickable(onClick = onClick).padding(vertical = 15.dp),
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
    watermarkText: String
): Bitmap = when (id) {
    "image-resizer", "thumbnail-creator" -> scale(src, scalePct / 100f)
    "image-rotator" -> rotate(src, rotation.toFloat())
    "image-blur" -> boxBlur(src, (strength / 100f * 12).toInt().coerceAtLeast(1))
    "sharpen-image" -> sharpen(src, strength / 100f)
    "watermark-image" -> watermark(src, watermarkText)
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
private fun sharpen(b: Bitmap, amt: Float): Bitmap {
    // unsharp-ish: overlay original over its blur at alpha
    val out = b.copy(Bitmap.Config.ARGB_8888, true)
    val blur = boxBlur(b, 2)
    val c = Canvas(out)
    val p = Paint().apply { alpha = (amt * 160).toInt().coerceIn(0,255) }
    c.drawBitmap(blur, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST) })
    c.drawBitmap(b, 0f, 0f, p)
    return out
}
private fun watermark(b: Bitmap, text: String): Bitmap {
    val out = b.copy(Bitmap.Config.ARGB_8888, true)
    val c = Canvas(out)
    val p = Paint().apply {
        color = AColor.WHITE; isAntiAlias = true
        textSize = out.width / 14f
        alpha = 150
        setShadowLayer(4f, 0f, 0f, AColor.BLACK)
    }
    val tw = p.measureText(text)
    c.drawText(text, out.width - tw - out.width*0.04f, out.height - out.height*0.04f, p)
    return out
}
