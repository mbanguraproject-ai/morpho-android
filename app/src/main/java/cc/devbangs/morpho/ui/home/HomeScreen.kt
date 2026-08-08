package cc.devbangs.morpho.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            bottom = contentPadding.calculateBottomPadding() + Space.xxl
        )
    ) {
        item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
        item { Brand() }
        item { BigSearch(onOpenSearch) }
        item { Spacer(Modifier.height(Space.xl)) }

        item { Eyebrow("BROWSE BY CATEGORY") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Space.gutter),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                items(cats, key = { it.id }) { c ->
                    CategoryChip(c, ToolRegistry.byCategory[c]?.size ?: 0) { onOpenCategory(c.id) }
                }
            }
        }
        item { Spacer(Modifier.height(Space.xl)) }

        if (popular.isNotEmpty()) {
            item { Eyebrow("POPULAR RIGHT NOW") }
            items(popular.chunked(2)) { row ->
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Space.gutter, vertical = Space.xs + 2.dp),
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

@Composable
private fun Brand() {
    Row(
        Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.xl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // solid cobalt lockup mark
        Box(
            Modifier.size(34.dp).clip(Shape.chip).background(Cobalt),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("cat-converter", tint = Paper, size = 19.dp, strokeWidth = 2.3f) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Morpho File", style = MaterialTheme.typography.titleLarge, color = Ink,
                fontWeight = FontWeight.Bold)
            Text("82 tools · offline-first", style = MaterialTheme.typography.labelSmall,
                color = InkSoft)
        }
    }
}

@Composable
private fun BigSearch(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .padding(horizontal = Space.gutter)
            .fillMaxWidth()
            .morphLift(Shape.card, elevation = 10.dp, pressed = pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Space.lg, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphoIcon("tab-search", tint = Cobalt, size = 22.dp, strokeWidth = 2.1f)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text("What do you need to do?", color = Ink,
                style = MaterialTheme.typography.titleMedium)
            Text("Convert, compress, generate, extract…", color = InkFaint,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text,
        color = InkFaint,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.sm)
    )
}

@Composable
private fun CategoryChip(category: ToolCategory, count: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .morphLift(Shape.pill, elevation = 6.dp, pressed = pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).clip(Shape.pill).background(category.accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("cat-${category.id}", tint = Paper, size = 17.dp, strokeWidth = 2.1f) }
        Spacer(Modifier.width(8.dp))
        Text(category.label, style = MaterialTheme.typography.titleMedium, color = Ink)
    }
}
