package cc.devbangs.morpho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.devbangs.morpho.ui.theme.Cobalt
import cc.devbangs.morpho.ui.theme.Paper

/**
 * Signature elevation: a tight, cobalt-tinted lift used ONLY on interactive
 * surfaces. Grey drop-shadows read generic; a faint brand-tinted lift reads
 * intentional. This is the one place depth is allowed.
 */
fun Modifier.morphLift(
    shape: Shape,
    elevation: Dp = 10.dp,
    pressed: Boolean = false
): Modifier = this
    .shadow(
        elevation = if (pressed) 3.dp else elevation,
        shape = shape,
        clip = false,
        ambientColor = Cobalt.copy(alpha = 0.10f),
        spotColor = Cobalt.copy(alpha = 0.16f)
    )
    .background(Paper, shape)
    .clip(shape)
