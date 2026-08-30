package cc.devbangs.morpho.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolCategory
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.data.Workspace
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

/**
 * Blueprint section 7 - onboarding determines the initial Home layout.
 *
 * Four screens: identity, needs, jobs, ready. The output is a seeded
 * workspace, so Home is personal the first time it is ever opened.
 * "Skip for now" is available throughout; nobody is trapped here.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val needs = remember { mutableStateListOf<ToolCategory>() }
    val chosen = remember { mutableStateListOf<String>() }

    val suggestions = remember(needs.toList()) {
        val cats = if (needs.isEmpty()) ToolCategory.entries.toList() else needs.toList()
        val pool = cats.flatMap { ToolRegistry.byCategory[it].orEmpty() }
        (pool.filter { it.popular } + pool.filter { !it.popular }).take(24)
    }

    fun finish() {
        Workspace.setAll(chosen.toList())
        onDone()
    }

    Column(
        Modifier.fillMaxSize().background(Paper)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = Space.gutter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepDots(step)
            Spacer(Modifier.weight(1f))
            if (step < 3) Text(
                "Skip for now", color = InkFaint, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(Shape.pill).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onDone
                ).padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Crossfade(targetState = step, animationSpec = tween(260), label = "step",
            modifier = Modifier.weight(1f)) { s ->
            when (s) {
                0 -> Identity()
                1 -> Needs(needs)
                2 -> Jobs(suggestions, chosen)
                else -> Ready(chosen.size)
            }
        }

        Column(Modifier.fillMaxWidth().padding(Space.gutter)) {
            PrimaryButton(
                label = when (step) {
                    0 -> "Get started"
                    1 -> if (needs.isEmpty()) "Show me everything" else "Continue"
                    2 -> if (chosen.isEmpty()) "Skip this step" else "Add ${chosen.size} to workspace"
                    else -> "Go to my workspace"
                },
                enabled = true
            ) {
                if (step >= 3) finish() else step++
            }
        }
    }
}

@Composable
private fun Identity() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Space.gutter),
        verticalArrangement = Arrangement.Center
    ) {
        OrganisingTiles()
        Spacer(Modifier.height(Space.xxl))
        Text("Your tools.\nOne workspace.",
            style = MaterialTheme.typography.displaySmall, color = Ink)
        Spacer(Modifier.height(Space.md))
        Text("Convert, edit, compress, create and more — without jumping between websites.",
            style = MaterialTheme.typography.bodyLarge, color = InkSoft)
    }
}

@Composable
private fun Needs(needs: MutableList<ToolCategory>) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = Space.gutter, vertical = Space.md)) {
            Text("What do you usually need to do?",
                style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("Pick as many as you like. This shapes your workspace.",
                style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
            contentPadding = PaddingValues(bottom = Space.lg)
        ) {
            items(ToolCategory.entries.toList()) { c ->
                val on = needs.contains(c)
                Column(
                    Modifier.clip(Shape.card)
                        .background(if (on) c.accent else PaperSunk)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (on) needs.remove(c) else needs.add(c) }
                        .padding(Space.md)
                ) {
                    MorphoIcon("cat-${c.id}", tint = if (on) Paper else c.accent, size = 22.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(c.label, color = if (on) Paper else Ink,
                        style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Jobs(suggestions: List<Tool>, chosen: MutableList<String>) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = Space.gutter, vertical = Space.md)) {
            Text("Pick the tools you'll use",
                style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("You can add or remove tools any time.",
                style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = Space.gutter),
            contentPadding = PaddingValues(bottom = Space.lg)
        ) {
            items(suggestions, key = { it.id }) { t ->
                val on = chosen.contains(t.id)
                Row(
                    Modifier.fillMaxWidth().clip(Shape.tile)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (on) chosen.remove(t.id) else chosen.add(t.id) }
                        .padding(vertical = Space.sm, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(Shape.chip).background(t.category.accent),
                        contentAlignment = Alignment.Center
                    ) { MorphoIcon(t.iconKey, tint = Paper, size = 20.dp) }
                    Spacer(Modifier.width(Space.md))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.short, style = MaterialTheme.typography.bodyMedium, color = InkSoft,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(Space.sm))
                    Box(
                        Modifier.size(28.dp).clip(Shape.pill)
                            .background(if (on) Cobalt else PaperSunk),
                        contentAlignment = Alignment.Center
                    ) {
                        if (on) MorphoIcon("check", tint = Paper, size = 15.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Ready(count: Int) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Space.gutter),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(56.dp).clip(Shape.card).background(Cobalt),
            contentAlignment = Alignment.Center
        ) { MorphoIcon("check", tint = Paper, size = 28.dp) }
        Spacer(Modifier.height(Space.xl))
        Text("Your workspace is ready.",
            style = MaterialTheme.typography.displaySmall, color = Ink)
        Spacer(Modifier.height(Space.md))
        Text(
            if (count == 0) "Nothing added yet — you can build it from Home whenever you like."
            else "Need something else? Explore every tool any time.",
            style = MaterialTheme.typography.bodyLarge, color = InkSoft
        )
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { i ->
            val on = i <= step
            Box(
                Modifier.height(6.dp).width(if (i == step) 20.dp else 6.dp)
                    .clip(Shape.pill)
                    .background(if (on) Cobalt else PaperSunk)
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(Shape.card)
            .background(if (enabled) Cobalt else PaperSunk)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, enabled = enabled, onClick = onClick
            )
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (enabled) Paper else InkFaint,
            style = MaterialTheme.typography.titleMedium)
    }
}

/** Section 7: restrained motion showing tool cards settling into one workspace. */
@Composable
private fun OrganisingTiles() {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val keys = listOf(
        "cat-pdf" to ToolCategory.PDF.accent,
        "cat-image" to ToolCategory.IMAGE.accent,
        "cat-audio" to ToolCategory.AUDIO.accent,
        "cat-developer" to ToolCategory.DEVELOPER.accent
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        keys.forEachIndexed { i, (key, tint) ->
            val p by animateFloatAsState(
                targetValue = if (go) 1f else 0f,
                animationSpec = tween(520, delayMillis = i * 90, easing = EaseOutCubic),
                label = "tile$i"
            )
            Box(
                Modifier.size(52.dp).graphicsLayer {
                    alpha = p
                    translationY = (1f - p) * 26f
                }.clip(Shape.card).background(tint),
                contentAlignment = Alignment.Center
            ) { MorphoIcon(key, tint = Paper, size = 24.dp) }
        }
    }
}
