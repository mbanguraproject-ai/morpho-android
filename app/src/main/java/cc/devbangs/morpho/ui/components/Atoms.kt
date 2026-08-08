package cc.devbangs.morpho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.theme.*

/** Status dot: cobalt = works offline, hollow = server/coming-soon. */
@Composable
fun StatusDot(offline: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(7.dp)
            .clip(Shape.pill)
            .background(if (offline) Cobalt else Color.Transparent)
    ) {
        if (!offline) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(Shape.pill)
                    .background(Color.Transparent)
            )
        }
    }
}

/** Small pill label used for "Offline" / "Soon". */
@Composable
fun MetaChip(text: String, accent: Color = InkSoft, filled: Boolean = false) {
    Box(
        Modifier
            .clip(Shape.chip)
            .background(if (filled) CobaltWash else PaperSunk)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            color = if (filled) Cobalt else accent,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}

/** Section header with optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}
