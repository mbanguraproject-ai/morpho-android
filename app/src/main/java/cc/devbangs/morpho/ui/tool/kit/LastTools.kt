package cc.devbangs.morpho.ui.tool.kit

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import java.io.File

fun hasLastTool(id: String): Boolean = id in setOf(
    "voice-recorder","audio-joiner","pdf-header-footer","pdf-bates-numbering", "speech-to-text")

@Composable
fun LastTool(id: String, accent: Color) {
    when (id) {
        "voice-recorder" -> VoiceRecorder(accent)
        "speech-to-text" -> SpeechToText(accent)
        "audio-joiner" -> AudioJoiner(accent)
        "pdf-header-footer" -> PdfStamp(id, accent)
        "pdf-bates-numbering" -> PdfStamp(id, accent)
    }
}

// ---- Voice recorder ----
@Composable
private fun VoiceRecorder(accent: Color) {
    val ctx = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var lastFile by remember { mutableStateOf<File?>(null) }
    var hasPerm by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    ) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { hasPerm = it }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        if (!hasPerm) {
            ToolButton("Grant microphone access", accent) {
                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            Box(Modifier.fillMaxWidth().clip(Shape.card)
                .background(if (recording) accent else accent.copy(alpha = 0.08f))
                .clickable {
                    if (!recording) {
                        val f = File(ctx.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
                        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else MediaRecorder()
                        r.setAudioSource(MediaRecorder.AudioSource.MIC)
                        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        r.setOutputFile(f.absolutePath)
                        r.prepare(); r.start()
                        recorder = r; lastFile = f; recording = true
                    } else {
                        try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
                        recorder = null; recording = false
                    }
                }.padding(Space.xl), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MorphoIcon("cat-audio", tint = if (recording) Paper else accent, size = 36.dp)
                    Spacer(Modifier.height(Space.sm))
                    Text(if (recording) "Recording… tap to stop" else "Tap to record",
                        color = if (recording) Paper else accent, fontSize = 15.sp)
                }
            }
            lastFile?.let { f ->
                if (!recording) ToolButton("Save recording", accent) {
                    saveMediaToGallery(ctx, f, "voice_${System.currentTimeMillis()}.m4a", false)
                }
            }
        }
    }
}

// ---- Audio joiner ----
@Composable
private fun AudioJoiner(accent: Color) {
    val ctx = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()) { uris = it }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose audio files", "cat-audio", accent) {
            picker.launch(arrayOf("audio/*"))
        }
        if (uris.isNotEmpty()) {
            Text("${uris.size} file(s) — joined in order", color = InkSoft, fontSize = 13.sp)
            ToolButton("Join & save", accent) {
                val out = joinAudio(ctx, uris)
                if (out != null) saveMediaToGallery(ctx, out, "joined_${System.currentTimeMillis()}.m4a", false)
            }
        }
    }
}

// ---- PDF header/footer + bates ----
@Composable
private fun PdfStamp(id: String, accent: Color) {
    val ctx = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var header by remember { mutableStateOf("") }
    var footer by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("MORPHO") }
    var start by remember { mutableStateOf("1") }
    var loadError by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            pages = renderPdf(ctx, u, 900)
            loadError = if (pages.isEmpty()) pdfFailureReason(ctx, u) else ""
        }
    }
    val bates = id == "pdf-bates-numbering"
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose a PDF", "file-add", accent) { picker.launch(arrayOf("application/pdf")) }
        if (loadError.isNotEmpty()) ToolErrorCard(
            "Couldn't open this PDF", loadError, accent,
            "Choose another", { loadError = ""; picker.launch(arrayOf("application/pdf")) }
        )
        if (pages.isNotEmpty()) {
            if (bates) {
                Column { FieldLabel("PREFIX"); ToolInput(prefix, { prefix = it }, "MORPHO", minLines = 1) }
                Column { FieldLabel("START NUMBER"); ToolInput(start, { start = it }, "1", minLines = 1, mono = true) }
            } else {
                Column { FieldLabel("HEADER"); ToolInput(header, { header = it }, "Confidential", minLines = 1) }
                Column { FieldLabel("FOOTER"); ToolInput(footer, { footer = it }, "© 2026 …", minLines = 1) }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(pages.take(6)) { pg ->
                    Image(pg.asImageBitmap(), null, Modifier.height(130.dp).clip(Shape.tile).background(PaperSunk),
                        contentScale = ContentScale.Fit)
                }
            }
            val startN = start.toIntOrNull() ?: 1
            val stamped = pages.mapIndexed { i, p ->
                if (bates) stampBates(p, prefix, startN + i) else stampHeaderFooter(p, header, footer)
            }
            ActionRow(accent,
                { pagesToPdf2(stamped)?.let { savePdfToDownloads(ctx, it, "stamped_${System.currentTimeMillis()}") } },
                { pagesToPdf2(stamped)?.let { sharePdf(ctx, it, "stamped_${System.currentTimeMillis()}") } })
        }
    }
}

