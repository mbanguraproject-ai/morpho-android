package cc.devbangs.morpho.ui.home

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.cornerPetal
import cc.devbangs.morpho.ui.components.morphLift
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

@Composable
fun HomeScreen(
    onOpenTool: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAllCategories: () -> Unit,
    contentPadding: PaddingValues
) {
    val cats = ToolCategory.entries
    val popular = ToolRegistry.popular

    LazyColumn(
        Modifier.fillMaxSize().background(Paper),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Space.xxl)
    ) {
        // Tight top bar — brand left, settings right. No tagline, no marketing.
        item {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter,
                    top = Space.md, bottom = Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(34.dp).clip(Shape.chip).background(Cobalt),
                    contentAlignment = Alignment.Center
                ) { MorphoIcon("cat-converter", tint = Paper, size = 19.dp) }
                Spacer(Modifier.width(10.dp))
                Text("Morpho", style = MaterialTheme.typography.titleLarge, color = Ink,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SettingsButton(onOpenSettings)
            }
        }

        // Real search box: compact, short placeholder, no clutter
        item { SearchBar(onOpenSearch) }

        item { RowHeader("CATEGORIES", "All 9", onSeeAllCategories) }
        itemsIndexed(cats.take(4).chunked(2)) { idx, row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = 5.dp)
                    .reveal(idx),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                row.forEach { c ->
                    CategoryMini(c, ToolRegistry.byCategory[c]?.size ?: 0,
                        { onOpenCategory(c.id) }, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (popular.isNotEmpty()) {
            item { RowHeader("POPULAR", null) }
            itemsIndexed(popular.chunked(2)) { idx, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = 5.dp)
                        .reveal(idx + 2),
                    horizontalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    row.forEach { t -> PopularCard(t, { onOpenTool(t.id) }, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    Box(
        Modifier.size(38.dp).morphLift(Shape.chip, elevation = 4.dp, pressed = pressed)
            .clickable(interactionSource = i, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { MorphoIcon("settings", tint = InkSoft, size = 19.dp) }
}

@Composable
private fun SearchBar(onClick: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    Row(
        Modifier.padding(horizontal = Space.gutter, vertical = Space.sm).fillMaxWidth()
            .morphLift(Shape.card, elevation = 10.dp, pressed = pressed)
            .clickable(interactionSource = i, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Search tools", style = MaterialTheme.typography.titleMedium, color = InkFaint,
            modifier = Modifier.weight(1f))
        MorphoIcon("tab-search", tint = Cobalt, size = 20.dp)
    }
}

@Composable
private fun RowHeader(eyebrow: String, action: String?, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter,
            top = Space.lg, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(eyebrow, color = InkFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.weight(1f))
        if (action != null) Text(
            action, color = Cobalt, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier
        )
    }
}

@Composable
private fun CategoryMini(c: ToolCategory, count: Int, onClick: () -> Unit, modifier: Modifier) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    Row(
        modifier.morphLift(Shape.tile, elevation = 8.dp, pressed = pressed, accent = c.accent)
            .clickable(interactionSource = i, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(Shape.chip).background(c.accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("cat-${c.id}", tint = Paper, size = 20.dp) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(c.label, style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1)
            Text("$count tools", fontSize = 11.sp, color = InkFaint)
        }
    }
}

@Composable
private fun PopularCard(t: Tool, onClick: () -> Unit, modifier: Modifier) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    val accent = t.category.accent
    Column(
        modifier.morphLift(Shape.card, elevation = 10.dp, pressed = pressed, accent = accent)
            .clickable(interactionSource = i, indication = null, onClick = onClick)
            .clipToBounds()
            .cornerPetal(accent)
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(Shape.chip).background(accent),
                contentAlignment = Alignment.Center
            ) { MorphoIcon(t.iconKey, tint = Paper, size = 22.dp) }
            Spacer(Modifier.weight(1f))
            if (t.offline) Box(
                Modifier.size(22.dp).clip(Shape.pill).background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { MorphoIcon("check", tint = accent, size = 13.dp) }
        }
        Spacer(Modifier.height(16.dp))
        Text(t.name, style = MaterialTheme.typography.titleMedium, color = Ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(t.short, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Staggered rise + fade-in reveal for list items. */
@Composable
private fun Modifier.reveal(indexKey: Int): Modifier {
    var shown by remember(indexKey) { mutableStateOf(false) }
    LaunchedEffect(indexKey) { shown = true }
    val p by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 420, delayMillis = (indexKey.coerceAtMost(8)) * 45, easing = EaseOutCubic),
        label = "reveal"
    )
    return this
        .graphicsLayer { translationY = (1f - p) * 34f; this.alpha = p }
}
