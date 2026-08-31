package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions

/**
 * Background Remover, powered by ML Kit Subject Segmentation.
 *
 * This tool was in the registry and marked popular but had no dispatch case,
 * so it fell through to the placeholder card.
 *
 * rembg, the usual reference implementation, is Python running U2-Net under
 * onnxruntime and cannot run in an Android app. ML Kit's subject segmentation
 * does the same job on-device: it returns a foreground bitmap with the
 * background already removed. No upload, no server, no per-use cost.
 *
 * The model ships via Google Play services rather than the APK, so the first
 * run may need a connection while it downloads. Every run after is offline.
 */
@Composable
fun BackgroundRemoverTool(accent: Color) {
    val ctx = LocalContext.current
    var src by remember { mutableStateOf<Bitmap?>(null) }
    var out by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var lowRes by remember { mutableStateOf(false) }

    fun segment(bmp: Bitmap) {
        busy = true
        out = null
        error = ""
        // Only the foreground bitmap is enabled: the docs recommend enabling
        // the minimum for memory, and the masks are not needed here.
        val segmenter = SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
        )
        segmenter.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { result ->
                val foreground = result.foregroundBitmap
                if (foreground == null) {
                    error = "Morpho couldn't find a clear subject in this photo. " +
                        "Try one where the subject stands out from the background."
                } else {
                    out = foreground
                }
                busy = false
                segmenter.close()
            }
            .addOnFailureListener {
                error = "Background removal isn't ready yet. The on-device model " +
                    "downloads once through Google Play services - connect to the " +
                    "internet and try again in a moment."
                busy = false
                segmenter.close()
            }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = decodeBitmap(ctx, uri, 2048)
            src = decoded
            out = null
            if (decoded == null) {
                error = "Morpho couldn't read that image. It may be in an " +
                    "unsupported format, or the file may be damaged."
                lowRes = false
            } else {
                // ML Kit documents 512x512 as the floor for accurate results.
                lowRes = minOf(decoded.width, decoded.height) < 512
                segment(decoded)
            }
        }
    }
    val pick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Accept an image handed over by a previous tool.
    LaunchedEffect(Unit) {
        cc.devbangs.morpho.workflow.WorkflowBus.consume()?.let { handed ->
            val decoded = decodeBitmapBytes(handed.bytes, 2048)
            src = decoded
            if (decoded == null) {
                error = "Morpho couldn't read the image it was handed."
            } else {
                lowRes = minOf(decoded.width, decoded.height) < 512
                segment(decoded)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        ImagePickPreview(
            bitmap = src,
            accent = accent,
            onPick = { error = ""; pick() },
            onClear = { src = null; out = null; error = ""; lowRes = false }
        )

        if (busy) ProcessingCard("Removing the background\u2026", accent)

        if (error.isNotEmpty()) ToolErrorCard(
            title = "Couldn't remove the background",
            body = error,
            accent = accent,
            actionLabel = "Choose another image",
            onAction = { error = ""; pick() }
        )

        if (lowRes && out != null) Text(
            "This image is under 512px on its shortest side, so the cut-out may be rough.",
            color = InkFaint, fontSize = 12.sp
        )

        out?.let { result ->
            FieldLabel("RESULT \u00b7 TRANSPARENT PNG")
            Image(
                result.asImageBitmap(), null,
                Modifier.fillMaxWidth().height(240.dp)
                    .clip(Shape.tile).background(PaperSunk),
                contentScale = ContentScale.Fit
            )
            StatGrid(
                listOf(
                    "Size" to "${result.width}\u00d7${result.height}",
                    "Format" to "PNG",
                    "Background" to "Removed",
                    "Processed" to "On device"
                ),
                accent
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                // PNG at quality 100: JPEG would flatten the alpha to black.
                Box(Modifier.weight(1f)) {
                    ToolButton("Save PNG", accent) {
                        saveToGallery(
                            ctx, result, "morpho_nobg_${System.currentTimeMillis()}",
                            Bitmap.CompressFormat.PNG, 100
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    ToolButton("Share", accent) {
                        shareBitmap(
                            ctx, result, "morpho_nobg_${System.currentTimeMillis()}",
                            Bitmap.CompressFormat.PNG, 100
                        )
                    }
                }
            }
        }
    }
}
