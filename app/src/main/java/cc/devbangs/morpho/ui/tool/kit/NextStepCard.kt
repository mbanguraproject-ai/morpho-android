package cc.devbangs.morpho.ui.tool.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import cc.devbangs.morpho.workflow.NextStep

/**
 * Renders "what's next" suggestion cards after a tool produces output.
 * Tapping a card hands the file to the bus and opens the next tool.
 */
@Composable
fun NextStepSuggestions(
    steps: List<NextStep>,
    onPick: (NextStep) -> Unit
) {
    if (steps.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = Space.xl)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Space.md)) {
            MorphoIcon("arrow-right", tint = InkFaint, size = 15.dp)
            Spacer(Modifier.width(6.dp))
            Text("WHAT'S NEXT", color = InkFaint, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            steps.forEach { step ->
                val tool = ToolRegistry.byId(step.toolId)
                val accent = tool?.category?.accent ?: Cobalt
                Row(
                    Modifier.fillMaxWidth().clip(Shape.card).background(Paper)
                        .border(1.5.dp, accent.copy(alpha = 0.25f), Shape.card)
                        .clickable { onPick(step) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(Shape.chip).background(accent),
                        contentAlignment = Alignment.Center
                    ) { if (tool != null) MorphoIcon(tool.iconKey, tint = Paper, size = 20.dp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(step.label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(step.reason, color = InkSoft, fontSize = 12.sp)
                    }
                    MorphoIcon("arrow-right", tint = accent, size = 18.dp)
                }
            }
        }
    }
}
