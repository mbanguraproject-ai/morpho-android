package cc.devbangs.morpho.ui.tool.kit

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.media.MediaMuxer
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

fun hasEncoderTool(id: String): Boolean = id in setOf(
    "wav-converter","gif-maker","video-to-gif","audio-compressor","volume-booster"
)

@Composable
fun EncoderTool(id: String, accent: Color) {
    when (id) {
        "wav-converter" -> WavConverter(accent)
        "audio-compressor" -> AudioCompressor(accent)
        "volume-booster" -> VolumeBooster(accent)
        "gif-maker" -> GifMaker(accent)
        "video-to-gif" -> VideoToGif(accent)
    }
}

// ---- WAV: decode any audio -> PCM -> WAV ----
@Composable
private fun WavConverter(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose audio" else "Audio selected ✓", "cat-audio", accent) {
            picker.launch(arrayOf("audio/*"))
        }
        if (uri != null) {
            Text("Decodes to uncompressed WAV (PCM 16-bit).", color = InkFaint, fontSize = 12.sp)
            ToolButton(if (busy) "Converting…" else "Convert to WAV", accent, enabled = !busy) {
                busy = true
                val wav = decodeToWav(ctx, uri!!)
                if (wav != null) { saveMediaToGallery(ctx, wav, "morpho_${System.currentTimeMillis()}.wav", false) }
                else msg = "⚠ Could not decode this file."
                busy = false
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

// ---- GIF Maker: images -> animated GIF ----
@Composable
private fun GifMaker(accent: Color) {
    val ctx = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var speed by remember { mutableStateOf(300) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)) { uris = it }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uris.isEmpty()) "Choose images" else "${uris.size} images ✓", "image-add", accent) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        if (uris.isNotEmpty()) {
            StepControl("FRAME DELAY (ms)", speed, listOf(150,300,500,800), accent) { speed = it }
            ToolButton(if (busy) "Encoding…" else "Create GIF", accent, enabled = !busy) {
                busy = true
                val bmps = uris.mapNotNull { decodeBitmap(ctx, it, 720) }
                val gif = encodeGif(ctx, bmps, speed)
                if (gif != null) saveGifToGallery(ctx, gif)
                busy = false
            }
        }
    }
}

// ---- Video -> GIF ----
@Composable
private fun VideoToGif(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var fps by remember { mutableStateOf(6) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> uri = u }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose a video" else "Video selected ✓", "cat-video", accent) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
        if (uri != null) {
            Text("Grabs frames from the first ~5s.", color = InkFaint, fontSize = 12.sp)
            StepControl("FRAMES/SEC", fps, listOf(4,6,8,10), accent) { fps = it }
            ToolButton(if (busy) "Encoding…" else "Create GIF", accent, enabled = !busy) {
                busy = true
                val frames = extractFrames(ctx, uri!!, fps, 5)
                val gif = encodeGif(ctx, frames, 1000 / fps)
                if (gif != null) saveGifToGallery(ctx, gif)
                busy = false
            }
        }
    }
}

// ---- encode helpers ----
private fun encodeGif(ctx: Context, frames: List<Bitmap>, delayMs: Int): File? {
    if (frames.isEmpty()) return null
    return try {
        val f = File(ctx.cacheDir, "gif_${System.currentTimeMillis()}.gif")
        val enc = GifEncoder()
        enc.setRepeat(0); enc.setDelay(delayMs)
        FileOutputStream(f).use { os ->
            enc.start(os)
            frames.forEach { enc.addFrame(it) }
            enc.finish()
        }
        f
    } catch (e: Exception) { null }
}

