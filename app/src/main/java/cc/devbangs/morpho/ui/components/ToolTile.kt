package cc.devbangs.morpho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

/** Grid tool tile — glyph badge, name, short line, offline dot. */
@Composable
fun ToolTile(
    tool: Tool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = tool.category.accent
    Column(
        modifier = modifier
            .clip(Shape.tile)
            .background(Paper)
            .border(1.dp, PaperLine, Shape.tile)
            .clickable(onClick = onClick)
            .padding(Space.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(tool.iconKey, accent)
            Spacer(Modifier.weight(1f))
            StatusDot(tool.offline)
        }
        Spacer(Modifier.height(Space.md))
        Text(
            tool.name,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            tool.short,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** List-row variant (used in search + category list mode). */
@Composable
fun ToolRow(tool: Tool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(Shape.tile)
            .clickable(onClick = onClick)
            .padding(vertical = Space.sm, horizontal = Space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(tool.iconKey, tool.category.accent)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(tool.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tool.short, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Space.sm))
        StatusDot(tool.offline)
    }
}

@Composable
fun IconBadge(iconKey: String, accent: Color, size: Int = 44) {
    Box(
        Modifier
            .size(size.dp)
            .clip(Shape.chip)
            .background(accent.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        MorphoIcon(iconKey, tint = accent, size = (size * 0.5).dp, strokeWidth = 1.9f)
    }
}
