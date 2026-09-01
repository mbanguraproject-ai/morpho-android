package cc.devbangs.morpho.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cc.devbangs.morpho.data.Stats
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.Eyebrow
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

private val HeroTint = Color(0xFFF0F3FF)
private val Warn = Color(0xFFB4231E)

/**
 * Blueprint sections 46 and 47 - what the usage signals are actually for.
 *
 * With 132 tools, the useful question is not how many there are but which ones
 * earn the next hour of work. Three answers here: what gets used, what fails,
 * and what people looked for and did not find.
 *
 * Everything shown is read from local storage. Nothing is uploaded anywhere,
 * and the screen says so rather than leaving it to be assumed.
 */
@Composable
fun StatsScreen(onBack: () -> Unit, contentPadding: PaddingValues) {
    val tools = Stats.tools()
    val troubled = Stats.troubled()
    val missed = Stats.missedSearches
    val used = tools.count { it.launches > 0 }

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
                    Text("Usage", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Text("Stays on this device", style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft)
                }
            }
            Spacer(Modifier.height(Space.sm))
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = Space.gutter),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + Space.xxl
            )
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk)
                        .padding(Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Stat("Opens", Stats.appOpens.toString(), Modifier.weight(1f))
                    Stat("Tools used", "$used", Modifier.weight(1f))
                    Stat("Of", "${ToolRegistry.all.size}", Modifier.weight(1f))
                }
            }

            if (tools.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = Space.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MorphoIcon("tab-grid", tint = InkFaint, size = 30.dp)
                        Spacer(Modifier.height(Space.md))
                        Text("Nothing recorded yet.", color = InkSoft,
                            style = MaterialTheme.typography.bodyLarge)
                        Text("Open a few tools and this fills in.", color = InkFaint,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (troubled.isNotEmpty()) {
                item { Eyebrow("NEEDS ATTENTION", gutter = false) }
                items(troubled, key = { "t-" + it.toolId }) { s ->
                    val tool = ToolRegistry.byId(s.toolId) ?: return@items
                    val pct = ((s.failureRate ?: 0f) * 100).toInt()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = Space.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(Shape.chip)
                                .background(Warn.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) { MorphoIcon("info", tint = Warn, size = 17.dp) }
                        Spacer(Modifier.width(Space.md))
                        Column(Modifier.weight(1f)) {
                            Text(tool.name, style = MaterialTheme.typography.titleMedium,
                                color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$pct% of runs failed \u00b7 ${s.failures} of " +
                                "${s.failures + s.successes}",
                                style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                        }
                    }
                }
            }

            if (tools.any { it.launches > 0 }) {
                item { Eyebrow("MOST USED", gutter = false) }
                items(tools.filter { it.launches > 0 }.take(12), key = { it.toolId }) { s ->
                    val tool = ToolRegistry.byId(s.toolId) ?: return@items
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = Space.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(Shape.chip)
                                .background(tool.category.accent),
                            contentAlignment = Alignment.Center
                        ) { MorphoIcon(tool.iconKey, tint = Paper, size = 18.dp) }
                        Spacer(Modifier.width(Space.md))
                        Column(Modifier.weight(1f)) {
                            Text(tool.name, style = MaterialTheme.typography.titleMedium,
                                color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                buildString {
                                    append("${s.launches} opens")
                                    if (s.successes > 0) append(" \u00b7 ${s.successes} saved")
                                    if (s.adds > 0) append(" \u00b7 in workspace")
                                },
                                style = MaterialTheme.typography.bodyMedium, color = InkSoft
                            )
                        }
                    }
                }
            }

            if (missed.isNotEmpty()) {
                item { Eyebrow("SEARCHED, NOT FOUND", gutter = false) }
                item {
                    Text(
                        "What people expected Morpho to have. The clearest list " +
                            "of what to build next.",
                        style = MaterialTheme.typography.bodyMedium, color = InkFaint,
                        modifier = Modifier.padding(bottom = Space.sm)
                    )
                }
                items(missed, key = { "m-$it" }) { q ->
                    Text(
                        q,
                        style = MaterialTheme.typography.bodyLarge, color = Ink,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)
                    )
                }
            }

            item {
                Spacer(Modifier.height(Space.xl))
                Box(
                    Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { Stats.clear() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Clear usage data", color = InkSoft, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Ink)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
    }
}
