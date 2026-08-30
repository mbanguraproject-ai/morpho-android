package cc.devbangs.morpho.ui.tool.kit

import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

/**
 * Blueprint section 26 - an error answers what happened, whether it can be
 * recovered, and what to do next. Never leave a failure unexplained.
 */
@Composable
fun ToolErrorCard(
    title: String,
    body: String,
    accent: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().clip(Shape.card)
            .background(Color(0xFFB4231E).copy(alpha = 0.06f))
            .border(1.dp, Color(0xFFB4231E).copy(alpha = 0.20f), Shape.card)
            .padding(Space.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MorphoIcon("info", tint = Color(0xFFB4231E), size = 18.dp)
            Spacer(Modifier.width(Space.sm))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        }
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.md))
            Box(
                Modifier.clip(Shape.pill).background(accent)
                    .clickable(onClick = onAction)
                    .padding(horizontal = Space.lg, vertical = 9.dp)
            ) {
                Text(actionLabel, color = Paper, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Section label above a field/result. */
@Composable
fun FieldLabel(text: String) {
    Text(text, color = InkFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
}

/** Multiline text input in the Morpho style. */
@Composable
fun ToolInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 4,
    mono: Boolean = false
) {
    Box(
        modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk)
            .border(1.dp, PaperLine, Shape.field).padding(14.dp)
    ) {
        if (value.isEmpty())
            Text(placeholder, color = InkFaint, fontSize = 15.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif)
        BasicTextField(
            value = value, onValueChange = onValueChange,
            textStyle = TextStyle(color = Ink, fontSize = 15.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif),
            cursorBrush = SolidColor(Cobalt),
            minLines = minLines,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Read-only result block with a copy affordance. */
@Composable
fun ToolResult(
    text: String,
    accent: Color,
    mono: Boolean = true,
    label: String = "RESULT"
) {
    val ctx = LocalContext.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(label, color = InkFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            if (text.isNotEmpty())
                CopyChip(accent) { copyToClipboard(ctx, text) }
        }
        Box(
            Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk)
                .border(1.dp, PaperLine, Shape.field).padding(14.dp)
        ) {
            Text(
                text.ifEmpty { "—" },
                color = if (text.isEmpty()) InkFaint else Ink,
                fontSize = 15.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun CopyChip(accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(Shape.pill).background(accent.copy(alpha = 0.10f))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphoIcon("copy", tint = accent, size = 13.dp)
        Spacer(Modifier.width(5.dp))
        Text("Copy", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Primary action button (solid accent). */
@Composable
fun ToolButton(text: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(Shape.field)
            .background(if (enabled) accent else PaperLine)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Paper, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Stat pill row, e.g. word/char counts. */
@Composable
fun StatGrid(stats: List<Pair<String, String>>, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(
                        Modifier.weight(1f).clip(Shape.tile).background(PaperSunk).padding(14.dp)
                    ) {
                        Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(label, color = InkSoft, fontSize = 12.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Morpho", text))
    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
}

/** On-brand animated processing state — shows while a tool is working. */
@androidx.compose.runtime.Composable
fun ProcessingCard(label: String, accent: Color) {
    val transition = rememberInfiniteTransition(label = "processing")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "spin"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.07f))
            .padding(vertical = 34.dp, horizontal = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(56.dp).rotate(angle).clip(Shape.chip)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("spinner", tint = accent, size = 28.dp) }
        Spacer(Modifier.height(14.dp))
        Text(label, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.alpha(pulse))
        Spacer(Modifier.height(4.dp))
        Text("Working on your file…", color = InkFaint, fontSize = 12.sp)
    }
}
