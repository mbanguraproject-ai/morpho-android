package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

fun hasOcrTool(id: String): Boolean = id in setOf("screenshot-to-text", "image-to-text")

@Composable
fun OcrTool(id: String, accent: Color) {
    val ctx = LocalContext.current
    var src by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u != null) {
            src = decodeBitmap(ctx, u, 2400); result = ""; busy = true
            val bmp = src
            if (bmp != null) {
                val image = InputImage.fromBitmap(bmp, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result = it.text.ifBlank { "No text found in image." }; busy = false }
                    .addOnFailureListener { result = "⚠ Recognition failed: ${it.message}"; busy = false }
            } else busy = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.08f))
            .clickable {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }.padding(Space.lg), verticalAlignment = Alignment.CenterVertically) {
            MorphoIcon("image-add", tint = accent, size = 26.dp)
            Spacer(Modifier.width(Space.md))
            Text(if (src == null) "Choose an image" else "Choose a different image",
                color = accent, fontSize = 15.sp)
        }
        src?.let { bmp ->
            Box(Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(Shape.card).background(PaperSunk),
                contentAlignment = Alignment.Center) {
                Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
            }
        }
        if (busy) Text("Reading text…", color = InkSoft, fontSize = 14.sp)
        if (result.isNotEmpty()) ToolResult(result, accent, mono = false, label = "EXTRACTED TEXT")
    }
}
