package cc.devbangs.morpho.ui.workspace

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cc.devbangs.morpho.core.Motion
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.Workspace
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import kotlin.math.roundToInt

private val HeroTint = Color(0xFFF0F3FF)
private val ROW_HEIGHT = 64.dp

/**
 * Blueprint section 25 - reorder and remove workspace tools.
 *
 * Drag a handle to move a tool. The previous version used up and down arrow
 * buttons, which needed one tap per position and gave no sense of where an
 * item was going.
 *
 * A plain Column rather than a LazyColumn: the workspace is a handful of
 * tools, and a fixed row height makes the drag arithmetic exact - the target
 * index is just the offset divided by the row height. Reordering happens live
 * as the drag crosses each boundary, so the list under the finger is always
 * the list that will be saved.
 */
@Composable
fun ArrangeWorkspaceScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val tools = Workspace.tools
    val density = LocalDensity.current
    val rowPx = with(density) { ROW_HEIGHT.toPx() }
    val haptics = LocalHapticFeedback.current

    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

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
                    Text("Manage workspace",
                        style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Text(
                        if (tools.isEmpty()) "Nothing to arrange yet"
                        else "Drag the handle to reorder",
                        style = MaterialTheme.typography.bodyMedium, color = InkSoft
                    )
                }
            }
            Spacer(Modifier.height(Space.sm))
        }

        if (tools.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(Space.gutter), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MorphoIcon("drag", tint = InkFaint, size = 30.dp)
                    Spacer(Modifier.height(Space.md))
                    Text("Your workspace is empty.", color = InkSoft,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Add a few tools first.", color = InkFaint,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            tools.forEachIndexed { index, tool ->
                val dragging = index == draggingIndex
                ArrangeRow(
                    tool = tool,
                    index = index,
                    dragging = dragging,
                    dragOffset = if (dragging) dragOffset else 0f,
                    hint = index == 0,
                    onRemove = { Workspace.remove(tool.id) },
                    onDragStart = {
                        draggingIndex = index
                        dragOffset = 0f
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { dy ->
                        dragOffset += dy
                        // Swap as soon as the finger passes the midpoint of the
                        // neighbouring row, then rebase so the item stays under
                        // the finger.
                        val shift = (dragOffset / rowPx).roundToInt()
                        val target = draggingIndex + shift
                        if (shift != 0 && target in Workspace.toolIds.indices) {
                            Workspace.move(draggingIndex, target)
                            dragOffset -= shift * rowPx
                            draggingIndex = target
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragEnd = {
                        draggingIndex = -1
                        dragOffset = 0f
                    }
                )
            }
            Spacer(
                Modifier.height(contentPadding.calculateBottomPadding() + Space.xxl)
            )
        }
    }
}

@Composable
private fun ArrangeRow(
    tool: Tool,
    index: Int,
    dragging: Boolean,
    dragOffset: Float,
    hint: Boolean,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val accent = tool.category.accent

    // On arrival the first row lifts and settles once, which shows that rows
    // move vertically without needing an illustration of a hand.
    val nudge = remember { Animatable(0f) }
    LaunchedEffect(hint) {
        if (hint) {
            kotlinx.coroutines.delay(Motion.d(Motion.PAGE).toLong() + 120)
            nudge.animateTo(-10f, tween(Motion.d(220)))
            nudge.animateTo(6f, tween(Motion.d(200)))
            nudge.animateTo(0f, tween(Motion.d(180)))
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (dragging) dragOffset else nudge.value
                scaleX = if (dragging) 1.02f else 1f
                scaleY = if (dragging) 1.02f else 1f
            }
            .then(
                if (dragging) Modifier
                    .shadow(12.dp, Shape.card, clip = false,
                        ambientColor = Ink.copy(alpha = 0.20f),
                        spotColor = Ink.copy(alpha = 0.28f))
                    .clip(Shape.card)
                    .background(Paper)
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(Shape.chip).background(accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(tool.iconKey, tint = Paper, size = 20.dp) }
        Spacer(Modifier.width(Space.md))
        Text(tool.name, style = MaterialTheme.typography.titleMedium, color = Ink,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))

        Box(
            Modifier.size(48.dp).clip(Shape.pill)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = androidx.compose.ui.semantics.Role.Button,
                    onClick = onRemove
                ),
            contentAlignment = Alignment.Center
        ) {
            MorphoIcon("close", tint = InkSoft, size = 16.dp,
                contentDescription = "Remove " + tool.name + " from your workspace")
        }

        Box(
            Modifier.size(48.dp)
                .pointerInput(index) {
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount.y)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            MorphoIcon("drag", tint = if (dragging) Cobalt else InkFaint, size = 20.dp,
                contentDescription = "Drag to reorder " + tool.name)
        }
    }
}
