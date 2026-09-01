package cc.devbangs.morpho.ui.files

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.FileKind
import cc.devbangs.morpho.data.MorphoFile
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Reads files in Morpho instead of handing them to another app.
 *
 * Tapping a file used to fire ACTION_VIEW, which sent the user's own output
 * out to whatever else was installed. Everything [cc.devbangs.morpho.data.FileStore]
 * lists is something Morpho produced - PDF, image, video, audio or text - so
 * those are read here directly. Office formats would need a rendering engine
 * the size of the rest of the app, and Morpho never writes them anyway: it
 * converts them to PDF. Anything unrecognised keeps the hand-off, clearly
 * labelled as leaving Morpho.
 */
@Composable
fun FileViewerScreen(
    file: MorphoFile,
    onBack: () -> Unit,
    onUseTool: (MorphoFile) -> Unit
) {
    val ctx = LocalContext.current
    BackHandler { onBack() }

    fun share() {
        runCatching {
            ctx.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = file.mime.ifBlank { "*/*" }
                        putExtra(Intent.EXTRA_STREAM, file.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share file"
                )
            )
        }
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonMorpho("chevron-left", onBack, contentDescription = "Back")
            Spacer(Modifier.width(Space.xs))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(file.kind.label, style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft, maxLines = 1)
            }
            IconButtonMorpho("share", ::share, contentDescription = "Share file")
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (file.kind) {
                FileKind.PDF -> PdfBody(file)
                FileKind.IMAGE -> ImageBody(file)
                FileKind.VIDEO -> VideoBody(file)
                FileKind.AUDIO -> AudioBody(file)
                FileKind.OTHER -> OtherBody(file)
            }
        }

        // The point of reading it here: act on it without leaving.
        Row(
            Modifier.fillMaxWidth().padding(Space.gutter)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Box(
                Modifier.weight(1f).clip(Shape.card).background(Cobalt)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onUseTool(file) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Use a tool on this", color = Paper, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Pages are rendered as they scroll into view and the renderer stays open for
 * the life of the screen. Rendering the whole document up front, as the tools
 * do, would hold every page in memory at once.
 */
@Composable
private fun PdfBody(file: MorphoFile) {
    val ctx = LocalContext.current
    var pageCount by remember { mutableStateOf(0) }
    var failure by remember { mutableStateOf("") }
    val holder = remember { PdfHolder() }

    DisposableEffect(file.uri) {
        runCatching {
            val pfd = ctx.contentResolver.openFileDescriptor(file.uri, "r")
            if (pfd == null) failure = "Morpho couldn't open that file."
            else {
                holder.pfd = pfd
                holder.renderer = PdfRenderer(pfd)
                pageCount = holder.renderer?.pageCount ?: 0
            }
        }.onFailure {
            failure = if (it is SecurityException)
                "This PDF is password-protected."
            else "This file may be damaged, or it may not really be a PDF."
        }
        onDispose { holder.close() }
    }

    when {
        failure.isNotEmpty() -> CenteredNote(failure)
        pageCount == 0 -> CenteredNote("Opening\u2026")
        else -> LazyColumn(
            Modifier.fillMaxSize().background(PaperSunk),
            contentPadding = PaddingValues(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            items((0 until pageCount).toList()) { index -> PdfPage(holder, index) }
        }
    }
}

private class PdfHolder {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    /** PdfRenderer allows one open page at a time, so rendering is serialised. */
    val lock = Any()
    fun close() {
        runCatching { renderer?.close() }
        runCatching { pfd?.close() }
        renderer = null
        pfd = null
    }
}

@Composable
private fun PdfPage(holder: PdfHolder, index: Int) {
    var bmp by remember(index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(index) {
        bmp = withContext(Dispatchers.Default) {
            runCatching {
                synchronized(holder.lock) {
                    val r = holder.renderer ?: return@synchronized null
                    r.openPage(index).use { page ->
                        val w = 1240
                        val h = (w * page.height / page.width.toFloat()).toInt()
                        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        out.eraseColor(android.graphics.Color.WHITE)
                        page.render(out, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        out
                    }
                }
            }.getOrNull()
        }
    }
    val shot = bmp
    if (shot == null) {
        Box(
            Modifier.fillMaxWidth().height(420.dp).clip(Shape.tile).background(Paper),
            contentAlignment = Alignment.Center
        ) { Text("Page ${index + 1}", color = InkFaint, fontSize = 13.sp) }
    } else {
        Image(
            shot.asImageBitmap(), null,
            Modifier.fillMaxWidth().clip(Shape.tile).background(Paper),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
private fun ImageBody(file: MorphoFile) {
    val ctx = LocalContext.current
    var bmp by remember(file.uri) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(file.uri) { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(file.uri) {
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                ctx.contentResolver.openInputStream(file.uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
            }.getOrNull()
        }
        bmp = decoded
        failed = decoded == null
    }

    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        if (scale > 1f) {
            offsetX += pan.x
            offsetY += pan.y
        } else {
            offsetX = 0f; offsetY = 0f
        }
    }

    val shot = bmp
    when {
        failed -> CenteredNote("Morpho couldn't read that image.")
        shot == null -> CenteredNote("Opening\u2026")
        else -> Box(
            Modifier.fillMaxSize().background(PaperSunk).transformable(transform),
            contentAlignment = Alignment.Center
        ) {
            Image(
                shot.asImageBitmap(), null,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationX = offsetX; translationY = offsetY
                },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun VideoBody(file: MorphoFile) {
    Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { c ->
                VideoView(c).apply {
                    setVideoURI(file.uri)
                    setOnPreparedListener { it.isLooping = false }
                    val controller = android.widget.MediaController(c)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    start()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AudioBody(file: MorphoFile) {
    val ctx = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(file.uri) {
        val mp = runCatching {
            MediaPlayer().apply {
                setDataSource(ctx, file.uri)
                prepare()
            }
        }.getOrNull()
        if (mp == null) failed = true else {
            player = mp
            duration = mp.duration
        }
        onDispose {
            runCatching { mp?.release() }
            player = null
        }
    }

    LaunchedEffect(playing) {
        while (playing) {
            position = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
            if (player?.isPlaying == false) playing = false
            delay(200)
        }
    }

    if (failed) { CenteredNote("Morpho couldn't play that file."); return }

    Column(
        Modifier.fillMaxSize().padding(Space.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(Shape.card)
                .background(cc.devbangs.morpho.data.ToolCategory.AUDIO.accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("cat-audio", tint = Paper, size = 44.dp) }
        Spacer(Modifier.height(Space.xl))
        Text(timeLabel(position) + " / " + timeLabel(duration),
            color = InkSoft, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(Space.md))
        Box(
            Modifier.fillMaxWidth().height(4.dp).clip(Shape.pill).background(PaperSunk)
        ) {
            val fraction = if (duration > 0) position / duration.toFloat() else 0f
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight().clip(Shape.pill).background(Cobalt)
            )
        }
        Spacer(Modifier.height(Space.xl))
        Box(
            Modifier.size(64.dp).clip(Shape.pill).background(Cobalt)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val mp = player ?: return@clickable
                    if (playing) { mp.pause(); playing = false }
                    else { mp.start(); playing = true }
                },
            contentAlignment = Alignment.Center
        ) {
            MorphoIcon(if (playing) "pause" else "play", tint = Paper, size = 26.dp,
                contentDescription = if (playing) "Pause" else "Play")
        }
    }
}

private fun timeLabel(ms: Int): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun OtherBody(file: MorphoFile) {
    val ctx = LocalContext.current
    var text by remember(file.uri) { mutableStateOf<String?>(null) }
    var readable by remember(file.uri) { mutableStateOf(true) }
    val scroll = rememberScrollState()

    LaunchedEffect(file.uri) {
        val looksTextual = file.mime.startsWith("text/") ||
            file.name.substringAfterLast('.', "").lowercase() in
            setOf("txt", "md", "csv", "json", "xml", "html", "log")
        if (!looksTextual) { readable = false; return@LaunchedEffect }
        text = withContext(Dispatchers.IO) {
            runCatching {
                ctx.contentResolver.openInputStream(file.uri)?.use {
                    // Enough to read, not enough to choke on a huge log.
                    String(it.readBytes().copyOf(minOf(it.available(), 2_000_000)))
                }
            }.getOrNull()
        }
        readable = text != null
    }

    val body = text
    when {
        !readable -> Column(
            Modifier.fillMaxSize().padding(Space.gutter),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MorphoIcon("cat-converter", tint = InkFaint, size = 34.dp)
            Spacer(Modifier.height(Space.md))
            Text("Morpho can't preview this type yet.",
                color = InkSoft, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(Space.sm))
            Box(
                Modifier.clip(Shape.card).background(PaperSunk)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(file.uri, file.mime.ifBlank { "*/*" })
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            )
                        }
                    }
                    .padding(horizontal = Space.lg, vertical = 11.dp)
            ) { Text("Open in another app", color = Cobalt, fontSize = 14.sp) }
        }
        body == null -> CenteredNote("Opening\u2026")
        else -> Text(
            body,
            color = Ink, fontSize = 13.sp,
            modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(Space.gutter)
        )
    }
}

@Composable
private fun CenteredNote(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = InkSoft, style = MaterialTheme.typography.bodyLarge)
    }
}