private fun saveGifToGallery(ctx: Context, file: File) {
    try {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "morpho_${System.currentTimeMillis()}.gif")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/gif")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Morpho")
        }
        val uri = ctx.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { o -> file.inputStream().use { it.copyTo(o) } }
            android.widget.Toast.makeText(ctx, "Saved GIF to Pictures/Morpho", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(ctx, "Save failed", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun extractFrames(ctx: Context, uri: Uri, fps: Int, maxSec: Int): List<Bitmap> {
    val out = mutableListOf<Bitmap>()
    try {
        MediaMetadataRetriever().use { r ->
            r.setDataSource(ctx, uri)
            val durMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val span = minOf(durMs, maxSec * 1000L)
            val step = 1000L / fps
            var t = 0L
            while (t < span) {
                val bmp = r.getFrameAtTime(t * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bmp != null) {
                    val scaled = if (bmp.width > 480) Bitmap.createScaledBitmap(bmp, 480, bmp.height * 480 / bmp.width, true) else bmp
                    out.add(scaled)
                }
                t += step
            }
        }
    } catch (e: Exception) {}
    return out
}

private fun decodeToWav(ctx: Context, uri: Uri): File? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(ctx, uri, null)
        var track = -1; var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { track = i; format = f; break }
        }
        if (track < 0 || format == null) return null
        extractor.selectTrack(track)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0); codec.start()
        val pcm = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var sawInputEOS = false; var sawOutputEOS = false
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inIndex = codec.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buf = codec.getInputBuffer(inIndex)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) { codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); sawInputEOS = true }
                    else { codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0); extractor.advance() }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 10000)
            if (outIndex >= 0) {
                val buf = codec.getOutputBuffer(outIndex)!!
                val chunk = ByteArray(info.size); buf.get(chunk); buf.clear()
                pcm.write(chunk); codec.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEOS = true
            }
        }
        codec.stop(); codec.release()
        val pcmBytes = pcm.toByteArray()
        val f = File(ctx.cacheDir, "wav_${System.currentTimeMillis()}.wav")
        FileOutputStream(f).use { writeWavHeader(it, pcmBytes.size, sampleRate, channels); it.write(pcmBytes) }
        f
    } catch (e: Exception) { null } finally { extractor.release() }
}

private fun writeWavHeader(os: FileOutputStream, pcmLen: Int, sampleRate: Int, channels: Int) {
    val byteRate = sampleRate * channels * 2
    val totalLen = pcmLen + 36
    val header = ByteBuffer.allocate(44)
    header.put("RIFF".toByteArray())
    header.putInt(Integer.reverseBytes(totalLen))
    header.put("WAVE".toByteArray()); header.put("fmt ".toByteArray())
    header.putInt(Integer.reverseBytes(16))
    header.putShort(java.lang.Short.reverseBytes(1))
    header.putShort(java.lang.Short.reverseBytes(channels.toShort()))
    header.putInt(Integer.reverseBytes(sampleRate))
    header.putInt(Integer.reverseBytes(byteRate))
    header.putShort(java.lang.Short.reverseBytes((channels * 2).toShort()))
    header.putShort(java.lang.Short.reverseBytes(16))
    header.put("data".toByteArray())
    header.putInt(Integer.reverseBytes(pcmLen))
    os.write(header.array())
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
private fun StepControl(label: String, value: Int, opts: List<Int>, accent: Color, onChange: (Int) -> Unit) {
    Column {
        FieldLabel("$label: $value")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            opts.forEach { n -> Box(Modifier.weight(1f)) {
                ToolButton("$n", if (value==n) accent else accent.copy(alpha=0.35f)) { onChange(n) } } }
        }
    }
}

// ---- PCM decode helper: returns (pcmBytes, sampleRate, channels) ----
private data class PcmAudio(val pcm: ByteArray, val sampleRate: Int, val channels: Int)

private fun decodePcm(ctx: Context, uri: Uri): PcmAudio? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(ctx, uri, null)
        var track = -1; var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { track = i; format = f; break }
        }
        if (track < 0 || format == null) return null
        extractor.selectTrack(track)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0); codec.start()
        val pcm = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inEos = false; var outEos = false
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        while (!outEos) {
            if (!inEos) {
                val ii = codec.dequeueInputBuffer(10000)
                if (ii >= 0) {
                    val buf = codec.getInputBuffer(ii) ?: continue
                    val sz = extractor.readSampleData(buf, 0)
                    if (sz < 0) { codec.queueInputBuffer(ii,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM); inEos = true }
                    else { codec.queueInputBuffer(ii,0,sz,extractor.sampleTime,0); extractor.advance() }
                }
            }
            val oi = codec.dequeueOutputBuffer(info, 10000)
            if (oi >= 0) {
                val buf = codec.getOutputBuffer(oi)
                if (buf != null) { val c = ByteArray(info.size); buf.get(c); buf.clear(); pcm.write(c) }
                codec.releaseOutputBuffer(oi, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outEos = true
            }
        }
        codec.stop(); codec.release()
        PcmAudio(pcm.toByteArray(), sampleRate, channels)
    } catch (e: Exception) { null } finally { extractor.release() }
}

