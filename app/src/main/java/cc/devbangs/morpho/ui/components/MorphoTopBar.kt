package cc.devbangs.morpho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

@Composable
fun MorphoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    hairline: Boolean = true
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Paper)
            .drawBehind {
                if (hairline) drawLine(
                    PaperLine, Offset(0f, size.height), Offset(size.width, size.height), 1f
                )
            }
    ) {
        // status-bar inset spacer
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButtonMorpho("chevron-left", onBack)
                Spacer(Modifier.width(Space.xs))
            } else {
                Spacer(Modifier.width(Space.sm))
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
            Spacer(Modifier.width(Space.xs))
        }
    }
}

@Composable
fun IconButtonMorpho(glyph: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color = Ink) {
    Box(
        Modifier
            .size(42.dp)
            .clip(Shape.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        MorphoIcon(glyph, tint = tint, size = 22.dp, strokeWidth = 2f)
    }
}
