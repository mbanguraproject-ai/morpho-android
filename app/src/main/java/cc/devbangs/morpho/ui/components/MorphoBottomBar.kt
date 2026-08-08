package cc.devbangs.morpho.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
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
    // Outer column: transparent, hosts the floating pill + consumes nav inset.
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = Shape.card,
                    clip = false,
                    ambientColor = Ink.copy(alpha = 0.10f),
                    spotColor = Cobalt.copy(alpha = 0.18f)
                )
                .clip(Shape.card)
                .background(Paper)
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TABS.forEach { tab -> TabItem(tab, tab.key == current) { onSelect(tab.key) } }
        }
        // float above the gesture / nav bar
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun RowScope.TabItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) Cobalt else InkFaint, label = "tint")
    Column(
        Modifier
            .weight(1f)
            .clip(Shape.chip)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MorphoIcon(tab.glyph, tint = tint, size = 22.dp)
        Spacer(Modifier.height(3.dp))
        Text(
            tab.label,
            color = tint,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
