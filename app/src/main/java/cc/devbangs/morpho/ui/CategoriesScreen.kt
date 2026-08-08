package cc.devbangs.morpho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.morphLift
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

@Composable
fun CategoriesScreen(
    onOpenCategory: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val cats = ToolCategory.entries
    LazyColumn(
        Modifier.fillMaxSize().background(Paper),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Space.xxl)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(0f to Color_HeroTint, 1f to Paper)
                )
            ) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.xl, bottom = Space.lg)) {
                    Text("All tools", style = MaterialTheme.typography.displaySmall, color = Ink)
                    Text("${ToolRegistry.all.size} tools across ${cats.size} categories",
                        style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                }
            }
        }
        items(cats.chunked(2)) { row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                row.forEach { c -> BigCategoryCard(c, { onOpenCategory(c.id) }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private val Color_HeroTint = androidx.compose.ui.graphics.Color(0xFFF0F3FF)

@Composable
private fun BigCategoryCard(c: ToolCategory, onClick: () -> Unit, modifier: Modifier) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    val count = ToolRegistry.byCategory[c]?.size ?: 0
    Column(
        modifier
            .morphLift(Shape.card, elevation = 8.dp, pressed = pressed, accent = c.accent)
            .clickable(interactionSource = i, indication = null, onClick = onClick)
            .padding(Space.lg)
    ) {
        Box(
            Modifier.size(46.dp).clip(Shape.chip).background(c.accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("cat-${c.id}", tint = Paper, size = 24.dp) }
        Spacer(Modifier.height(Space.md))
        Text(c.label, style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(c.blurb, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
            maxLines = 2, modifier = Modifier.heightIn(min = 38.dp))
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$count tools", color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            MorphoIcon("chevron-right", tint = c.accent, size = 13.dp)
        }
    }
}
