package cc.devbangs.morpho.ui.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MorphoIcon: a single-color, stroke-based custom glyph drawn on a 24x24 grid.
 * All app iconography is drawn here — nothing from Material/lucide.
 */
@Composable
fun MorphoIcon(
    key: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Float = 1.9f
) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / 24f          // unit scale
        val sw = strokeWidth * u
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawGlyph(key, tint, u, stroke)
    }
}


// ---- geometry helpers on a 24-grid ----
internal fun DrawScope.gLine(x1: Float, y1: Float, x2: Float, y2: Float, c: Color, u: Float, s: Stroke) =
    drawLine(c, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), s.width, s.cap)

internal fun DrawScope.gRect(x: Float, y: Float, w: Float, h: Float, c: Color, u: Float, s: Stroke, r: Float = 2f) =
    drawRoundRect(
        color = c,
        topLeft = Offset(x * u, y * u),
        size = Size(w * u, h * u),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * u, r * u),
        style = s
    )

internal fun DrawScope.gCircle(cx: Float, cy: Float, rad: Float, c: Color, u: Float, s: Stroke) =
    drawCircle(c, rad * u, Offset(cx * u, cy * u), style = s)

internal fun DrawScope.gDot(cx: Float, cy: Float, rad: Float, c: Color, u: Float) =
    drawCircle(c, rad * u, Offset(cx * u, cy * u))

internal fun DrawScope.gPath(c: Color, u: Float, s: Stroke, build: Path.() -> Unit) {
    val p = Path().apply(build)
    drawPath(p, c, style = s)
}

internal fun Path.m(x: Float, y: Float, u: Float) = moveTo(x * u, y * u)
internal fun Path.l(x: Float, y: Float, u: Float) = lineTo(x * u, y * u)
