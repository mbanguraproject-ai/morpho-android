package cc.devbangs.morpho.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.Workspace
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

private val HeroTint = Color(0xFFF0F3FF)

/**
 * Blueprint section 25 - reorder and remove workspace tools.
 *
 * A single-column list with explicit move and remove controls rather than
 * drag-and-drop: the workspace renders as a two-column grid, where a drag
 * gesture has no unambiguous meaning, and explicit controls stay usable with
 * larger touch targets and screen readers.
 */
@Composable
fun ArrangeWorkspaceScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val tools = Workspace.tools

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
                IconButtonMorpho("chevron-left", onBack, contentDescription = "Back")
                Spacer(Modifier.width(Space.xs))
                Column(Modifier.weight(1f)) {
                    Text("Arrange workspace",
                        style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Text(
                        if (tools.isEmpty()) "Nothing to arrange yet"
                        else "Move tools up or down to reorder Home",
                        style = MaterialTheme.typography.bodyMedium, color = InkSoft
                    )
                }
            }
            Spacer(Modifier.height(Space.sm))
        }

        if (tools.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(Space.gutter), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MorphoIcon("pdf-reorder-pages", tint = InkFaint, size = 30.dp)
                    Spacer(Modifier.height(Space.md))
                    Text("Your workspace is empty.", color = InkSoft,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Add a few tools first.", color = InkFaint,
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
                itemsIndexed(tools, key = { _, t -> t.id }) { index, t ->
                    ArrangeRow(
                        tool = t,
                        canMoveUp = index > 0,
                        canMoveDown = index < tools.lastIndex,
                        onUp = { Workspace.move(index, index - 1) },
                        onDown = { Workspace.move(index, index + 1) },
                        onRemove = { Workspace.remove(t.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArrangeRow(
    tool: Tool,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    val accent = tool.category.accent
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.xs, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(Shape.chip).background(accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(tool.iconKey, tint = Paper, size = 20.dp) }
        Spacer(Modifier.width(Space.md))
        Text(tool.name, style = MaterialTheme.typography.titleMedium, color = Ink,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(Space.sm))

        // No spacers: each button is a 48dp target around a 36dp circle, so the
        // padding inside the targets already separates them visually.
        StepButton("chevron-up", canMoveUp, onUp, label = "Move " + tool.name + " up")
        StepButton("chevron-down", canMoveDown, onDown, label = "Move " + tool.name + " down")
        StepButton("close", true, onRemove, danger = true,
            label = "Remove " + tool.name + " from your workspace")
    }
}

@Composable
private fun StepButton(
    icon: String,
    enabled: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
    label: String
) {
    val bg = when {
        !enabled -> PaperSunk
        danger -> Ink.copy(alpha = 0.06f)
        else -> CobaltWash
    }
    val fg = when {
        !enabled -> InkFaint.copy(alpha = 0.4f)
        danger -> InkSoft
        else -> Cobalt
    }
    Box(
        Modifier.size(48.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, enabled = enabled,
            role = androidx.compose.ui.semantics.Role.Button, onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(36.dp).clip(Shape.pill).background(bg),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(icon, tint = fg, size = 15.dp, contentDescription = label) }
    }
}
