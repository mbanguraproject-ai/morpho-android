package cc.devbangs.morpho.ui.tool.invoice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Color as AColor
import android.net.Uri
import cc.devbangs.morpho.ui.tool.kit.decodeBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

/** Output width of a saved signature, in pixels. */
private const val SIG_OUT_W = 900

/**
 * Draw-with-your-finger signature pad.
 *
 * Strokes are kept as points in the pad's own coordinates and only converted
 * when saved, so the same signature renders at whatever size the document
 * needs rather than being fixed to the size of the phone it was drawn on.
 *
 * Saved as a transparent PNG, because a signature on a white rectangle sitting
 * over a coloured template looks stuck on.
 */
@Composable
fun SignaturePad(
    accent: Color,
    onSave: (List<List<Offset>>, IntSize) -> Unit,
    onCancel: () -> Unit
) {
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    padSize = IntSize(size.width, size.height)
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDragEnd = {
                            if (current.size > 1) strokes = strokes + listOf(current)
                            current = emptyList()
                        },
                        onDragCancel = { current = emptyList() }
                    ) { change, _ ->
                        change.consume()
                        current = current + change.position
                    }
                }
        ) {
            (strokes + listOf(current)).forEach { stroke ->
                for (i in 1 until stroke.size) {
                    drawLine(
                        Color.Black, stroke[i - 1], stroke[i],
                        strokeWidth = 5f, cap = StrokeCap.Round
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton("Clear", accent, filled = false, modifier = Modifier.weight(1f)) {
                strokes = emptyList(); current = emptyList()
            }
            PadButton("Cancel", accent, filled = false, modifier = Modifier.weight(1f), onClick = onCancel)
            PadButton(
                "Use this", accent, filled = true, modifier = Modifier.weight(1f),
                enabled = strokes.isNotEmpty()
            ) { onSave(strokes, padSize) }
        }
    }
}

@Composable
private fun PadButton(
    label: String,
    accent: Color,
    filled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    !enabled -> accent.copy(alpha = 0.18f)
                    filled -> accent
                    else -> accent.copy(alpha = 0.10f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (filled) Color.White else accent,
            fontSize = 13.sp
        )
    }
}

/**
 * Render captured strokes to a transparent PNG under filesDir.
 *
 * Blocking - the caller runs it off the main thread. Returns the path, or an
 * empty string if anything went wrong, so a failed save leaves the invoice
 * without a signature rather than pointing at a file that is not there.
 */
fun writeSignature(ctx: Context, strokes: List<List<Offset>>, padSize: IntSize): String = try {
    if (strokes.isEmpty() || padSize.width <= 0 || padSize.height <= 0) ""
    else {
        val scale = SIG_OUT_W.toFloat() / padSize.width
        val outH = (padSize.height * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(SIG_OUT_W, outH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        val paint = AndroidPaint().apply {
            color = AColor.BLACK
            isAntiAlias = true
            strokeWidth = 5f * scale
            strokeCap = AndroidPaint.Cap.ROUND
            style = AndroidPaint.Style.STROKE
        }
        strokes.forEach { stroke ->
            for (i in 1 until stroke.size) {
                canvas.drawLine(
                    stroke[i - 1].x * scale, stroke[i - 1].y * scale,
                    stroke[i].x * scale, stroke[i].y * scale, paint
                )
            }
        }
        val dir = File(ctx.filesDir, "invoice_assets").apply { mkdirs() }
        val file = File(dir, "sig_" + System.currentTimeMillis() + ".png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    }
} catch (e: Exception) {
    ""
} catch (e: OutOfMemoryError) {
    ""
}

/**
 * Copy a picked logo into the app's own storage, and return its path.
 *
 * Copied rather than kept as a gallery Uri on purpose: a Uri permission can be
 * revoked and the original can be deleted, and an invoice from last year
 * losing its letterhead because the photo was tidied up is not acceptable.
 *
 * Decoded through the shared image intake, so it arrives the right way up and
 * capped in size - a 12 megapixel photo of a logo is nobody's intent.
 * Blocking; the caller runs it off the main thread.
 */
fun writeLogo(ctx: Context, uri: Uri): String {
    return try {
        val bmp = decodeBitmap(ctx, uri, 600) ?: return ""
        val dir = File(ctx.filesDir, "invoice_assets").apply { mkdirs() }
        val file = File(dir, "logo_" + System.currentTimeMillis() + ".png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    } catch (e: Exception) {
        ""
    } catch (e: OutOfMemoryError) {
        ""
    }
}
