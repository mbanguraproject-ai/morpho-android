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
    Column(
        modifier = modifier
            .clip(Shape.card)
            .background(Paper)
            .border(1.dp, PaperLine, Shape.card)
            .clickable(onClick = onClick)
            .padding(Space.lg)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(Shape.chip)
                .background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            MorphoIcon("cat-${category.id}", tint = accent, size = 24.dp, strokeWidth = 2f)
        }
        Spacer(Modifier.height(Space.md))
        Text(
            category.label,
            style = MaterialTheme.typography.titleLarge,
            color = Ink
        )
        Spacer(Modifier.height(2.dp))
        Text(
            category.blurb,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(Space.md))
        Text(
            "$count tools",
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}
