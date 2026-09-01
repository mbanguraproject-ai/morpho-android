package cc.devbangs.morpho.ui.category

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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.data.ToolSearch
import cc.devbangs.morpho.ui.components.Eyebrow
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.components.cornerPetal
import cc.devbangs.morpho.ui.components.morphLift
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
    val tint = category.accent
    val sections = remember(category) { ToolSearch.curate(tools) }

    Column(Modifier.fillMaxSize().background(Paper)) {
        // Fixed, not scrolled with the content: every other pushed screen keeps
        // its header - and its back button - in place. Same 56dp row shape as
        // ToolScreen, since Category to Tool is the main path through the app.
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(0f to tint.copy(alpha = 0.10f), 1f to Paper)
            )
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonMorpho("chevron-left", onBack, contentDescription = "Back")
                Spacer(Modifier.width(Space.xs))
                Box(
                    Modifier.size(40.dp).clip(Shape.card).background(tint),
                    contentAlignment = Alignment.Center
                ) { MorphoIcon("cat-${category.id}", tint = Paper, size = 21.dp) }
                Spacer(Modifier.width(Space.md))
                Column(Modifier.weight(1f)) {
                    Text(category.label, style = MaterialTheme.typography.headlineSmall,
                        color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${tools.size} tools · ${tools.count { it.offline }} work offline",
                        style = MaterialTheme.typography.bodyMedium, color = InkSoft,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(Space.sm))
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = Space.xs,
                bottom = contentPadding.calculateBottomPadding() + Space.xxl
            )
        ) {
        if (sections.isEmpty()) {
            items(tools.chunked(2), key = { it.first().id }) { row ->
                ToolGridRow(row, onOpenTool)
            }
        } else {
            sections.forEach { (label, list) ->
                item(key = "sec-" + label) { Eyebrow(label.uppercase()) }
                items(list.chunked(2), key = { it.first().id }) { row ->
                    ToolGridRow(row, onOpenTool)
                }
            }
        }
        }
    }
}

@Composable
private fun ToolGridRow(row: List<Tool>, onOpenTool: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = 6.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        row.forEach { t ->
            ToolCardTall(t, { onOpenTool(t.id) }, Modifier.weight(1f).fillMaxHeight())
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ToolCardTall(t: Tool, onClick: () -> Unit, modifier: Modifier) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    val accent = t.category.accent
    Column(
        modifier
            .morphLift(Shape.card, elevation = 8.dp, pressed = pressed, accent = accent)
            .clickable(interactionSource = i, indication = null, onClick = onClick)
            .clipToBounds()
            .then(if (t.popular) Modifier.cornerPetal(accent) else Modifier)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(Shape.chip).background(accent),
                contentAlignment = Alignment.Center
            ) { MorphoIcon(t.iconKey, tint = Paper, size = 22.dp) }
            Spacer(Modifier.weight(1f))
            // One badge, not two: needing Plus matters more to know up front
            // than working offline, so the crown takes the slot when both apply.
            if (t.plus) Box(
                Modifier.size(22.dp).clip(Shape.pill).background(CobaltWash),
                contentAlignment = Alignment.Center
            ) {
                MorphoIcon("crown", tint = Cobalt, size = 13.dp,
                    contentDescription = "Plus tool")
            } else if (t.offline) Box(
                Modifier.size(22.dp).clip(Shape.pill).background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { MorphoIcon("check", tint = accent, size = 13.dp,
                contentDescription = "Works offline") }
        }
        Spacer(Modifier.height(14.dp))
        Text(t.name, style = MaterialTheme.typography.titleMedium, color = Ink,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 40.dp))
        Spacer(Modifier.height(2.dp))
        Text(t.short, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
