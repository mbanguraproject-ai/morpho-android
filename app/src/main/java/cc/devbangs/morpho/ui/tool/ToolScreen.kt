package cc.devbangs.morpho.ui.tool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.IconBadge
import cc.devbangs.morpho.ui.components.MetaChip
import cc.devbangs.morpho.ui.components.MorphoTopBar
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

@Composable
fun ToolScreen(
    toolId: String,
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val tool = ToolRegistry.byId(toolId)
    Column(Modifier.fillMaxSize().background(Paper)) {
        MorphoTopBar(title = tool?.name ?: "Tool", onBack = onBack)
        if (tool == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Unknown tool", color = InkSoft)
            }
            return
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Space.gutter, end = Space.gutter, top = Space.md,
                    bottom = contentPadding.calculateBottomPadding() + Space.xl
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(tool.iconKey, tool.category.accent, size = 52)
                Spacer(Modifier.width(Space.md))
                Column(Modifier.weight(1f)) {
                    Text(tool.short, style = MaterialTheme.typography.bodyLarge, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    MetaChip(
                        if (tool.offline) "Works offline" else "Coming soon",
                        filled = tool.offline
                    )
                }
            }
            Spacer(Modifier.height(Space.xl))

            // Tool body dispatch — real implementations land here.
            ToolBody(toolId = tool.id, offline = tool.offline)
        }
    }
}

/** Placeholder body; real per-tool UIs replace this via ToolHost dispatch. */
@Composable
private fun ToolBody(toolId: String, offline: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(Shape.card)
            .background(PaperSunk)
            .padding(Space.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphoIcon(if (offline) "settings" else "clock", tint = InkFaint, size = 30.dp)
            Spacer(Modifier.height(Space.md))
            Text(
                if (offline) "Tool interface loads here."
                else "This tool needs a server engine and\nlands in a later build.",
                color = InkSoft,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
