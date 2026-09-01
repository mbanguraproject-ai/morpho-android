package cc.devbangs.morpho.ui.files

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.FileStore
import cc.devbangs.morpho.data.MorphoFile
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Warn = Color(0xFFB4231E)

/**
 * What you can do with a file without leaving Morpho.
 *
 * Rename and delete go through MediaStore. An app can always modify media it
 * created, which is everything this screen lists - but that ownership is lost
 * if Morpho is reinstalled, and the system then throws a recoverable security
 * exception carrying a consent dialog. That is caught and shown rather than
 * surfacing as an inexplicable failure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionsSheet(
    file: MorphoFile,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onUseTool: () -> Unit,
    onChanged: () -> Unit
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var renaming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(file.name) }
    var armedDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    // Re-runs the pending action once the user grants access.
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val consent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) pending?.invoke()
        pending = null
    }

    // Everything here already lives on the device, in Downloads/Morpho or
    // Pictures/Morpho. What was missing is putting a copy somewhere the user
    // chooses, which is what "download" means for a file the app filed away.
    val saveCopy = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(file.mime.ifBlank { "*/*" })
    ) { dest ->
        if (dest != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val input = ctx.contentResolver.openInputStream(file.uri)
                        ?: return@runCatching false
                    val out = ctx.contentResolver.openOutputStream(dest)
                        ?: return@runCatching false
                    input.use { i -> out.use { o -> i.copyTo(o) } }
                    true
                }.getOrDefault(false)
            }
            if (ok) onDismiss() else error = "Morpho couldn't save a copy there."
        }
    }

    fun runGuarded(sender: IntentSender?, retry: () -> Unit) {
        if (sender == null) return
        pending = retry
        consent.launch(IntentSenderRequest.Builder(sender).build())
    }

    fun refresh() {
        scope.launch {
            withContext(Dispatchers.IO) { FileStore.refresh(ctx) }
            onChanged()
        }
    }

    fun doRename() {
        val target = newName.trim()
        if (target.isBlank() || target == file.name) { renaming = false; return }
        when (val outcome = renameFile(ctx, file, target)) {
            is FileOp.Ok -> { renaming = false; error = ""; refresh(); onDismiss() }
            is FileOp.NeedsConsent -> runGuarded(outcome.sender) { doRename() }
            is FileOp.Failed -> error = outcome.reason
        }
    }

    fun doDelete() {
        when (val outcome = deleteFile(ctx, file)) {
            is FileOp.Ok -> { error = ""; refresh(); onDismiss() }
            is FileOp.NeedsConsent -> runGuarded(outcome.sender) { doDelete() }
            is FileOp.Failed -> error = outcome.reason
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Paper,
        scrimColor = Ink.copy(alpha = 0.32f)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter)) {
            Text(file.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(file.kind.label, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
            Spacer(Modifier.height(Space.lg))

            if (renaming) {
                Row(
                    Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk)
                        .padding(horizontal = Space.md, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        BasicTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                            cursorBrush = SolidColor(Cobalt),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Box(Modifier.weight(1f)) {
                        SheetAction("close", "Cancel", InkSoft) { renaming = false }
                    }
                    Box(Modifier.weight(1f)) {
                        SheetAction("check", "Save name", Cobalt) { doRename() }
                    }
                }
            } else {
                SheetAction("chevron-right", "Open", Ink, onClick = onOpen)
                SheetAction("sparkle", "Use a tool on this", Cobalt, onClick = onUseTool)
                SheetAction("share", "Share", Ink) {
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
                SheetAction("download", "Save a copy\u2026", Ink) { saveCopy.launch(file.name) }
                SheetAction("pencil", "Rename", Ink) { newName = file.name; renaming = true }
                SheetAction(
                    "trash",
                    if (armedDelete) "Tap again to delete" else "Delete",
                    Warn
                ) {
                    if (armedDelete) doDelete() else armedDelete = true
                }
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(Space.sm))
                Text(error, color = Warn, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
private fun SheetAction(glyph: String, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Shape.tile)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick
            )
            .padding(vertical = 13.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(Shape.chip).background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(glyph, tint = tint, size = 17.dp) }
        Spacer(Modifier.width(Space.md))
        Text(label, color = tint, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal)
    }
}

sealed interface FileOp {
    data object Ok : FileOp
    /** The system will ask the user; re-run the action if they agree. */
    data class NeedsConsent(val sender: IntentSender?) : FileOp
    data class Failed(val reason: String) : FileOp
}

private fun renameFile(ctx: android.content.Context, file: MorphoFile, name: String): FileOp = try {
    // Keep the original extension unless the user typed one.
    val ext = file.name.substringAfterLast('.', "")
    val target = if (ext.isNotEmpty() && !name.endsWith(".$ext", ignoreCase = true))
        "$name.$ext" else name
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, target)
    }
    val rows = ctx.contentResolver.update(file.uri, values, null, null)
    if (rows > 0) FileOp.Ok else FileOp.Failed("Morpho couldn't rename that file.")
} catch (e: Exception) {
    recoverable(e) ?: FileOp.Failed("Morpho couldn't rename that file.")
}

private fun deleteFile(ctx: android.content.Context, file: MorphoFile): FileOp = try {
    val rows = ctx.contentResolver.delete(file.uri, null, null)
    if (rows > 0) FileOp.Ok else FileOp.Failed("Morpho couldn't delete that file.")
} catch (e: Exception) {
    recoverable(e) ?: FileOp.Failed("Morpho couldn't delete that file.")
}

/**
 * Ownership of a file is lost when the app is reinstalled, and the system then
 * refuses the write but hands back a consent dialog to show.
 */
private fun recoverable(e: Exception): FileOp? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException)
        FileOp.NeedsConsent(e.userAction.actionIntent.intentSender)
    else null
