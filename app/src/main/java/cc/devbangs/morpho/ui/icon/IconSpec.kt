package cc.devbangs.morpho.ui.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MorphoIcon now renders verified Phosphor Bold vectors via ToolIcons.
 * The old hand-drawn Canvas glyph atlas is retired.
 * `strokeWidth` is kept in the signature for call-site compatibility (ignored).
 */
@Composable
fun MorphoIcon(
    key: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Float = 1.9f,
    /**
     * Blueprint section 39 - screen-reader labels.
     * Null is correct for a decorative icon sitting beside its own text label;
     * an icon that is the only content of a control must pass one.
     */
    contentDescription: String? = null
) {
    Icon(
        imageVector = ToolIcons.of(key),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}
