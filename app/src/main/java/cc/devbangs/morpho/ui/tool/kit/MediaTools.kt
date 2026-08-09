package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Typeface
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

fun hasMediaTool(id: String): Boolean = id in setOf(
    "video-trimmer","audio-trimmer","mp4-to-mp3","meme-generator"
)

@Composable
fun MediaTool(id: String, accent: Color) {
    when (id) {
        "meme-generator" -> MemeTool(accent)
        else -> TrimTool(id, accent)
    }
}

// ---- Trim video / audio, and MP4->audio extraction ----
@Composable
private fun TrimTool(id: String, accent: Color) {
    val ctx = LocalContext.current
    val isVideo = id == "video-trimmer" || id == "mp4-to-mp3"
    val extractAudio = id == "mp4-to-mp3"
    var uri by remember { mutableStateOf<Uri?>(null) }
    var durMs by remember { mutableStateOf(0L) }
    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(0L) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u != null) {
            uri = u; durMs = mediaDurationUs(ctx, u) / 1000
            startMs = 0; endMs = durMs
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (isVideo) "Choose a video" else "Choose audio",
            if (isVideo) "cat-video" else "cat-audio", accent) {
            picker.launch(PickVisualMediaRequest(
                if (isVideo) ActivityResultContracts.PickVisualMedia.VideoOnly
                else ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
        if (uri != null && durMs > 0) {
            Text("Duration ${fmtTime(durMs)}", color = InkSoft, fontSize = 13.sp)
            if (!extractAudio) {
                TimeSlider("START", startMs, durMs, accent) { startMs = it.coerceAtMost(endMs - 1000) }
                TimeSlider("END", endMs, durMs, accent) { endMs = it.coerceAtLeast(startMs + 1000) }
                Text("Clip: ${fmtTime(startMs)} → ${fmtTime(endMs)}  (${fmtTime(endMs - startMs)})",
                    color = Ink, fontSize = 14.sp)
            } else {
                Text("Extracts the audio track (AAC/.m4a) — lossless, no re-encode.",
                    color = InkSoft, fontSize = 13.sp)
            }
            ToolButton(if (busy) "Working…" else if (extractAudio) "Extract audio" else "Save clip",
                accent, enabled = !busy) {
                busy = true
                val s = if (extractAudio) 0L else startMs
                val e = if (extractAudio) durMs else endMs
                val file = trimMedia(ctx, uri!!, s, e, "m4a".let { if (isVideo && !extractAudio) "mp4" else "m4a" })
                if (file != null)
                    saveMediaToGallery(ctx, file, "morpho_${System.currentTimeMillis()}.${if (isVideo && !extractAudio) "mp4" else "m4a"}",
                        isVideo && !extractAudio)
                busy = false
            }
        }
    }
}

@Composable
private fun TimeSlider(label: String, valueMs: Long, maxMs: Long, accent: Color, onChange: (Long) -> Unit) {
    Column {
        FieldLabel("$label: ${fmtTime(valueMs)}")
        // simple stepped control (no Slider dependency): −5s / −1s / +1s / +5s
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(-5000L to "−5s", -1000L to "−1s", 1000L to "+1s", 5000L to "+5s").forEach { (d, lbl) ->
                Box(Modifier.weight(1f)) {
                    ToolButton(lbl, accent.copy(alpha = 0.55f)) {
                        onChange((valueMs + d).coerceIn(0, maxMs))
                    }
                }
            }
        }
    }
}

// ---- Meme generator (top/bottom text on an image) ----
@Composable
private fun MemeTool(accent: Color) {
    val ctx = LocalContext.current
    var src by remember { mutableStateOf<Bitmap?>(null) }
    var top by remember { mutableStateOf("") }
    var bottom by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u != null) src = decodeBitmap(ctx, u, 1600)
    }
    val out = remember(src, top, bottom) { src?.let { drawMeme(it, top, bottom) } }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose an image", "image-add", accent) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        if (src != null) {
            Column { FieldLabel("TOP TEXT"); ToolInput(top, { top = it }, "WHEN THE BUILD", minLines = 1) }
            Column { FieldLabel("BOTTOM TEXT"); ToolInput(bottom, { bottom = it }, "PASSES FIRST TRY", minLines = 1) }
            out?.let { bmp ->
                Box(Modifier.fillMaxWidth().heightIn(max = 340.dp).clip(Shape.card).background(PaperSunk),
                    contentAlignment = Alignment.Center) {
                    Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Box(Modifier.weight(1f)) { ToolButton("Save", accent) {
                        saveToGallery(ctx, bmp, "meme_${System.currentTimeMillis()}", Bitmap.CompressFormat.JPEG, 92) } }
                    Box(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
                            .clickable { shareBitmap(ctx, bmp, "meme_${System.currentTimeMillis()}", Bitmap.CompressFormat.JPEG, 92) }
                            .padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                            Text("Share", color = accent, fontSize = 15.sp) }
                    }
                }
            }
        }
    }
}

private fun drawMeme(src: Bitmap, top: String, bottom: String): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val c = Canvas(out)
    val size = out.width / 10f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.WHITE; textSize = size; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.BLACK; textSize = size; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        style = Paint.Style.STROKE; strokeWidth = size / 12f
    }
    fun draw(text: String, y: Float) {
        if (text.isBlank()) return
        val t = text.uppercase()
        c.drawText(t, out.width / 2f, y, stroke)
        c.drawText(t, out.width / 2f, y, fill)
    }
    draw(top, size * 1.2f)
    draw(bottom, out.height - size * 0.5f)
    return out
}

@Composable
private fun PickRow(label: String, icon: String, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.08f))
        .clickable(onClick = onClick).padding(Space.lg), verticalAlignment = Alignment.CenterVertically) {
        MorphoIcon(icon, tint = accent, size = 26.dp)
        Spacer(Modifier.width(Space.md)); Text(label, color = accent, fontSize = 15.sp)
    }
}
