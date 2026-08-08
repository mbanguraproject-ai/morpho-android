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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

@Composable
fun CategoryCard(
    category: ToolCategory,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = category.accent
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Column(
        modifier = modifier
            .morphLift(Shape.card, elevation = 9.dp, pressed = pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(Space.lg)
    ) {
        SolidIconChip(glyph = "cat-${category.id}", accent = accent, size = 44)
        Spacer(Modifier.height(Space.md))
        Text(
            category.label,
            style = MaterialTheme.typography.titleLarge,
            color = Ink
        )
        Spacer(Modifier.height(3.dp))
        Text(
            category.blurb,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 38.dp)
        )
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$count tools",
                color = Ink,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(6.dp))
            MorphoIcon("chevron-right", tint = accent, size = 13.dp, strokeWidth = 2.4f)
        }
    }
}

/** Solid accent-filled rounded chip with a white glyph — the confident category mark. */
@Composable
fun SolidIconChip(glyph: String, accent: androidx.compose.ui.graphics.Color, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(Shape.chip)
            .background(accent),
        contentAlignment = Alignment.Center
    ) {
        MorphoIcon(glyph, tint = Paper, size = (size * 0.52).dp, strokeWidth = 2f)
    }
}
