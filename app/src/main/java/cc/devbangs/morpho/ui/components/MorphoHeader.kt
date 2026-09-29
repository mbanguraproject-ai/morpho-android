package cc.devbangs.morpho.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cc.devbangs.morpho.R
import cc.devbangs.morpho.ads.AdState
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials

/** Height the header occupies below the status bar, for list top padding. */
val HeaderHeight = 60.dp

/**
 * The one header Morpho wears.
 *
 * Home, Files, Tools and Search used to each print their own title and their
 * own one-liner, so four tabs of the same app looked like four apps. This is
 * the home header, lifted out whole, so every tab opens on the same mark and
 * the same plan pill and the same way into settings.
 *
 * [subtitle] exists only for a screen that has something live to say in that
 * slot - Files says "Loading..." while it reads storage. Left null it reads
 * the brand line, which is the normal case.
 *
 * [hazeState] is null on a screen where the header sits in normal flow with
 * nothing scrolling underneath it - Search. There it paints solid Paper,
 * because frosting a surface with nothing behind it just makes it vanish.
 */
@Composable
fun MorphoHeader(
    hazeState: HazeState?,
    onOpenSettings: () -> Unit,
    subtitle: String? = null
) {
    Row(
        Modifier.fillMaxWidth().zIndex(1f)
            .let {
                if (hazeState != null)
                    it.hazeChild(hazeState, style = HazeMaterials.ultraThin(Paper))
                else it
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Space.gutter, end = Space.gutter, top = Space.sm, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WingsMark()
        Spacer(Modifier.width(9.dp))
        Column {
            Text("Morpho", style = MaterialTheme.typography.titleLarge, color = Ink,
                fontWeight = FontWeight.Bold)
            Text(subtitle ?: "Files, transformed",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        PlanPill()
        Spacer(Modifier.width(8.dp))
        SettingsButton(onOpenSettings)
    }
}

/** Cobalt wings mark that flaps once on entry, then rests. */
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
        Modifier.size(34.dp).clip(Shape.chip).background(Cobalt),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(34.dp).graphicsLayer {
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
