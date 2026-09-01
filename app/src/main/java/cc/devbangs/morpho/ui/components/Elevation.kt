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
import cc.devbangs.morpho.ui.theme.Ink
import cc.devbangs.morpho.ui.theme.Paper
import cc.devbangs.morpho.ui.theme.PaperLine

/**
 * Signature elevation: white surface, a grounded shadow, and an edge that
 * catches light at the top and defines the card at the bottom.
 *
 * The surface is [Paper] and so is every page background, so the card and the
 * page are the same colour - separation has to come entirely from the shadow
 * and the edge. Previously neither did the job: the shadow was tinted with the
 * accent at low alpha, which reads as a faint colour haze rather than depth on
 * white, and the border was a white highlight, invisible against a white page.
 *
 * [accent] still tints the lift, but only slightly, over a neutral base.
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
        // Ink, not accent: a shadow is an absence of light, not a colour.
        ambientColor = Ink.copy(alpha = 0.18f),
        spotColor = Ink.copy(alpha = 0.26f)
    )
    .background(Paper, shape)
    // Light catch at the very top, a real hairline everywhere else, so the
    // card has a defined edge even where the shadow falls away.
    .border(1.dp, Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.9f),
        0.18f to PaperLine,
        1f to PaperLine
    ), shape)
    .clip(shape)