// ---- helpers ----
private fun stampHeaderFooter(src: Bitmap, header: String, footer: String): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true); val c = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt(); textSize = out.width/40f }
    if (header.isNotBlank()) c.drawText(header, out.width*0.06f, out.height*0.04f, paint)
    if (footer.isNotBlank()) c.drawText(footer, out.width*0.06f, out.height*0.97f, paint)
    return out
}
private fun stampBates(src: Bitmap, prefix: String, n: Int): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true); val c = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt(); textSize = out.width/42f
        textAlign = Paint.Align.RIGHT }
    val label = "$prefix-${"%06d".format(n)}"
    c.drawText(label, out.width*0.95f, out.height*0.97f, paint)
    return out
}
private fun pagesToPdf2(pages: List<Bitmap>): ByteArray? {
    if (pages.isEmpty()) return null
    val doc = android.graphics.pdf.PdfDocument()
    pages.forEach { bmp ->
        val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, doc.pages.size + 1).create()
        val page = doc.startPage(info); page.canvas.drawBitmap(bmp, 0f, 0f, null); doc.finishPage(page)
    }
    val s = java.io.ByteArrayOutputStream(); doc.writeTo(s); doc.close(); return s.toByteArray()
}
private fun joinAudio(ctx: android.content.Context, uris: List<Uri>): File? {
    if (uris.isEmpty()) return null
    // concat by remuxing sequentially into one MPEG-4 container (same-codec files)
    return try {
        val out = File(ctx.cacheDir, "join_${System.currentTimeMillis()}.m4a")
        val muxer = android.media.MediaMuxer(out.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var dstTrack = -1; var started = false; var timeOffset = 0L
        val buffer = java.nio.ByteBuffer.allocate(1 shl 20)
        val info = android.media.MediaCodec.BufferInfo()
        uris.forEach { uri ->
            val ex = android.media.MediaExtractor(); ex.setDataSource(ctx, uri, null)
            var audioTrack = -1
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if (f.getString(android.media.MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrack = i
                    if (!started) { dstTrack = muxer.addTrack(f); muxer.start(); started = true }
                }
            }
            if (audioTrack < 0) { ex.release(); return@forEach }
            ex.selectTrack(audioTrack)
            var lastTs = 0L
            while (true) {
                info.offset = 0; info.size = ex.readSampleData(buffer, 0)
                if (info.size < 0) break
                info.presentationTimeUs = timeOffset + ex.sampleTime
                lastTs = info.presentationTimeUs
                info.flags = ex.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, info)
                ex.advance()
            }
            timeOffset = lastTs + 20000
            ex.release()
        }
        muxer.stop(); muxer.release(); out
    } catch (e: Exception) { null }
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
@Composable
private fun ActionRow(accent: Color, onSave: () -> Unit, onShare: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Box(Modifier.weight(1f)) { ToolButton("Save PDF", accent) { onSave() } }
        Box(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
                .clickable { onShare() }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                Text("Share", color = accent, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SpeechToText(accent: Color) {
    val ctx = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var hasPerm by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    ) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { hasPerm = it }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(ctx)) SpeechRecognizer.createSpeechRecognizer(ctx) else null
    }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    fun start() {
        val r = recognizer ?: run { msg = "\u26a0 Speech recognition isn't available on this device."; return }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { msg = "Listening\u2026" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(err: Int) { listening = false; msg = "Tap to try again." }
            override fun onResults(b: Bundle?) {
                val res = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!res.isNullOrEmpty()) text = (text + " " + res[0]).trim()
                listening = false; msg = ""
            }
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(type: Int, params: Bundle?) {}
        })
        r.startListening(intent); listening = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        if (!hasPerm) {
            ToolButton("Grant microphone access", accent) { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        } else {
            ToolButton(if (listening) "Listening\u2026" else "Start speaking", accent, enabled = !listening) { start() }
            Text("Converts your speech to text. May use internet on some devices.", color = InkFaint, fontSize = 12.sp)
        }
        if (text.isNotEmpty()) {
            ToolResult(text, accent, mono = false, label = "TRANSCRIPT")
            Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
                .clickable { text = "" }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text("Clear", color = accent, fontSize = 14.sp)
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
    }
}
