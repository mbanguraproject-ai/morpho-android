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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) { picked = uri; src = decodeBitmap(ctx, uri) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        // picker
        PickButton(accent, src != null) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        val bmp = src
        if (bmp != null) {
            when (id) {
                "image-metadata-viewer" -> MetadataBody(bmp, picked, accent)
                else -> TransformBody(id, bmp, accent)
            }
        }
    }
}

@Composable
private fun PickButton(accent: Color, hasImage: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.07f))
            .border(1.5.dp, accent.copy(alpha = 0.22f), Shape.card)
            .clickable(onClick = onClick).padding(vertical = 30.dp, horizontal = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(Shape.chip).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("image-add", tint = accent, size = 26.dp) }
        Spacer(Modifier.height(12.dp))
        Text(if (hasImage) "Choose a different image" else "Choose an image",
            color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text("Tap to select", color = InkFaint, fontSize = 12.sp)
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
    var watermark by remember { mutableStateOf("Morpho") }

    val out = remember(id, src, quality, scalePct, rotation, strength, watermark) {
        when (id) {
            "image-resizer","thumbnail-creator" -> scale(src, scalePct / 100f)
            "image-rotator" -> rotate(src, rotation.toFloat())
            "image-blur" -> boxBlur(src, (strength / 100f * 12).toInt().coerceAtLeast(1))
            "sharpen-image" -> sharpen(src, strength / 100f)
            "watermark-image" -> watermark(src, watermark)
            else -> src // compressor, exif-remover, batch-convert: pixels unchanged, output re-encoded
        }
    }
    val isPng = id in setOf("exif-remover")
    val fmt = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
    val q = if (id == "image-compressor") quality else 92
    val outSize = remember(out, q, fmt) { bitmapBytes(out, fmt, q) }
    val srcSize = remember(src) { bitmapBytes(src, Bitmap.CompressFormat.JPEG, 100) }

    // controls
    when (id) {
        "image-compressor" -> StepControl("QUALITY", quality, listOf(40,60,80,95), accent) { quality = it }
        "image-resizer" -> StepControl("SCALE %", scalePct, listOf(25,50,75,100), accent) { scalePct = it }
        "thumbnail-creator" -> StepControl("SIZE %", scalePct, listOf(10,25,40,60), accent) { scalePct = it }
        "image-rotator" -> StepControl("ROTATE°", rotation, listOf(0,90,180,270), accent) { rotation = it }
        "image-blur","sharpen-image" -> StepControl("STRENGTH", strength, listOf(25,50,75,100), accent) { strength = it }
        "watermark-image" -> Column { FieldLabel("WATERMARK TEXT"); ToolInput(watermark, { watermark = it }, "Your text", minLines = 1) }
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
        if (id == "image-compressor") "New size" to bytesHuman(outSize) else "Output" to bytesHuman(outSize),
        "Original" to bytesHuman(srcSize),
        "Saved" to if (srcSize > 0) "${(100 - outSize*100/srcSize).coerceAtLeast(0)}%" else "—"
    ), accent)

    // actions
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Box(Modifier.weight(1f)) { ToolButton("Save", accent) { saveToGallery(ctx, out, "morpho_${System.currentTimeMillis()}", fmt, q) } }
        Box(Modifier.weight(1f)) { OutlineButton("Share", accent) { shareBitmap(ctx, out, "morpho_${System.currentTimeMillis()}", fmt, q) } }
    }
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
