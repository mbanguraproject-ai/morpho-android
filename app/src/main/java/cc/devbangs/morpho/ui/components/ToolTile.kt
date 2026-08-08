package cc.devbangs.morpho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@Composable
fun ToolTile(tool: Tool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = modifier
            .morphLift(Shape.tile, elevation = 8.dp, pressed = pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(Space.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SolidIconChip(tool.iconKey, tool.category.accent, 40)
            Spacer(Modifier.weight(1f))
            if (tool.offline) OfflineTick(tool.category.accent)
        }
        Spacer(Modifier.height(Space.md))
        Text(
            tool.name,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 40.dp)
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
        SolidIconChip(tool.iconKey, tool.category.accent, 42)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(tool.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tool.short, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Space.sm))
        if (tool.offline) OfflineTick(tool.category.accent)
        MorphoIcon("chevron-right", tint = InkFaint, size = 16.dp)
    }
}

/** Small "works offline" affordance: accent tick, no label noise. */
@Composable
private fun OfflineTick(accent: Color) {
    Box(
        Modifier.size(20.dp).clip(Shape.pill).background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        MorphoIcon("check", tint = accent, size = 12.dp, strokeWidth = 2.4f)
    }
}

/** Kept for the tool detail header. */
@Composable
fun IconBadge(iconKey: String, accent: Color, size: Int = 44) =
    SolidIconChip(iconKey, accent, size)
