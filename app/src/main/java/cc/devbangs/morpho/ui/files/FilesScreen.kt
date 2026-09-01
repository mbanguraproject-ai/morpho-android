package cc.devbangs.morpho.ui.files

import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.FileKind
import cc.devbangs.morpho.data.FileStore
import cc.devbangs.morpho.data.MorphoFile
import cc.devbangs.morpho.ui.components.Eyebrow
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Everything Morpho has made.
 *
 * Scoped to Morpho's own output rather than the device, because a tools app
 * cannot honestly list the user's whole storage - see [FileStore]. In practice
 * this is the more useful screen anyway: someone opening Files wants the PDF
 * they just compressed, not a file manager.
 */
@Composable
fun FilesScreen(
    contentPadding: PaddingValues,
    onOpenFile: (MorphoFile) -> Unit,
    onUseTool: (MorphoFile) -> Unit
) {
    val ctx = LocalContext.current
    val hazeState = remember { HazeState() }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<FileKind?>(null) }
    var grid by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<MorphoFile?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { FileStore.refresh(ctx) }
    }

    val all = FileStore.files
    val shown = remember(all, query, filter) {
        val q = query.trim().lowercase()
        all.filter { f ->
            (filter == null || f.kind == filter) &&
                (q.isEmpty() || f.name.lowercase().contains(q))
        }
    }
    val groups = remember(shown) { groupByRecency(shown) }

    // Read in Morpho rather than handed to whatever else is installed.
    fun open(f: MorphoFile) = onOpenFile(f)

    Box(Modifier.fillMaxSize().background(Paper)) {
        LazyColumn(
            Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + Space.xxl
            )
        ) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(60.dp))
                Spacer(Modifier.height(Space.md))
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.gutter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.weight(1f).clip(Shape.card).background(PaperSunk)
                            .padding(horizontal = Space.md, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MorphoIcon("tab-search", tint = Cobalt, size = 18.dp)
                        Spacer(Modifier.width(Space.sm))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty())
                                Text("Search files\u2026", color = InkFaint, fontSize = 15.sp)
                            BasicTextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                                cursorBrush = SolidColor(Cobalt),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(Modifier.width(Space.sm))
                    ViewToggle(grid) { grid = it }
                }
            }

            item {
                LazyRow(
                    Modifier.fillMaxWidth().padding(top = Space.md),
                    contentPadding = PaddingValues(horizontal = Space.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    item {
                        KindChip("All files", all.size, filter == null) { filter = null }
                    }
                    items(FileKind.entries) { k ->
                        val n = FileStore.countOf(k)
                        if (n > 0) KindChip(k.label, n, filter == k) {
                            filter = if (filter == k) null else k
                        }
                    }
                }
            }

            if (all.isNotEmpty()) item {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Space.gutter, vertical = Space.md)
                        .clip(Shape.card).background(PaperSunk).padding(Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(38.dp).clip(Shape.chip).background(CobaltWash),
                        contentAlignment = Alignment.Center
                    ) { MorphoIcon("tab-grid", tint = Cobalt, size = 19.dp) }
                    Spacer(Modifier.width(Space.md))
                    Column {
                        Text(
                            "${all.size} files \u00b7 ${humanSize(FileStore.totalBytes)}",
                            style = MaterialTheme.typography.titleMedium, color = Ink
                        )
                        Text(
                            "Everything Morpho has made on this device",
                            style = MaterialTheme.typography.bodyMedium, color = InkSoft
                        )
                    }
                }
            }

            if (shown.isEmpty()) {
                item { EmptyFiles(loaded = FileStore.loaded, filtered = all.isNotEmpty()) }
            } else {
                groups.forEach { (label, list) ->
                    item(key = "h-$label") { Eyebrow(label.uppercase()) }
                    if (grid) {
                        items(list.chunked(3), key = { it.first().uri.toString() }) { row ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .padding(horizontal = Space.gutter, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                row.forEach { f ->
                                    Box(Modifier.weight(1f)) {
                                        GridCell(f, onClick = { open(f) }, onMenu = { menuFor = f })
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    } else {
                        items(list, key = { it.uri.toString() }) { f ->
                            FileRow(f, onClick = { open(f) }, onMenu = { menuFor = f })
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().zIndex(1f)
                .hazeChild(hazeState, style = HazeMaterials.ultraThin(Paper))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = Space.gutter, end = Space.gutter, top = Space.sm, bottom = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Files", style = MaterialTheme.typography.titleLarge, color = Ink,
                    fontWeight = FontWeight.Bold)
                Text(
                    if (FileStore.loading) "Loading\u2026"
                    else "Everything Morpho has made",
                    style = MaterialTheme.typography.bodySmall, color = InkFaint, fontSize = 11.sp
                )
            }
        }

        menuFor?.let { target ->
            FileActionsSheet(
                file = target,
                onDismiss = { menuFor = null },
                onOpen = { menuFor = null; onOpenFile(target) },
                onUseTool = { menuFor = null; onUseTool(target) },
                onChanged = { menuFor = null }
            )
        }
    }
}

@Composable
private fun ViewToggle(grid: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.clip(Shape.card).background(PaperSunk).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ToggleCell("tab-grid", grid, "Grid view") { onChange(true) }
        ToggleCell("list", !grid, "List view") { onChange(false) }
    }
}

@Composable
private fun ToggleCell(glyph: String, on: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(Shape.chip)
            .background(if (on) Paper else PaperSunk)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        MorphoIcon(glyph, tint = if (on) Cobalt else InkFaint, size = 17.dp,
            contentDescription = label)
    }
}

@Composable
private fun KindChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(Shape.card)
            .background(if (selected) Cobalt else PaperSunk)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(label, color = if (selected) Paper else Ink,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("$count", color = if (selected) Paper.copy(alpha = 0.8f) else InkFaint,
            fontSize = 11.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(f: MorphoFile, onClick: () -> Unit, onMenu: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.gutter)
            .clip(Shape.tile)
            // The dots button is the visible affordance; long press is the
            // shortcut, so the same gesture works in both views.
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenu()
                },
                onClick = onClick
            )
            .padding(vertical = Space.sm, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumb(f, 44.dp)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(f.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${humanSize(f.sizeBytes)} \u00b7 ${f.kind.label}",
                style = MaterialTheme.typography.bodyMedium, color = InkSoft, maxLines = 1
            )
        }
        // Section 39 minimum, and far enough from the row's own tap target
        // that opening and acting on a file do not get confused.
        Box(
            Modifier.size(48.dp).clip(Shape.pill)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = androidx.compose.ui.semantics.Role.Button,
                    onClick = onMenu
                ),
            contentAlignment = Alignment.Center
        ) {
            MorphoIcon("dots", tint = InkFaint, size = 18.dp,
                contentDescription = "Actions for " + f.name)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCell(f: MorphoFile, onClick: () -> Unit, onMenu: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.clip(Shape.card).background(PaperSunk)
            // A dots button on an 80dp thumbnail would be cramped and a poor
            // target; long press is what people already do on a tile.
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenu()
                },
                onClick = onClick
            )
            .padding(Space.sm)
    ) {
        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Thumb(f, 80.dp, fill = true)
        }
        Spacer(Modifier.height(6.dp))
        Text(f.name, style = MaterialTheme.typography.bodyMedium, color = Ink,
            maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
    }
}

