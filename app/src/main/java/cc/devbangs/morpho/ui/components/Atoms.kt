package cc.devbangs.morpho.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.theme.*

/**
 * The section label above a list of tools: 11sp bold, tracked, InkFaint,
 * with an optional trailing action.
 *
 * Every screen used to hand-roll this, which is how one role ended up with
 * three different letter-spacings across the app. This is the single
 * implementation; screens should not draw their own.
 *
 * [gutter] adds screen-edge padding. Pass false inside a container that
 * already applies the gutter itself.
 */
@Composable
fun Eyebrow(
    text: String,
    gutter: Boolean = true,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(
            start = if (gutter) Space.gutter else 0.dp,
            end = if (gutter) Space.gutter else 0.dp,
            top = Space.lg,
            bottom = Space.xs
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = InkFaint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            // Lets a screen reader jump between sections instead of reading
            // every tool in a 32-tool category to reach the next group.
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (action != null && onAction != null) Text(
            action,
            color = Cobalt,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(Shape.pill).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAction
            ).padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