// ---- Encode PCM -> AAC (.m4a) at a target bitrate ----
private fun encodeAac(ctx: Context, pcm: ByteArray, sampleRate: Int, channels: Int, bitrate: Int): File? {
    return try {
        val out = File(ctx.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
        }
        val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        enc.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); enc.start()
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxTrack = -1; var muxStarted = false
        val info = MediaCodec.BufferInfo()
        var pos = 0; var inEos = false; var outEos = false
        val presUsPerByte = 1_000_000.0 / (sampleRate * channels * 2)
        while (!outEos) {
            if (!inEos) {
                val ii = enc.dequeueInputBuffer(10000)
                if (ii >= 0) {
                    val buf = enc.getInputBuffer(ii)!!
                    buf.clear()
                    val remaining = pcm.size - pos
                    if (remaining <= 0) { enc.queueInputBuffer(ii,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM); inEos = true }
                    else {
                        val chunk = minOf(buf.capacity(), remaining)
                        buf.put(pcm, pos, chunk)
                        val presUs = (pos * presUsPerByte).toLong()
                        enc.queueInputBuffer(ii, 0, chunk, presUs, 0); pos += chunk
                    }
                }
            }
            val oi = enc.dequeueOutputBuffer(info, 10000)
            when {
                oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { muxTrack = muxer.addTrack(enc.outputFormat); muxer.start(); muxStarted = true }
                oi >= 0 -> {
                    val buf = enc.getOutputBuffer(oi)!!
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && muxStarted) { buf.position(info.offset); buf.limit(info.offset+info.size); muxer.writeSampleData(muxTrack, buf, info) }
                    enc.releaseOutputBuffer(oi, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outEos = true
                }
            }
        }
        enc.stop(); enc.release(); muxer.stop(); muxer.release()
        out
    } catch (e: Exception) { null }
}

@androidx.compose.runtime.Composable
private fun AudioCompressor(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var bitrate by remember { mutableStateOf(96000) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose audio" else "Audio selected \u2713", "cat-audio", accent) { picker.launch(arrayOf("audio/*")) }
        if (uri != null) {
            FieldLabel("QUALITY")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                listOf(64000 to "64k", 96000 to "96k", 128000 to "128k", 192000 to "192k").forEach { (br, lbl) ->
                    Box(Modifier.weight(1f)) { ToolButton(lbl, if (bitrate==br) accent else accent.copy(alpha=0.35f)) { bitrate = br } }
                }
            }
            Text("Re-encodes to AAC (.m4a) at the chosen bitrate to shrink size.", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Compressing audio...", accent)
            else {
                ToolButton("Compress audio", accent) {
                    busy = true; val u = uri!!; val br = bitrate
                    scope.launch {
                        val result = withContext(Dispatchers.Default) {
                            val pcm = decodePcm(ctx, u) ?: return@withContext null
                            encodeAac(ctx, pcm.pcm, pcm.sampleRate, pcm.channels, br)
                        }
                        if (result != null) saveMediaToGallery(ctx, result, "morpho_${System.currentTimeMillis()}.m4a", false)
                        else msg = "\u26a0 Could not compress this file."
                        busy = false
                    }
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun VolumeBooster(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var gain by remember { mutableStateOf(150) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> uri = u; msg = "" }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (uri == null) "Choose audio" else "Audio selected \u2713", "cat-audio", accent) { picker.launch(arrayOf("audio/*")) }
        if (uri != null) {
            FieldLabel("VOLUME: ${gain}%")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                listOf(125,150,200,300).forEach { g ->
                    Box(Modifier.weight(1f)) { ToolButton("${g}%", if (gain==g) accent else accent.copy(alpha=0.35f)) { gain = g } }
                }
            }
            Text("Amplifies volume with clipping protection. Outputs AAC (.m4a).", color = InkFaint, fontSize = 12.sp)
            if (busy) ProcessingCard("Boosting volume...", accent)
            else {
                ToolButton("Boost volume", accent) {
                    busy = true; val u = uri!!; val g = gain / 100f
                    scope.launch {
                        val result = withContext(Dispatchers.Default) {
                            val a = decodePcm(ctx, u) ?: return@withContext null
                            val pcm = a.pcm
                            val bb = java.nio.ByteBuffer.wrap(pcm).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                            val sb = bb.asShortBuffer()
                            var i = 0
                            while (i < sb.limit()) {
                                val boosted = (sb.get(i) * g).toInt().coerceIn(-32768, 32767)
                                sb.put(i, boosted.toShort()); i++
                            }
                            encodeAac(ctx, pcm, a.sampleRate, a.channels, 160000)
                        }
                        if (result != null) saveMediaToGallery(ctx, result, "morpho_${System.currentTimeMillis()}.m4a", false)
                        else msg = "\u26a0 Could not process this file."
                        busy = false
                    }
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = InkSoft, fontSize = 13.sp)
        }
    }
}