/**
 * Platform thumbnail where one exists, type icon otherwise.
 * loadThumbnail arrived in Android 10; PDFs have no MediaStore thumbnail at
 * any version, so those always fall back to the icon.
 */
@Composable
private fun Thumb(f: MorphoFile, size: androidx.compose.ui.unit.Dp, fill: Boolean = false) {
    val ctx = LocalContext.current
    var bmp by remember(f.uri) { mutableStateOf<Bitmap?>(null) }
    val thumbable = f.kind == FileKind.IMAGE || f.kind == FileKind.VIDEO

    LaunchedEffect(f.uri) {
        if (thumbable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bmp = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.loadThumbnail(f.uri, Size(256, 256), null)
                }.getOrNull()
            }
        }
    }

    val shot = bmp
    if (shot != null) {
        Image(
            shot.asImageBitmap(), null,
            Modifier.then(if (fill) Modifier.fillMaxSize() else Modifier.size(size))
                .clip(Shape.chip),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier.size(if (fill) 44.dp else size).clip(Shape.chip)
                .background(tintFor(f.kind)),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(glyphFor(f.kind), tint = Paper, size = if (fill) 22.dp else size * 0.45f) }
    }
}

@Composable
private fun EmptyFiles(loaded: Boolean, filtered: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(Space.gutter, Space.xxl, Space.gutter, Space.gutter),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MorphoIcon("file-add", tint = InkFaint, size = 30.dp)
        Spacer(Modifier.height(Space.md))
        Text(
            if (!loaded) "Looking for your files\u2026"
            else if (filtered) "Nothing matches that."
            else "Nothing here yet.",
            color = InkSoft, style = MaterialTheme.typography.bodyLarge
        )
        if (loaded && !filtered) Text(
            "Files you make with Morpho will appear here.",
            color = InkFaint, style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun glyphFor(k: FileKind) = when (k) {
    FileKind.PDF -> "cat-pdf"
    FileKind.IMAGE -> "cat-image"
    FileKind.VIDEO -> "cat-video"
    FileKind.AUDIO -> "cat-audio"
    FileKind.OTHER -> "cat-converter"
}

private fun tintFor(k: FileKind) = when (k) {
    FileKind.PDF -> cc.devbangs.morpho.data.ToolCategory.PDF.accent
    FileKind.IMAGE -> cc.devbangs.morpho.data.ToolCategory.IMAGE.accent
    FileKind.VIDEO -> cc.devbangs.morpho.data.ToolCategory.VIDEO.accent
    FileKind.AUDIO -> cc.devbangs.morpho.data.ToolCategory.AUDIO.accent
    FileKind.OTHER -> cc.devbangs.morpho.data.ToolCategory.CONVERTER.accent
}

private fun humanSize(bytes: Long): String = when {
    bytes <= 0L -> "\u2014"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    else -> String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

/** Today / Yesterday / Last 7 days / Earlier, in that order, empties dropped. */
private fun groupByRecency(files: List<MorphoFile>): List<Pair<String, List<MorphoFile>>> {
    if (files.isEmpty()) return emptyList()
    val midnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis / 1000
    val dayAgo = midnight - 86_400
    val weekAgo = midnight - 6 * 86_400

    val today = files.filter { it.addedAt >= midnight }
    val yesterday = files.filter { it.addedAt in dayAgo until midnight }
    val week = files.filter { it.addedAt in weekAgo until dayAgo }
    val older = files.filter { it.addedAt < weekAgo }

    return listOf(
        "Today" to today,
        "Yesterday" to yesterday,
        "Last 7 days" to week,
        "Earlier" to older
    ).filter { it.second.isNotEmpty() }
}
