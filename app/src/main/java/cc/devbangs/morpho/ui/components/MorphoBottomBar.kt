package cc.devbangs.morpho.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Motion
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials

data class BottomTab(val key: String, val label: String, val glyph: String)

private val TABS = listOf(
    BottomTab("home", "Home", "tab-home"),
    BottomTab("files", "Files", "tab-files"),
    BottomTab("categories", "Tools", "tab-grid"),
    BottomTab("search", "Search", "tab-search"),
)

@Composable
fun MorphoBottomBar(
    hazeState: HazeState,
    current: String,
    onSelect: (String) -> Unit,
    onMaster: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Docked, not floating: the surface runs edge to edge and continues behind
    // Android's navigation bar, so there is no gap or floating pill between
    // Morpho's bar and the system one. The blur is applied to the whole
    // column, so it also covers the area behind the system bar.
    Column(
        modifier
            .fillMaxWidth()
            // The same material as the Home and Tools headers. A custom, nearly
            // opaque tint here made the bar read as a solid slab while the top
            // of every screen was frosted.
            .hazeChild(hazeState, style = HazeMaterials.ultraThin(Paper))
    ) {
        // A hairline reads as a seam; a drop shadow only makes sense on a
        // surface that floats above the content.
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperLine))
        Row(
            // The centre button is taller than the tabs, so the row needs its own
            // breathing room above the system bar rather than sitting on it.
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Two tabs, the master action, two tabs: five even slots with the
            // primary action dead centre.
            TabItem(TABS[0], TABS[0].key == current) { onSelect(TABS[0].key) }
            TabItem(TABS[1], TABS[1].key == current) { onSelect(TABS[1].key) }
            MasterSlot(onMaster)
            TabItem(TABS[2], TABS[2].key == current) { onSelect(TABS[2].key) }
            TabItem(TABS[3], TABS[3].key == current) { onSelect(TABS[3].key) }
        }
        // Holds the tabs clear of the system bar while the surface paints under it.
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )
    }
}

/** The master sheet entry - sections 14 and 17. Sits in the bar rather than
 *  floating above it, so the docked edge stays a clean line. */
@Composable
private fun RowScope.MasterSlot(onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).heightIn(min = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        val press = remember { MutableInteractionSource() }
        val pressed by press.collectIsPressedAsState()
        Box(
            // Raised, and carrying the Morpho mark rather than a sparkle: the
            // sparkle glyph already stands for the generator category and the
            // AI tools, so it read as one more tool instead of the app itself.
            Modifier.size(52.dp)
                // Not morphLift: that tints its shadow with the accent colour and
                // paints a white surface underneath, so a cobalt circle got a
                // cobalt halo bleeding past its edge. A neutral ink shadow gives
                // the lift without the colour spill.
                .shadow(
                    elevation = if (pressed) 4.dp else 10.dp,
                    shape = Shape.pill,
                    clip = false,
                    ambientColor = Ink.copy(alpha = 0.18f),
                    spotColor = Ink.copy(alpha = 0.30f)
                )
                .clip(Shape.pill)
                .background(Cobalt)
                .clickable(
                    interactionSource = press,
                    indication = null,
                    role = androidx.compose.ui.semantics.Role.Button,
                    onClick = onClick
                )
                .semantics { contentDescription = "What do you need?" },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(cc.devbangs.morpho.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        if (selected) Cobalt else InkFaint,
        animationSpec = androidx.compose.animation.core.tween(Motion.d(Motion.SMALL)),
        label = "tint"
    )
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = 48.dp)   // section 39 minimum
            .clip(Shape.chip)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = androidx.compose.ui.semantics.Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MorphoIcon(tab.glyph, tint = tint, size = 25.dp)
        Spacer(Modifier.height(3.dp))
        Text(
            tab.label,
            color = tint,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
