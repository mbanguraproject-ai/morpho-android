package cc.devbangs.morpho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.devbangs.morpho.ui.theme.Paper

/**
 * Signature elevation: white surface + tinted lift + a 1px top inset highlight,
 * so cards read as physical objects catching light (the mockup's depth).
 * [accent] tints the shadow; defaults to cobalt.
 */
fun Modifier.morphLift(
    shape: Shape,
    elevation: Dp = 10.dp,
    pressed: Boolean = false,
    accent: Color = Color(0xFF1A46E5)
): Modifier = this
    .shadow(
        elevation = if (pressed) 3.dp else elevation,
        shape = shape,
        clip = false,
        ambientColor = accent.copy(alpha = 0.14f),
        spotColor = accent.copy(alpha = 0.22f)
    )
    .background(Paper, shape)
    // faint top highlight = the inset 0 1px 0 rgba(255,255,255,.9)
    .border(1.dp, Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.85f),
        0.5f to Color.Transparent
    ), shape)
    .clip(shape)
