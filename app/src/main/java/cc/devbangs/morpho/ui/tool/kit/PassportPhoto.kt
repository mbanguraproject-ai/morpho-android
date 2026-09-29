package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Rect
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions

/**
 * ID and passport photos at an exact physical size.
 *
 * Sizes are named by their millimetres, with a note of where that size is
 * usually asked for - not as a claim about any authority's current rules.
 * Requirements change and vary by document, so the tool prints what the user
 * asked for and tells them to check the spec themselves.
 *
 * There is no face detection in the app's dependencies, so the crop is manual:
 * a vertical offset the user sets while watching the preview. That is honest
 * about what it does rather than implying an alignment it cannot perform.
 */

private class PhotoSize(val label: String, val wMm: Double, val hMm: Double, val note: String)

private val PHOTO_SIZES = listOf(
    PhotoSize("35 × 45 mm", 35.0, 45.0, "The most widely requested passport size"),
    PhotoSize("2 × 2 in", 50.8, 50.8, "Square format, 51 × 51 mm"),
    PhotoSize("33 × 48 mm", 33.0, 48.0, "Taller, narrower format"),
    PhotoSize("50 × 70 mm", 50.0, 70.0, "Large format"),
    PhotoSize("25 × 35 mm", 25.0, 35.0, "Small ID and licence format")
)

private class BgChoice(val label: String, val argb: Int?)

private val BG_CHOICES = listOf(
    BgChoice("Original", null),
    BgChoice("White", AColor.WHITE),
    BgChoice("Off-white", AColor.rgb(242, 242, 242)),
    BgChoice("Light blue", AColor.rgb(198, 219, 239)),
    BgChoice("Light grey", AColor.rgb(210, 210, 210))
)

