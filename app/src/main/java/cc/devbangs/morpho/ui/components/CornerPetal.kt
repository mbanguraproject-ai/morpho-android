package cc.devbangs.morpho.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation

/**
 * Morpho signature: the twin-petal corner mark. Born from a clip accident,
 * made deliberate. Two offset soft circles intersected into a leaf pair,
 * tucked into the top-right corner in the surface's accent color.
 * Apply AFTER a clip/clipToBounds so it reads as tucked into the card.
 */
fun Modifier.cornerPetal(
    accent: Color,
    alpha: Float = 0.10f
): Modifier = this.drawBehind {
    val w = size.width
    fun petal(cx: Float, cy: Float, r: Float): Path {
        val a = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx, cy), r)) }
        val b = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(Offset(cx - r * 0.62f, cy + r * 0.62f), r))
        }
        return Path().apply { op(a, b, PathOperation.Intersect) }
    }
    // two stacked petals of slightly different size = the twin-leaf look
    drawPath(petal(w - 6f, 6f, 30f), accent.copy(alpha = alpha))
    drawPath(petal(w - 2f, 2f, 46f), accent.copy(alpha = alpha * 0.7f))
}
