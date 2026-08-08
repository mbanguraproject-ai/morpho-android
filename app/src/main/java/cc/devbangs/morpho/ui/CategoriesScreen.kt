package cc.devbangs.morpho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.CategoryCard
import cc.devbangs.morpho.ui.theme.Ink
import cc.devbangs.morpho.ui.theme.Paper

@Composable
fun CategoriesScreen(
    onOpenCategory: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val cats = ToolCategory.entries
    LazyColumn(
        Modifier.fillMaxSize().background(Paper),
        contentPadding = PaddingValues(
            bottom = contentPadding.calculateBottomPadding() + Space.xl
        )
    ) {
        item {
            Column(Modifier.padding(horizontal = Space.gutter)) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(Space.lg))
                Text("All tools", style = MaterialTheme.typography.displaySmall, color = Ink)
                Text("${ToolRegistry.all.size} tools across ${cats.size} categories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cc.devbangs.morpho.ui.theme.InkSoft)
                Spacer(Modifier.height(Space.md))
            }
        }
        items(cats.chunked(2)) { row ->
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = Space.gutter, vertical = Space.xs),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                row.forEach { cat ->
                    CategoryCard(
                        category = cat,
                        count = ToolRegistry.byCategory[cat]?.size ?: 0,
                        onClick = { onOpenCategory(cat.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
