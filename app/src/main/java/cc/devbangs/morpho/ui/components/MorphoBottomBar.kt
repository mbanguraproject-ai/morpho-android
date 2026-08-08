package cc.devbangs.morpho.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

data class BottomTab(val key: String, val label: String, val glyph: String)

private val TABS = listOf(
    BottomTab("home", "Home", "tab-home"),
    BottomTab("categories", "Tools", "tab-grid"),
    BottomTab("search", "Search", "tab-search"),
)

@Composable
fun MorphoBottomBar(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Paper)
            .drawBehind {
                // top hairline
                drawLine(
                    color = PaperLine,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f
                )
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TABS.forEach { tab ->
                val selected = tab.key == current
                TabItem(tab, selected) { onSelect(tab.key) }
            }
        }
        // consume the system nav-bar inset so content never clips
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Paper)
        )
    }
}

@Composable
private fun RowScope.TabItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) Cobalt else InkFaint, label = "tint")
    val lift by animateFloatAsState(if (selected) 1f else 0f, label = "lift")
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MorphoIcon(
            key = tab.glyph,
            tint = tint,
            size = 23.dp,
            strokeWidth = if (selected) 2.1f else 1.9f
        )
        Spacer(Modifier.height(3.dp))
        Text(
            tab.label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
