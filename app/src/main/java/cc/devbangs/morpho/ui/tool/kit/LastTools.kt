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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

/** Capture settings, so the user picks the trade rather than inheriting one. */
private data class RecQuality(
    val label: String,
    val detail: String,
    val sampleRate: Int,
    val channels: Int,
    val bitRate: Int
)

private val REC_QUALITIES = listOf(
    RecQuality("Voice note", "22 kHz mono - smallest file", 22050, 1, 64_000),
    RecQuality("Standard", "44 kHz mono - speech and lectures", 44100, 1, 128_000),
    RecQuality("High", "44 kHz stereo - music and room sound", 44100, 2, 192_000)
)

@Composable
private fun VoiceRecorder(accent: Color) {
    val ctx = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var lastFile by remember { mutableStateOf<File?>(null) }
    var quality by remember { mutableStateOf(1) }
    var elapsed by remember { mutableStateOf(0L) }
    var level by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf("") }
    val recording = recorder != null
    var hasPerm by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    ) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { hasPerm = it }

    // Clock and level meter, so it is obvious the microphone is actually live.
    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        val began = System.currentTimeMillis()
        while (true) {
            kotlinx.coroutines.delay(100)
            elapsed = System.currentTimeMillis() - began
            level = (runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0) / 12000f)
                .coerceIn(0f, 1f)
        }
    }
    // A recorder left running holds the microphone open for the whole app.
    DisposableEffect(Unit) {
        onDispose {
            recorder?.let { r -> runCatching { r.stop() }; runCatching { r.release() } }
            ToolWork.reset()
        }
    }

    fun begin() {
        error = ""
        val q = REC_QUALITIES[quality]
        val f = File(ctx.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx)
                else @Suppress("DEPRECATION") MediaRecorder()
        // prepare() throws IOException and start() throws IllegalStateException
        // when the microphone is held by a call or another app. Unguarded, that
        // was a process crash rather than a failed recording.
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(q.sampleRate)
            r.setAudioChannels(q.channels)
            r.setAudioEncodingBitRate(q.bitRate)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            lastFile = f
            elapsed = 0L
            ToolWork.start()
        } catch (e: Exception) {
            runCatching { r.release() }
            f.delete()
            error = "Morpho couldn't start the microphone. Another app or a call may be using it."
        }
    }

    fun finish() {
        val r = recorder ?: return
        recorder = null
        ToolWork.done()
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        level = 0f
        if (!ok || (lastFile?.length() ?: 0L) <= 0L) {
            lastFile?.delete(); lastFile = null
            error = "That recording came out empty. Try again and speak for a second or two."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        if (!hasPerm) {
            Text("Morpho records straight to your device - nothing is uploaded.",
                color = InkSoft, fontSize = 14.sp)
            ToolButton("Grant microphone access", accent) {
                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            return@Column
        }

        if (!recording && lastFile == null) {
            Column {
                FieldLabel("QUALITY")
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    REC_QUALITIES.forEachIndexed { i, q ->
                        val on = i == quality
                        Row(
                            Modifier.fillMaxWidth().clip(Shape.field)
                                .background(if (on) accent.copy(alpha = 0.12f) else PaperSunk)
                                .clickable { quality = i }
                                .padding(horizontal = Space.md, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(q.label, color = Ink, fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold)
                                Text(q.detail, color = InkSoft, fontSize = 12.5.sp)
                            }
                            if (on) MorphoIcon("check", tint = accent, size = 17.dp)
                        }
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(Shape.card)
                .background(if (recording) accent.copy(alpha = 0.12f) else PaperSunk)
                .padding(vertical = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Text(fmtTime(elapsed), color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.xl),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(24) { i ->
                    val lit = recording && level * 24f > i
                    Box(
                        Modifier.weight(1f).height(if (lit) 18.dp else 6.dp).clip(Shape.pill)
                            .background(if (lit) accent else accent.copy(alpha = 0.18f))
                    )
                }
            }
            Text(
                when {
                    recording -> "Recording"
                    lastFile != null -> "Recorded"
                    else -> "Ready"
                },
                color = InkSoft, fontSize = 13.sp
            )
        }

        if (error.isNotEmpty()) ToolErrorCard("Couldn't record", error, accent)

        if (recording) {
            ToolButton("Stop recording", accent) { finish() }
        } else if (lastFile == null) {
            ToolButton("Start recording", accent) { begin() }
        } else {
            val f = lastFile!!
            ToolResultCard(
                fileName = f.name,
                sizeBytes = f.length(),
                accent = accent,
                detail = "${fmtTime(elapsed)} · ${REC_QUALITIES[quality].label}",
                onSave = {
                    saveMediaToGallery(ctx, f, "voice_${System.currentTimeMillis()}.m4a", false)
                },
                onShare = { shareCacheFile(ctx, f, "audio/mp4", "Share audio") }
            )
            ToolButton("Record another", accent.copy(alpha = 0.35f)) {
                lastFile = null; elapsed = 0L; error = ""
            }
        }
    }
}

// ---- Audio joiner ----
@Composable
private fun AudioJoiner(accent: Color) {
    val ctx = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var joining by remember { mutableStateOf(false) }
    val joinScope = androidx.compose.runtime.rememberCoroutineScope()
    var joinError by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()) { picked ->
        if (picked.isNotEmpty()) { uris = picked; joinError = "" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow("Choose audio files", "cat-audio", accent) {
            picker.launch(arrayOf("audio/*"))
        }
        if (uris.isNotEmpty()) {
            Text("${uris.size} file(s) — joined in the order below",
                color = InkSoft, fontSize = 13.sp)
            // The order is the whole point of a joiner, so it has to be movable.
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                uris.forEachIndexed { i, u ->
                    Row(
                        Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk)
                            .padding(start = Space.md, end = Space.sm, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${i + 1}", color = accent, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = Space.md))
                        Text(u.lastPathSegment ?: "Audio", color = Ink, fontSize = 13.5.sp,
                            maxLines = 1, modifier = Modifier.weight(1f))
                        if (i > 0) Box(
                            Modifier.size(36.dp).clip(Shape.pill).clickable {
                                uris = uris.toMutableList()
                                    .also { it.add(i - 1, it.removeAt(i)) }
                                joinError = ""
                            },
                            contentAlignment = Alignment.Center
                        ) { MorphoIcon("chevron-up", tint = accent, size = 14.dp) }
                        Box(
                            Modifier.size(36.dp).clip(Shape.pill).clickable {
                                uris = uris.toMutableList().also { it.removeAt(i) }
                                joinError = ""
                            },
                            contentAlignment = Alignment.Center
                        ) { MorphoIcon("close", tint = InkFaint, size = 14.dp) }
                    }
                }
            }
            if (joinError.isNotEmpty()) ToolErrorCard("Couldn't join those files", joinError, accent)
            // Concatenating tracks is not main-thread work, and without this
            // there was no indication anything was happening at all.
            if (joining) ProcessingCard("Joining audio\u2026", accent)
            else ToolButton("Join & save", accent) {
                joining = true
                val srcs = uris.toList()
                joinScope.launch {
                    val out = withContext(Dispatchers.IO) { joinAudio(ctx, srcs) }
                    // A null used to mean the button simply did nothing, which
                    // reads as the app ignoring the tap.
                    if (out != null) {
                        joinError = ""
                        saveMediaToGallery(ctx, out, "joined_${System.currentTimeMillis()}.m4a", false)
                    } else {
                        joinError = "Morpho couldn't decode one of those files on this " +
                            "device. Try removing the last one you added."
                    }
                    joining = false
                }
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
    var stamping by remember { mutableStateOf(false) }
    var stampOut by remember { mutableStateOf<ByteArray?>(null) }
    var stampName by remember { mutableStateOf("") }
    val stampScope = androidx.compose.runtime.rememberCoroutineScope()
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
            // stamped used to be computed here, in composition, so every
            // keystroke in the header field copied and redrew every page of the
            // document on the main thread for a result usually thrown away.
            // It is built once, on demand, off the main thread.
            if (stamping) ProcessingCard("Stamping your pages...", accent)
            else ToolButton(if (bates) "Add Bates numbers" else "Add header and footer", accent) {
                stamping = true
                val startN = start.toIntOrNull() ?: 1
                val src = pages.toList()
                val useBates = bates
                val pfx = prefix; val hdr = header; val ftr = footer
                stampScope.launch {
                    val bytes = withContext(Dispatchers.Default) {
                        val marked = src.mapIndexed { i, p ->
                            if (useBates) stampBates(p, pfx, startN + i)
                            else stampHeaderFooter(p, hdr, ftr)
                        }
                        pagesToPdf2(marked)
                    }
                    if (bytes != null) {
                        stampOut = bytes
                        stampName = "stamped_${System.currentTimeMillis()}"
                    }
                    stamping = false
                }
            }
            stampOut?.let { bytes ->
                ToolResultCard(
                    fileName = "$stampName.pdf",
                    sizeBytes = bytes.size.toLong(),
                    accent = accent,
                    detail = "${pages.size} page(s)",
                    onSave = { savePdfToDownloads(ctx, bytes, stampName) },
                    onShare = { sharePdf(ctx, bytes, stampName) }
                )
            }
        }
    }
}

// ---- helpers ----
/** Copy a cache file into the shared/ dir and hand it to the chooser. */
private fun shareCacheFile(
    ctx: android.content.Context, file: File, mime: String, title: String
) {
    try {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val copy = File(dir, file.name)
        file.inputStream().use { i ->
            java.io.FileOutputStream(copy).use { o -> i.copyTo(o) }
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", copy
        )
        ctx.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                title
            )
        )
    } catch (e: Exception) {
        android.widget.Toast.makeText(ctx, "Share failed", android.widget.Toast.LENGTH_SHORT).show()
    }
}

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