@Composable
fun PassportPhotoTool(accent: Color) {
    val ctx = LocalContext.current
    var src by remember { mutableStateOf<Bitmap?>(null) }
    var cutout by remember { mutableStateOf<Bitmap?>(null) }
    var sizeIdx by remember { mutableStateOf(0) }
    var dpi by remember { mutableStateOf(300) }
    var bgIdx by remember { mutableStateOf(0) }
    var offset by remember { mutableStateOf(0) }      // -40..40 percent of slack
    var sheet by remember { mutableStateOf(false) }
    var segmenting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val size = PHOTO_SIZES[sizeIdx]
    val wPx = mmToPx(size.wMm, dpi)
    val hPx = mmToPx(size.hMm, dpi)
    val bg = BG_CHOICES[bgIdx]

    fun segment(bmp: Bitmap) {
        segmenting = true
        error = ""
        val segmenter = SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
        )
        segmenter.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { r ->
                cutout = r.foregroundBitmap
                if (r.foregroundBitmap == null) {
                    error = "Morpho couldn't separate the subject from the background " +
                        "in this photo. Keep the original background, or retake it " +
                        "against a plainer wall."
                    bgIdx = 0
                }
                segmenting = false
                segmenter.close()
            }
            .addOnFailureListener {
                error = "The background model isn't ready yet. It downloads once " +
                    "through Google Play services - connect to the internet and try " +
                    "again in a moment."
                bgIdx = 0
                segmenting = false
                segmenter.close()
            }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = decodeBitmap(ctx, uri, 2048)
            src = decoded
            cutout = null
            error = if (decoded == null)
                "Morpho couldn't read that image. Try another photo." else ""
            if (decoded != null && BG_CHOICES[bgIdx].argb != null) segment(decoded)
        }
    }
    val pick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val preview = remember(src, cutout, sizeIdx, dpi, bgIdx, offset) {
        val s = src ?: return@remember null
        buildPhoto(s, cutout, bg.argb, wPx, hPx, offset)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        ImagePickPreview(
            bitmap = src, accent = accent,
            onPick = pick,
            onClear = { src = null; cutout = null; error = "" }
        )

        if (src != null) {
            Column {
                FieldLabel("PHOTO SIZE")
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    PHOTO_SIZES.forEachIndexed { i, p ->
                        val on = i == sizeIdx
                        Row(
                            Modifier.fillMaxWidth().clip(Shape.field)
                                .background(if (on) accent.copy(alpha = 0.12f) else PaperSunk)
                                .clickable { sizeIdx = i }
                                .padding(horizontal = Space.md, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.label, color = Ink, fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold)
                                Text(p.note, color = InkSoft, fontSize = 12.sp)
                            }
                            if (on) MorphoIcon("check", tint = accent, size = 16.dp)
                        }
                    }
                }
            }

            Column {
                FieldLabel("PRINT RESOLUTION")
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(300, 600).forEach { d ->
                        Box(Modifier.weight(1f)) {
                            ToolButton("$d dpi",
                                if (d == dpi) accent else accent.copy(alpha = 0.35f)) { dpi = d }
                        }
                    }
                }
            }

            Column {
                FieldLabel("BACKGROUND")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BG_CHOICES.forEachIndexed { i, c ->
                        val on = i == bgIdx
                        Column(
                            Modifier.weight(1f).clip(Shape.field)
                                .background(if (on) accent.copy(alpha = 0.16f) else PaperSunk)
                                .clickable {
                                    bgIdx = i
                                    val s = src
                                    if (c.argb != null && cutout == null && s != null) segment(s)
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(20.dp).clip(Shape.chip)
                                    .background(
                                        if (c.argb == null) InkFaint.copy(alpha = 0.3f)
                                        else Color(c.argb)
                                    )
                                    .border(1.dp, PaperLine, Shape.chip)
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(c.label, color = if (on) Ink else InkSoft, fontSize = 9.5.sp,
                                maxLines = 1)
                        }
                    }
                }
            }

            if (segmenting) ProcessingCard("Separating the subject...", accent)

            Column {
                FieldLabel("VERTICAL POSITION")
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(-20 to "Up", -5 to "Slight up", 0 to "Centre",
                           5 to "Slight down", 20 to "Down").forEach { (v, lbl) ->
                        Box(Modifier.weight(1f)) {
                            ToolButton(lbl,
                                if (v == offset) accent else accent.copy(alpha = 0.3f)) {
                                offset = v
                            }
                        }
                    }
                }
                Text(
                    "Morpho does not detect faces, so the crop is yours to set - " +
                        "watch the preview and check the head sits where your form asks.",
                    color = InkFaint, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Row(
                Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk)
                    .clickable { sheet = !sheet }
                    .padding(horizontal = Space.md, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(22.dp).clip(Shape.chip)
                        .background(if (sheet) accent else accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { if (sheet) MorphoIcon("check", tint = Paper, size = 13.dp) }
                Spacer(Modifier.width(Space.md))
                Column {
                    Text("Make a 6 × 4 in print sheet", color = Ink, fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("Fills a standard photo print with copies to cut out",
                        color = InkSoft, fontSize = 12.sp)
                }
            }

            if (error.isNotEmpty()) ToolErrorCard("Couldn't do that", error, accent)

            preview?.let { p ->
                val shown = remember(p, sheet, dpi) {
                    if (sheet) sheetOf(p, dpi) else p
                }
                if (shown == null) {
                    ToolErrorCard(
                        "Couldn't build the print sheet",
                        "That combination needs more memory than this device will give. " +
                            "Try 300 dpi instead of 600.",
                        accent
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(Shape.card)
                            .background(PaperSunk).padding(Space.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(shown.asImageBitmap(), null, Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit)
                    }
                    StatGrid(
                        listOf(
                            "Photo" to size.label,
                            "Pixels" to "$wPx×$hPx",
                            "Output" to if (sheet) "${shown.width}×${shown.height}"
                                        else "$wPx×$hPx",
                            "For print" to "$dpi dpi"
                        ),
                        accent
                    )
                    BitmapResultActions(
                        accent,
                        if (sheet) "id_photo_sheet" else "id_photo",
                        Bitmap.CompressFormat.JPEG, 95
                    ) { shown }
                }
            }
        }
    }
}

private fun mmToPx(mm: Double, dpi: Int): Int =
    Math.round(mm / 25.4 * dpi).toInt().coerceAtLeast(1)

/**
 * Composite, crop to the target ratio, and scale to the exact pixel size.
 *
 * [offsetPct] slides the crop window through whatever slack the source has in
 * the cropped direction, so a head too high or too low can be brought in.
 */
private fun buildPhoto(
    src: Bitmap,
    cutout: Bitmap?,
    bgArgb: Int?,
    wPx: Int,
    hPx: Int,
    offsetPct: Int
): Bitmap? = try {
    val base = if (bgArgb != null && cutout != null) {
        val layered = Bitmap.createBitmap(cutout.width, cutout.height, Bitmap.Config.ARGB_8888)
        Canvas(layered).apply {
            drawColor(bgArgb)
            drawBitmap(cutout, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        layered
    } else src

    val targetRatio = wPx.toDouble() / hPx.toDouble()
    val srcRatio = base.width.toDouble() / base.height.toDouble()
    val cropW: Int
    val cropH: Int
    if (srcRatio > targetRatio) {
        cropH = base.height
        cropW = (base.height * targetRatio).toInt().coerceAtLeast(1)
    } else {
        cropW = base.width
        cropH = (base.width / targetRatio).toInt().coerceAtLeast(1)
    }
    val slackX = base.width - cropW
    val slackY = base.height - cropH
    val left = (slackX / 2)
    // Offset only applies where there is room to move.
    val top = (slackY / 2 + slackY * offsetPct / 100).coerceIn(0, slackY)

    val cropped = Bitmap.createBitmap(base, left, top, cropW, cropH)
    val out = Bitmap.createScaledBitmap(cropped, wPx, hPx, true)
    if (cropped !== out) cropped.recycle()
    if (base !== src && base !== out) base.recycle()
    out
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

/** Tile copies onto a 6x4 inch sheet, with a hairline to cut along. */
private fun sheetOf(photo: Bitmap, dpi: Int): Bitmap? = try {
    val sheetW = 6 * dpi
    val sheetH = 4 * dpi
    val gap = Math.round(2.0 / 25.4 * dpi).toInt().coerceAtLeast(2)
    val margin = gap
    val cols = ((sheetW - margin * 2 + gap) / (photo.width + gap)).coerceAtLeast(1)
    val rows = ((sheetH - margin * 2 + gap) / (photo.height + gap)).coerceAtLeast(1)

    val out = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawColor(AColor.WHITE)
    val cut = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.rgb(190, 190, 190)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    val gridW = cols * photo.width + (cols - 1) * gap
    val gridH = rows * photo.height + (rows - 1) * gap
    val originX = ((sheetW - gridW) / 2).coerceAtLeast(0)
    val originY = ((sheetH - gridH) / 2).coerceAtLeast(0)
    for (r in 0 until rows) {
        for (col in 0 until cols) {
            val x = originX + col * (photo.width + gap)
            val y = originY + r * (photo.height + gap)
            c.drawBitmap(photo, null,
                Rect(x, y, x + photo.width, y + photo.height), null)
            c.drawRect(
                x.toFloat(), y.toFloat(),
                (x + photo.width).toFloat(), (y + photo.height).toFloat(), cut
            )
        }
    }
    out
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}
