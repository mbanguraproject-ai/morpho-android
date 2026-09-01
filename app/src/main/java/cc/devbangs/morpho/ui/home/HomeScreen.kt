package cc.devbangs.morpho.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cc.devbangs.morpho.R
import cc.devbangs.morpho.ads.AdState
import cc.devbangs.morpho.core.Motion
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.data.Workspace
import cc.devbangs.morpho.ui.components.Eyebrow
import cc.devbangs.morpho.ui.components.cornerPetal
import cc.devbangs.morpho.ui.components.morphLift
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import java.util.Calendar

/**
 * Blueprint section 5 - Home is "my Morpho", not the catalog.
 *
 * Workspace, Recent, and a way through to the full tool universe. Discovery
 * lives in the Tools tab; search lives in the Search tab. Home stays personal.
 */
@Composable
fun HomeScreen(
    onOpenTool: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAddTools: () -> Unit,
    onArrange: () -> Unit,
    onExploreTools: () -> Unit,
    contentPadding: PaddingValues
) {
    val hazeState = remember { HazeState() }
    val workspace = Workspace.tools
    val recent = Workspace.recent

    Box(Modifier.fillMaxSize().background(Paper)) {
        LazyColumn(
            Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Space.xxl)
        ) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(60.dp))
                Text(
                    greeting(),
                    style = MaterialTheme.typography.displaySmall, color = Ink,
                    modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.lg)
                )
                Text(
                    "Let's get your work done",
                    style = MaterialTheme.typography.bodyLarge, color = InkSoft,
                    modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, top = 2.dp)
                )
            }

            if (workspace.isEmpty()) {
                item { EmptyWorkspace(onOpenAddTools) }
            } else {
                item { Eyebrow("YOUR WORKSPACE", action = "Manage", onAction = onArrange) }
                // The add cell is the last slot in the grid rather than a link
                // underneath it, so adding reads as part of the workspace.
                val slots = (0..workspace.size).toList()
                itemsIndexed(slots.chunked(2)) { idx, row ->
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = Space.gutter, vertical = 5.dp)
                            .height(IntrinsicSize.Min)
                            .reveal(idx),
                        horizontalArrangement = Arrangement.spacedBy(Space.md)
                    ) {
                        row.forEach { slot ->
                            val cell = Modifier.weight(1f).fillMaxHeight()
                            if (slot < workspace.size) {
                                val t = workspace[slot]
                                WorkspaceCard(t, { onOpenTool(t.id) }, cell)
                            } else {
                                AddToolsCell(onOpenAddTools, cell)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (recent.isNotEmpty()) {
                item { Eyebrow("RECENT") }
                itemsIndexed(recent) { idx, t ->
                    RecentRow(t) { onOpenTool(t.id) }
                }
            }

            item { ToolsBanner(onExploreTools) }
        }

        Row(
            Modifier.fillMaxWidth().zIndex(1f)
                .hazeChild(hazeState, style = HazeMaterials.ultraThin(Paper))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = Space.gutter, end = Space.gutter, top = Space.sm, bottom = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WingsMark()
            Spacer(Modifier.width(9.dp))
            Column {
                Text("Morpho", style = MaterialTheme.typography.titleLarge, color = Ink,
                    fontWeight = FontWeight.Bold)
                Text("Files, transformed", style = MaterialTheme.typography.bodySmall,
                    color = InkFaint, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            PlanPill()
            Spacer(Modifier.width(8.dp))
            SettingsButton(onOpenSettings)
        }
    }
}

private fun greeting(): String =
    when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

/** Blueprint section 24 - a guided beginning, not a dead end. */
@Composable
private fun EmptyWorkspace(onAdd: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    Column(
        Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, top = Space.xl),
    ) {
        Text("Build your workspace", style = MaterialTheme.typography.headlineSmall, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text("Choose the tools you use most.", style = MaterialTheme.typography.bodyLarge, color = InkSoft)
        Spacer(Modifier.height(Space.xl))
        Row(
            Modifier.fillMaxWidth()
                .morphLift(Shape.card, elevation = 10.dp, pressed = pressed, accent = Cobalt)
                .clickable(interactionSource = i, indication = null, onClick = onAdd)
                .padding(horizontal = Space.lg, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(Shape.pill).background(Cobalt),
                contentAlignment = Alignment.Center
            ) { MorphoIcon("plus", tint = Paper, size = 17.dp) }
            Spacer(Modifier.width(Space.md))
            Text("Add your first tool", style = MaterialTheme.typography.titleMedium, color = Ink)
        }
    }
}

@Composable
private fun WorkspaceCard(t: Tool, onClick: () -> Unit, modifier: Modifier) {
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
        Box(
            Modifier.size(42.dp).clip(Shape.chip).background(accent),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(t.iconKey, tint = Paper, size = 22.dp) }
        Spacer(Modifier.height(16.dp))
        Text(t.name, style = MaterialTheme.typography.titleMedium, color = Ink,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 40.dp))
        Text(t.short, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RecentRow(t: Tool, onClick: () -> Unit) {
    val accent = t.category.accent
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.gutter)
            .clip(Shape.tile)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(vertical = Space.sm, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(Shape.chip).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { MorphoIcon(t.iconKey, tint = accent, size = 19.dp) }
        Spacer(Modifier.width(Space.md))
        Text(t.name, style = MaterialTheme.typography.titleMedium, color = Ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        MorphoIcon("chevron-right", tint = InkFaint, size = 15.dp)
    }
}

/** The one route from Home into the full catalog. Count is live. */
@Composable
private fun ToolsBanner(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = Space.gutter, vertical = Space.xl)
            .clip(Shape.card).background(CobaltWash)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(Shape.chip).background(Cobalt),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("tab-grid", tint = Paper, size = 19.dp) }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text("${ToolRegistry.all.size} tools at your fingertips",
                style = MaterialTheme.typography.titleMedium, color = Ink)
            Text("Explore the complete collection",
                style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        }
        MorphoIcon("chevron-right", tint = Cobalt, size = 16.dp)
    }
}

