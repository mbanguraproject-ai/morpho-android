package cc.devbangs.morpho.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.data.ToolSearch
import cc.devbangs.morpho.ui.components.Eyebrow
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.components.morphLift
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

private val HeroTint = Color(0xFFF0F3FF)

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenTool: (String) -> Unit,
    contentPadding: PaddingValues
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val groups = remember(query) {
        if (query.isBlank()) emptyList() else ToolSearch.grouped(query)
    }
    val popular = ToolRegistry.popular
    val nothing = if (query.isBlank()) popular.isEmpty() else groups.isEmpty()
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(Paper)) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(0f to HeroTint, 1f to Paper)
            )
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                Modifier.fillMaxWidth().padding(start = Space.sm, end = Space.gutter, top = Space.sm, bottom = Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonMorpho("chevron-left", onBack, contentDescription = "Back")
                Spacer(Modifier.width(Space.xs))
                Row(
                    Modifier.weight(1f).morphLift(Shape.card, elevation = 8.dp)
                        .padding(horizontal = Space.md, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MorphoIcon("tab-search", tint = Cobalt, size = 20.dp)
                    Spacer(Modifier.width(Space.md))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty())
                            Text("Search tools…", color = InkFaint, fontSize = 15.sp)
                        BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                            cursorBrush = SolidColor(Cobalt),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                    }
                    if (query.isNotEmpty())
                        IconButtonMorpho("close", { query = "" }, tint = InkFaint,
                        contentDescription = "Clear search")
                }
            }
        }

        if (nothing) {
            Box(Modifier.fillMaxSize().padding(Space.gutter), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MorphoIcon("tab-search", tint = InkFaint, size = 32.dp)
                    Spacer(Modifier.height(Space.md))
                    Text("No tools match “$query”.", color = InkSoft,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Try “pdf”, “compress”, or “qr”.", color = InkFaint,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = Space.gutter),
                contentPadding = PaddingValues(
                    top = Space.sm, bottom = contentPadding.calculateBottomPadding() + Space.xxl
                )
            ) {
                if (query.isBlank()) {
                    item { Eyebrow("POPULAR", gutter = false) }
                    items(popular, key = { it.id }) { t -> SearchRow(t) { onOpenTool(t.id) } }
                } else {
                    groups.forEach { (group, tools) ->
                        item(key = "h-" + group.name) { Eyebrow(group.label.uppercase(), gutter = false) }
                        items(tools, key = { it.id }) { t -> SearchRow(t) { onOpenTool(t.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(t: Tool, onClick: () -> Unit) {
    val accent = t.category.accent
    Row(
        Modifier.fillMaxWidth().clip(Shape.tile).clickable(onClick = onClick)
            .padding(vertical = Space.sm, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(Shape.chip).background(accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(t.iconKey, tint = Paper, size = 22.dp) }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(t.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.short, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Space.sm))
        if (t.offline) Box(
            Modifier.size(22.dp).clip(Shape.pill).background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("check", tint = accent, size = 13.dp) }
        Spacer(Modifier.width(6.dp))
        MorphoIcon("chevron-right", tint = InkFaint, size = 16.dp)
    }
}
