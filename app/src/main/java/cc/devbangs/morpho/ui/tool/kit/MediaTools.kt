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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import android.media.MediaPlayer
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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

fun hasMediaTool(id: String): Boolean = id in setOf(
    "video-trimmer","audio-trimmer","mp4-to-mp3","meme-generator","silence-video"
)

@Composable
fun MediaTool(id: String, accent: Color) {
    when (id) {
        "meme-generator" -> MemeTool(accent)
        "silence-video" -> SilenceVideo(accent)
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
    val scope = rememberCoroutineScope()

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
                ClipPreview(uri!!, startMs, endMs, accent)
            } else {
                Text("Extracts the audio track (AAC/.m4a) — lossless, no re-encode.",
                    color = InkSoft, fontSize = 13.sp)
            }
            if (busy) {
                ProcessingCard(if (extractAudio) "Extracting audio..." else "Processing your clip...", accent)
            } else {
                ToolButton(if (extractAudio) "Extract audio" else "Save clip", accent) {
                    busy = true
                    val ms0 = if (extractAudio) 0L else startMs
                    val ms1 = if (extractAudio) durMs else endMs
                    val u = uri!!
                    val ext = if (isVideo && !extractAudio) "mp4" else "m4a"
                    scope.launch {
                        val file = withContext(Dispatchers.Default) { trimMedia(ctx, u, ms0, ms1, ext) }
                        if (file != null)
                            saveMediaToGallery(ctx, file, "morpho_${System.currentTimeMillis()}.$ext", isVideo && !extractAudio)
                        busy = false
                    }
                }
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
    Column(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.07f))
            .border(1.5.dp, accent.copy(alpha = 0.22f), Shape.card)
            .clickable(onClick = onClick)
            .padding(vertical = 30.dp, horizontal = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(Shape.chip).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(icon, tint = accent, size = 26.dp) }
        Spacer(Modifier.height(12.dp))
        Text(label, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text("Tap to select", color = InkFaint, fontSize = 12.sp)
    }
}

@androidx.compose.runtime.Composable
private fun ClipPreview(uri: Uri, startMs: Long, endMs: Long, accent: Color) {
    val ctx = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }
    var prepared by remember { mutableStateOf(false) }

    // (re)prepare when the uri changes
    LaunchedEffect(uri) {
        prepared = false
        try {
            player.reset()
            player.setDataSource(ctx, uri)
            player.setOnPreparedListener { prepared = true }
            player.prepareAsync()
        } catch (e: Exception) { prepared = false }
    }

    // stop playback at endMs
    LaunchedEffect(playing) {
        if (playing && prepared) {
            try {
                player.seekTo(startMs.toInt())
                player.start()
                while (isActive && player.isPlaying) {
                    if (player.currentPosition >= endMs) { player.pause(); playing = false; break }
                    delay(50)
                }
            } catch (e: Exception) { playing = false }
        } else if (!playing) {
            try { if (player.isPlaying) player.pause() } catch (e: Exception) {}
        }
    }

    // release on leave
    DisposableEffect(Unit) {
        onDispose { try { player.release() } catch (e: Exception) {} }
    }

    Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
        .clickable(enabled = prepared) { playing = !playing }
        .padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MorphoIcon(if (playing) "pause" else "play", tint = accent, size = 20.dp)
            Text(
                if (!prepared) "Loading preview\u2026" else if (playing) "Stop preview" else "Preview clip",
                color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun SilenceVideo(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u != null) { uri = u; msg = "" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a video" else "Video selected \u2713", "cat-video", accent) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
        if (uri != null) {
            Text("Removes the audio track \u2014 keeps video quality (no re-encode).", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Muting your video...", accent)
            else ToolButton("Mute video", accent) {
                busy = true; val u = uri!!
                scope.launch {
                    val file = withContext(Dispatchers.Default) { muteVideo(ctx, u) }
                    if (file != null) saveMediaToGallery(ctx, file, "morpho_${System.currentTimeMillis()}.mp4", true)
                    else msg = "\u26a0 Could not process this video."
                    busy = false
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}
