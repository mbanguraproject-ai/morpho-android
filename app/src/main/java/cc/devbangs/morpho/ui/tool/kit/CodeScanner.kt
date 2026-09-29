package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Read QR codes and barcodes out of a picture.
 *
 * Morpho could write codes and not read one, which is half a tool: the person
 * who generates a code is usually the same person who later needs to check
 * what is actually on one.
 */

private class Decoded(val text: String, val format: String)

@Composable
fun ScannerTool(accent: Color) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var results by remember { mutableStateOf<List<Decoded>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var nothingFound by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    fun scan(uri: Uri) {
        scanning = true
        nothingFound = false
        results = emptyList()
        scope.launch {
            // Decoding needs the pixels as an IntArray, which is four bytes a
            // pixel - a 12MP photo would be 48MB. Capped well below that; no
            // code needs that resolution to read.
            val bmp = withContext(Dispatchers.IO) { decodeBitmap(ctx, uri, 1600) }
            preview = bmp
            val found = if (bmp == null) emptyList()
                        else withContext(Dispatchers.Default) { decodeCodes(bmp) }
            results = found
            nothingFound = found.isEmpty()
            scanning = false
        }
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) scan(uri) }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val u = pendingUri
        if (ok && u != null) scan(u)
        pendingUri = null
    }

    fun capture() {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "code_${System.currentTimeMillis()}.jpg")
        val u = androidx.core.content.FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", f
        )
        pendingUri = u
        camera.launch(u)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.weight(1f)) {
                SourceTile("Take a photo", "camera", accent) { capture() }
            }
            Box(Modifier.weight(1f)) {
                SourceTile("From gallery", "image-add", accent) {
                    gallery.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        }

        if (scanning) ProcessingCard("Looking for codes...", accent)

        if (nothingFound && !scanning) ToolErrorCard(
            title = "No code found",
            body = "Morpho couldn't read a code in that picture. Get closer so the code " +
                "fills more of the frame, keep it flat, and avoid glare across it.",
            accent = accent
        )

        if (results.isNotEmpty()) {
            Text(
                if (results.size == 1) "1 code found" else "${results.size} codes found",
                color = InkSoft, fontSize = 13.sp
            )
            results.forEach { r ->
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.clip(Shape.pill).background(accent.copy(alpha = 0.12f))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(r.format, color = accent, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(Space.sm))
                        Text(payloadKind(r.text), color = InkFaint, fontSize = 11.5.sp)
                    }
                    ToolResult(r.text, accent, mono = true, label = "CONTENT")
                }
            }
        }
    }
}

@Composable
private fun SourceTile(label: String, icon: String, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(vertical = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MorphoIcon(icon, tint = accent, size = 24.dp)
        Spacer(Modifier.height(Space.sm))
        Text(label, color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Try hard, then give up honestly.
 *
 * One pass with the default binarizer misses a lot of real-world pictures:
 * phone photos of screens, printed labels under uneven light, codes shot at an
 * angle. Two binarizers across four rotations is still well under a second on
 * a 1600px bitmap, and it turns most of those misses into reads.
 */
private fun decodeCodes(src: Bitmap): List<Decoded> {
    val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.TRY_HARDER to true
    )
    for (rotation in listOf(0, 90, 180, 270)) {
        val bmp = if (rotation == 0) src else rotate(src, rotation.toFloat()) ?: continue
        try {
            val w = bmp.width
            val h = bmp.height
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            val source = RGBLuminanceSource(w, h, pixels)
            for (useHybrid in listOf(true, false)) {
                val binary = BinaryBitmap(
                    if (useHybrid) HybridBinarizer(source) else GlobalHistogramBinarizer(source)
                )
                // Multiple reader first: a picture of a sheet of labels holds
                // more than one code, and returning only the first is wrong.
                val multi = runCatching {
                    GenericMultipleBarcodeReader(MultiFormatReader())
                        .decodeMultiple(binary, hints)
                }.getOrNull()
                if (multi != null && multi.isNotEmpty()) {
                    return multi.map { Decoded(it.text, prettyFormat(it.barcodeFormat.name)) }
                        .distinctBy { it.text }
                }
                val one = runCatching {
                    MultiFormatReader().apply { setHints(hints) }.decodeWithState(binary)
                }.getOrNull()
                if (one != null) {
                    return listOf(Decoded(one.text, prettyFormat(one.barcodeFormat.name)))
                }
            }
        } catch (e: Exception) {
            // Next rotation.
        } catch (e: OutOfMemoryError) {
            return emptyList()
        } finally {
            if (bmp !== src) bmp.recycle()
        }
    }
    return emptyList()
}

private fun rotate(src: Bitmap, degrees: Float): Bitmap? = try {
    val m = android.graphics.Matrix().apply { postRotate(degrees) }
    Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

private fun prettyFormat(name: String): String = when (name) {
    "QR_CODE" -> "QR"
    "EAN_13" -> "EAN-13"
    "EAN_8" -> "EAN-8"
    "UPC_A" -> "UPC-A"
    "UPC_E" -> "UPC-E"
    "CODE_128" -> "Code 128"
    "CODE_39" -> "Code 39"
    "CODE_93" -> "Code 93"
    "DATA_MATRIX" -> "Data Matrix"
    "PDF_417" -> "PDF417"
    "AZTEC" -> "Aztec"
    "ITF" -> "ITF"
    "CODABAR" -> "Codabar"
    else -> name.replace('_', ' ')
}

/** What the payload actually is, so the raw text is not the only clue. */
private fun payloadKind(text: String): String {
    val t = text.trim()
    val lower = t.lowercase()
    return when {
        lower.startsWith("wifi:") -> "Wi-Fi network"
        lower.startsWith("begin:vcard") || lower.startsWith("mecard:") -> "Contact card"
        lower.startsWith("begin:vevent") -> "Calendar event"
        lower.startsWith("mailto:") -> "Email address"
        lower.startsWith("tel:") -> "Phone number"
        lower.startsWith("smsto:") || lower.startsWith("sms:") -> "Text message"
        lower.startsWith("geo:") -> "Location"
        lower.startsWith("bitcoin:") || lower.startsWith("ethereum:") -> "Crypto address"
        lower.startsWith("otpauth:") -> "Two-factor setup"
        lower.startsWith("http://") || lower.startsWith("https://") -> "Web link"
        t.all { it.isDigit() } && t.length in 8..14 -> "Product number"
        else -> "Plain text"
    }
}
