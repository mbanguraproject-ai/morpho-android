package cc.devbangs.morpho.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.MorphoTopBar
import cc.devbangs.morpho.ui.components.ToolTile
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

@Composable
fun CategoryScreen(
    categoryId: String,
    onBack: () -> Unit,
    onOpenTool: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val category = ToolCategory.from(categoryId)
    val tools = ToolRegistry.byCategory[category].orEmpty()

    Column(Modifier.fillMaxSize().background(Paper)) {
        MorphoTopBar(title = category.label, onBack = onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + Space.xl
            )
        ) {
            item {
                Row(
                    Modifier.padding(
                        start = Space.gutter, end = Space.gutter,
                        top = Space.xs, bottom = Space.lg
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(52.dp).clip(Shape.chip)
                            .background(category.accent.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MorphoIcon("cat-${category.id}", tint = category.accent,
                            size = 27.dp, strokeWidth = 2f)
                    }
                    Spacer(Modifier.width(Space.md))
                    Column(Modifier.weight(1f)) {
                        Text(category.blurb, style = MaterialTheme.typography.bodyLarge, color = Ink)
                        Text("${tools.size} tools", style = MaterialTheme.typography.bodyMedium,
                            color = category.accent)
                    }
                }
            }
            items(tools.chunked(2)) { row ->
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Space.gutter, vertical = Space.xs),
                    horizontalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    row.forEach { t ->
                        ToolTile(t, onClick = { onOpenTool(t.id) }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
