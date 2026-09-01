package cc.devbangs.morpho.ui.master

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Recommender
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolSearch
import cc.devbangs.morpho.ui.components.Eyebrow
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import cc.devbangs.morpho.workflow.WorkflowBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Above this a hand-off would hold too much in memory to be worth it. */
private const val MAX_HANDOFF_BYTES = 64L * 1024 * 1024

/**
 * Blueprint sections 14 and 17 - job first, tool second.
 *
 * One surface that goes from "I have something to do" to "the tool is open
 * with my file in it", without navigating. Two ways in, both live:
 *
 *  - describe it in plain language, routed by [ToolSearch], which already
 *    resolves phrasing like "make pdf smaller" to a single correct tool;
 *  - pick a file, and see only the tools that accept that kind of file, with
 *    the file carried into whichever one is chosen.
 *
 * No intent categories to choose from first: section 16 is explicit that this
 * should remove decisions rather than add a classification step in front of
 * the tools.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterSheet(
    onDismiss: () -> Unit,
    onOpenTool: (String) -> Unit,
    /** Opened from the file viewer: the subject is already decided. */
    initial: cc.devbangs.morpho.data.MorphoFile? = null
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileMime by remember { mutableStateOf("") }
    var fileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var reading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        reading = true
        error = ""
        scope.launch {
            val loaded = readFile(ctx, uri)
            val bytes = loaded?.third
            when {
                loaded == null || bytes == null ->
                    error = "Morpho couldn't read that file."
                bytes.size > MAX_HANDOFF_BYTES ->
                    error = "That file is too large to carry into a tool. " +
                        "Open the tool directly and pick it there."
                else -> {
                    fileName = loaded.first
                    fileMime = loaded.second
                    fileBytes = bytes
                    query = ""
                }
            }
            reading = false
        }
    }

    // Arriving from the viewer with the file already picked.
    LaunchedEffect(initial?.uri) {
        val f = initial ?: return@LaunchedEffect
        reading = true
        val loaded = readFile(ctx, f.uri)
        val bytes = loaded?.third
        if (bytes != null && bytes.size <= MAX_HANDOFF_BYTES) {
            fileName = f.name.ifBlank { loaded.first }
            fileMime = f.mime.ifBlank { loaded.second }
            fileBytes = bytes
        } else {
            error = "That file is too large to carry into a tool."
        }
        reading = false
    }

    val results: List<Pair<ToolSearch.Group, List<Tool>>> =
        remember(query, fileName, fileMime) {
            when {
                fileBytes != null -> ToolSearch.forFile(fileMime, fileName)
                query.isNotBlank() -> ToolSearch.grouped(query)
                else -> emptyList()
            }
        }

    val suggestion = remember(query, fileName) {
        if (query.isBlank() && fileBytes == null) Recommender.forWorkspace(1).firstOrNull()
        else null
    }

    fun open(tool: Tool) {
        // Hand the file over so the tool opens with it already loaded, rather
        // than making the user pick the same file a second time.
        fileBytes?.let { WorkflowBus.handOff(it, fileMime) }
        onOpenTool(tool.id)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Paper,
        scrimColor = Ink.copy(alpha = 0.32f)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter)) {
            Text(
                "What do you need?",
                style = MaterialTheme.typography.headlineSmall, color = Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Describe it, or start from a file.",
                style = MaterialTheme.typography.bodyMedium, color = InkSoft
            )
            Spacer(Modifier.height(Space.lg))

            Row(
                Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk)
                    .padding(horizontal = Space.md, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MorphoIcon("tab-search", tint = Cobalt, size = 19.dp)
                Spacer(Modifier.width(Space.md))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(
                        "e.g. this pdf is too big", color = InkFaint, fontSize = 15.sp
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            // Typing replaces a picked file as the subject.
                            if (it.isNotBlank()) fileBytes = null
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(Cobalt),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(Space.sm))

            if (fileBytes == null) {
                SheetRow(
                    glyph = "file-add",
                    title = if (reading) "Reading file\u2026" else "Start from a file",
                    body = "Pick a file and see what Morpho can do with it",
                    enabled = !reading
                ) { picker.launch(arrayOf("*/*")) }
            } else {
                SheetRow(
                    glyph = "check",
                    title = fileName.ifBlank { "Selected file" },
                    body = "Choose a tool below \u00b7 tap to pick a different file",
                    accentTitle = true
                ) { picker.launch(arrayOf("*/*")) }
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(Space.sm))
                Text(error, color = InkSoft, style = MaterialTheme.typography.bodyMedium)
            }

            suggestion?.let { s ->
                Spacer(Modifier.height(Space.sm))
                SheetRow(
                    glyph = s.tool.iconKey,
                    title = s.tool.name,
                    body = s.reason,
                    accentTitle = true
                ) { open(s.tool) }
            }
        }

        if (results.isNotEmpty()) {
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 420.dp)
                    .padding(horizontal = Space.gutter),
                contentPadding = PaddingValues(bottom = Space.xxl)
            ) {
                results.forEach { (group, tools) ->
                    item(key = "g-" + group.name) {
                        Eyebrow(group.label.uppercase(), gutter = false)
                    }
                    items(tools, key = { it.id }) { t -> ToolRow(t) { open(t) } }
                }
            }
        } else if (query.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(Space.gutter)) {
                Text("No matching tools.", color = InkSoft,
                    style = MaterialTheme.typography.bodyLarge)
                Text("Try a broader phrase, like \"compress\" or \"convert\".",
                    color = InkFaint, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

/** Name, mime and bytes for a picked or handed-over file. */
private suspend fun readFile(
    ctx: android.content.Context,
    uri: Uri
): Triple<String, String, ByteArray?>? = withContext(Dispatchers.IO) {
    runCatching {
        val name = ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) c.getString(i) else null
        } ?: uri.lastPathSegment.orEmpty()
        val mime = ctx.contentResolver.getType(uri).orEmpty()
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        Triple(name, mime, bytes)
    }.getOrNull()
}

@Composable
private fun SheetRow(
    glyph: String,
    title: String,
    body: String,
    enabled: Boolean = true,
    accentTitle: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(Shape.card)
            .background(if (accentTitle) CobaltWash else PaperSunk)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, enabled = enabled, onClick = onClick
            )
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(Shape.chip).background(Cobalt),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(glyph, tint = Paper, size = 20.dp) }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = if (accentTitle) Cobalt else Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ToolRow(t: Tool, onClick: () -> Unit) {
    val accent = t.category.accent
    Row(
        Modifier.fillMaxWidth().clip(Shape.tile)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(vertical = Space.sm, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(Shape.chip).background(accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(t.iconKey, tint = Paper, size = 20.dp) }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(t.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.short, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MorphoIcon("chevron-right", tint = InkFaint, size = 15.dp)
    }
}