/** Dashed slot that sits in the workspace grid as its final cell. */
@Composable
private fun AddToolsCell(onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clip(Shape.card)
            .border(1.dp, Cobalt.copy(alpha = 0.35f), Shape.card)
            .background(CobaltWash.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick
            )
            .padding(15.dp)
    ) {
        Box(
            Modifier.size(42.dp).clip(Shape.chip).background(CobaltWash),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("plus", tint = Cobalt, size = 22.dp) }
        Spacer(Modifier.height(16.dp))
        Text("Add tools", style = MaterialTheme.typography.titleMedium, color = Cobalt,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 40.dp))
        Text("Customize your workspace",
            style = MaterialTheme.typography.bodyMedium, color = InkSoft,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    // Section 39: the painted surface stays 38dp; the touch target is 48dp.
    Box(
        Modifier.size(48.dp).clickable(
            interactionSource = i, indication = null,
            role = androidx.compose.ui.semantics.Role.Button, onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(38.dp).morphLift(Shape.chip, elevation = 4.dp, pressed = pressed),
            contentAlignment = Alignment.Center
        ) {
            MorphoIcon("settings", tint = InkSoft, size = 19.dp,
                contentDescription = "Settings")
        }
    }
}

/** Staggered rise + fade-in reveal for list items. */
@Composable
private fun Modifier.reveal(indexKey: Int): Modifier {
    var shown by remember(indexKey) { mutableStateOf(false) }
    LaunchedEffect(indexKey) { shown = true }
    val p by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = Motion.d(Motion.COMPONENT),
            delayMillis = Motion.stagger(indexKey),
            easing = Motion.Enter
        ),
        label = "reveal"
    )
    return this.graphicsLayer { translationY = (1f - p) * 34f; this.alpha = p }
}

/** Cobalt wings mark that flaps once on home entry, then rests. */
@Composable
private fun WingsMark() {
    var spread by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { spread = true }
    val wingSpread by animateFloatAsState(
        targetValue = if (spread) 1f else 0.55f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow),
        label = "wingSpread"
    )
    Box(
        Modifier.size(32.dp).clip(Shape.chip).background(Cobalt),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(30.dp).graphicsLayer {
                scaleX = wingSpread
                scaleY = 0.9f + (wingSpread * 0.1f)
            }
        )
    }
}

/** Small non-intrusive plan indicator: "Free" or "Plus". */
@Composable
private fun PlanPill() {
    val isPlus = AdState.isPlus.value
    val bg = if (isPlus) Cobalt else PaperSunk
    val fg = if (isPlus) Paper else InkSoft
    Box(
        Modifier.clip(Shape.pill).background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(if (isPlus) "Plus" else "Free", color = fg, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold)
    }
}
