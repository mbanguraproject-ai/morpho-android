package cc.devbangs.morpho.ui.tool.kit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun hasAiTool(id: String): Boolean = id in setOf(
    "grammar-checker", "ai-text-rewriter", "essay-writer",
    "paragraph-generator", "audio-transcriber"
)

@Composable
fun AiTool(id: String, accent: Color) {
    if (id == "audio-transcriber") TranscribeTool(accent) else AiTextTool(id, accent)
}

/** What each tool sends, and what it asks the user for. */
private data class AiSpec(
    val task: String,
    val label: String,
    val placeholder: String,
    val action: String,
    val tones: List<String>?
)

private fun specFor(id: String): AiSpec = when (id) {
    "grammar-checker" -> AiSpec(
        "grammar", "TEXT TO CHECK",
        "Paste the text you want corrected.", "Check grammar", null
    )
    "ai-text-rewriter" -> AiSpec(
        "rewrite", "TEXT TO REWRITE",
        "Paste the text you want reworded.", "Rewrite",
        listOf("Clearer", "Shorter", "Friendlier", "More formal")
    )
    "essay-writer" -> AiSpec(
        "essay", "WHAT SHOULD IT BE ABOUT?",
        "The impact of mobile money on small traders", "Write essay",
        listOf("Neutral", "Persuasive", "Academic")
    )
    else -> AiSpec(
        "paragraph", "WHAT SHOULD IT BE ABOUT?",
        "Why regular backups matter", "Write paragraph",
        listOf("Neutral", "Friendly", "Formal")
    )
}

@Composable
private fun AiTextTool(id: String, accent: Color) {
    val spec = remember(id) { specFor(id) }
    var input by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(spec.tones?.firstOrNull() ?: "") }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        FieldLabel(spec.label)
        ToolInput(input, { input = it; error = "" }, spec.placeholder, minLines = 5)

        spec.tones?.let { tones ->
            FieldLabel("TONE")
            AiChipRow(tones, tone, accent) { tone = it }
        }

        if (busy) ProcessingCard("Working on it\u2026", accent)
        else ToolButton(spec.action, accent, enabled = input.isNotBlank()) {
            busy = true; error = ""; result = ""
            val text = input
            val opts = if (tone.isBlank()) emptyMap() else mapOf("tone" to tone.lowercase())
            scope.launch {
                when (val r = withContext(Dispatchers.IO) { aiText(spec.task, text, opts) }) {
                    is AiOutcome.Success -> result = r.text
                    is AiOutcome.Failure -> error = r.reason
                }
                busy = false
            }
        }

        if (error.isNotEmpty()) ToolErrorCard(
            "Couldn't finish that", error, accent
        )

        if (result.isNotEmpty()) {
            ToolResult(result, accent, mono = false, label = "RESULT")
            Text(
                "Written by a model. Read it before you use it - it can be " +
                    "confidently wrong.",
                color = InkFaint, fontSize = 12.sp
            )
        }
    }
}

/**
 * Speech to text.
 *
 * Whisper Large v3 rather than the Turbo variant: Turbo trades accuracy on
 * accented and non-English speech for speed, and Morpho ships to 177 countries.
 */
@Composable
private fun TranscribeTool(accent: Color) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { u -> if (u != null) { uri = u; result = ""; error = "" } }

    LaunchedEffect(Unit) {
        cc.devbangs.morpho.workflow.WorkflowBus.consume()?.let {
            uri = bytesToTempUri(ctx, it.bytes, it.mime)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRowAi(
            if (uri == null) "Choose audio or video" else "File selected \u2713",
            accent
        ) { picker.launch(arrayOf("audio/*", "video/*")) }

        if (uri != null) {
            Text(
                "Speech is turned into text on Morpho's server. Up to 24MB, " +
                    "which is roughly half an hour of voice recording.",
                color = InkFaint, fontSize = 12.sp
            )

            if (busy) ProcessingCard("Listening to your recording\u2026", accent)
            else ToolButton("Transcribe", accent) {
                busy = true; error = ""; result = ""
                val u = uri!!
                scope.launch {
                    when (val r = withContext(Dispatchers.IO) { aiTranscribe(ctx, u) }) {
                        is AiOutcome.Success -> result = r.text
                        is AiOutcome.Failure -> error = r.reason
                    }
                    busy = false
                }
            }
        }

        if (error.isNotEmpty()) ToolErrorCard("Couldn't transcribe that", error, accent)

        if (result.isNotEmpty()) {
            ToolResult(result, accent, mono = false, label = "TRANSCRIPT")
            Text(
                "Automatic transcription. Check names, numbers and anything that " +
                    "matters before relying on it.",
                color = InkFaint, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AiChipRow(
    options: List<String>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.weight(1f).clip(Shape.field)
                    .background(if (on) accent else PaperSunk)
                    .clickable { onSelect(o) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(o, color = if (on) Paper else InkSoft, fontSize = 13.sp)
            }
        }
    }
}

/** Local: every PickRow in the kit is private to its own file. */
@Composable
private fun PickRowAi(label: String, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cc.devbangs.morpho.ui.icon.MorphoIcon("cat-audio", tint = accent, size = 20.dp)
        Spacer(Modifier.width(Space.md))
        Text(label, color = accent, fontSize = 15.sp)
    }
}
