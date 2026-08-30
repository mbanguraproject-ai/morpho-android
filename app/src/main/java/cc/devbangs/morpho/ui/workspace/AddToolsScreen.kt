package cc.devbangs.morpho.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Recommender
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.data.Workspace
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.components.morphLift
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

private val HeroTint = Color(0xFFF0F3FF)

/**
 * Blueprint section 6 - "Add tools to your workspace".
 *
 * Search, category filtering, tool descriptions and add/remove controls in one
 * surface. Adding is immediate and reversible from the same row, so the user
 * can see the workspace take shape without leaving the screen.
 */
@Composable
fun AddToolsScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ToolCategory?>(null) }

    val results = remember(query, category) {
        val base = category?.let { ToolRegistry.byCategory[it].orEmpty() } ?: ToolRegistry.all
        val q = query.trim().lowercase()
        if (q.isEmpty()) base
        else base.filter {
            it.name.lowercase().contains(q) ||
                it.short.lowercase().contains(q) ||
                it.category.label.lowercase().contains(q)
        }
    }
    val added = Workspace.toolIds.size
    val showRecommended = query.isBlank() && category == null
    val recommended = if (showRecommended) Recommender.forWorkspace() else emptyList()
    val recLabel = if (Recommender.isPersonal(recommended)) "RECOMMENDED" else "POPULAR"

    Column(Modifier.fillMaxSize().background(Paper)) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(0f to HeroTint, 1f to Paper)
            )
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonMorpho("chevron-left", onBack)
                Spacer(Modifier.width(Space.xs))
                Column(Modifier.weight(1f)) {
                    Text("Add tools", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Text(
                        if (added == 0) "Nothing in your workspace yet"
                        else if (added == 1) "1 tool in your workspace"
                        else "$added tools in your workspace",
                        style = MaterialTheme.typography.bodyMedium, color = InkSoft
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = Space.gutter, vertical = Space.sm)
                    .morphLift(Shape.card, elevation = 8.dp)
                    .padding(horizontal = Space.md, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MorphoIcon("tab-search", tint = Cobalt, size = 19.dp)
                Spacer(Modifier.width(Space.md))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty())
                        Text("Search tools…", color = InkFaint, fontSize = 15.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(Cobalt),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty())
                    IconButtonMorpho("close", { query = "" }, tint = InkFaint)
            }

            LazyRow(
                Modifier.fillMaxWidth().padding(bottom = Space.md),
                contentPadding = PaddingValues(horizontal = Space.gutter),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                item {
                    FilterChip("All", category == null) { category = null }
                }
                items(ToolCategory.entries) { c ->
                    FilterChip(c.label, category == c, c.accent) {
                        category = if (category == c) null else c
                    }
                }
            }
        }

        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(Space.gutter), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MorphoIcon("tab-search", tint = InkFaint, size = 30.dp)
                    Spacer(Modifier.height(Space.md))
                    Text("No matching tools.", color = InkSoft,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Try a broader word, or pick a category.", color = InkFaint,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = Space.gutter),
                contentPadding = PaddingValues(
                    top = Space.xs, bottom = contentPadding.calculateBottomPadding() + Space.xxl
                )
            ) {
                if (recommended.isNotEmpty()) {
                    item { AddSectionLabel(recLabel) }
                    items(recommended, key = { "rec-" + it.tool.id }) { s ->
                        AddToolRow(s.tool, s.reason)
                    }
                    item { AddSectionLabel("ALL TOOLS") }
                }
                items(results, key = { it.id }) { t -> AddToolRow(t) }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    accent: Color = Cobalt,
    onClick: () -> Unit
) {
    Box(
        Modifier.clip(Shape.pill)
            .background(if (selected) accent else PaperSunk)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, color = if (selected) Paper else InkSoft,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AddSectionLabel(text: String) {
    Text(text, color = InkFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = Space.md, bottom = Space.xs))
}

@Composable
private fun AddToolRow(t: Tool, reason: String? = null) {
    val accent = t.category.accent
    val inWorkspace = Workspace.contains(t.id)
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.sm, horizontal = 2.dp),
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
            Text(reason ?: t.short, style = MaterialTheme.typography.bodyMedium,
                color = if (reason != null) Cobalt else InkSoft,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Space.md))
        Box(
            Modifier.size(34.dp).clip(Shape.pill)
                .background(if (inWorkspace) Cobalt else CobaltWash)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { Workspace.toggle(t.id) },
            contentAlignment = Alignment.Center
        ) {
            MorphoIcon(
                if (inWorkspace) "check" else "plus",
                tint = if (inWorkspace) Paper else Cobalt,
                size = 17.dp
            )
        }
    }
}
