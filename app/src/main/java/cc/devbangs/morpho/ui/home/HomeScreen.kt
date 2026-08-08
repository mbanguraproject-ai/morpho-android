package cc.devbangs.morpho.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.*
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

@Composable
fun HomeScreen(
    onOpenTool: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenSearch: () -> Unit,
    contentPadding: PaddingValues
) {
    val cats = ToolCategory.entries
    val popular = ToolRegistry.popular

    LazyColumn(
        Modifier.fillMaxSize().background(Paper),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + Space.xl
        )
    ) {
        item { Header() }
        item { SearchEntry(onOpenSearch) }
        item { Spacer(Modifier.height(Space.md)) }

        if (popular.isNotEmpty()) {
            item { SectionHeader("Popular") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    items(popular, key = { it.id }) { t ->
                        PopularCard(t) { onOpenTool(t.id) }
                    }
                }
            }
            item { Spacer(Modifier.height(Space.lg)) }
        }

        item { SectionHeader("Categories") }
        // 2-col category grid via chunked rows (keeps LazyColumn simple + no nested scroll)
        items(cats.chunked(2)) { row ->
            Row(
                Modifier
                    .fillMaxWidth()
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

@Composable
private fun Header() {
    Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MorphoIcon("cat-converter", tint = Cobalt, size = 22.dp, strokeWidth = 2.2f)
            Spacer(Modifier.width(8.dp))
            Text(
                "Morpho File",
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(Space.md))
        Text(
            "Every file tool, one clean app.",
            style = MaterialTheme.typography.displaySmall,
            color = Ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "82 tools · works offline where it counts",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft
        )
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = Space.gutter, vertical = Space.sm)
            .fillMaxWidth()
            .clip(Shape.field)
            .background(PaperSunk)
            .border(1.dp, PaperLine, Shape.field)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphoIcon("tab-search", tint = InkFaint, size = 19.dp)
        Spacer(Modifier.width(Space.md))
        Text("Search 82 tools…", color = InkFaint, fontSize = 15.sp)
    }
}

@Composable
private fun PopularCard(tool: Tool, onClick: () -> Unit) {
    Column(
        Modifier
            .width(150.dp)
            .clip(Shape.tile)
            .background(Paper)
            .border(1.dp, PaperLine, Shape.tile)
            .clickable(onClick = onClick)
            .padding(Space.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(tool.iconKey, tool.category.accent, size = 40)
            Spacer(Modifier.weight(1f))
            StatusDot(tool.offline)
        }
        Spacer(Modifier.height(Space.md))
        Text(tool.name, style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 2)
    }
}
