package cc.devbangs.morpho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MorphoLight = lightColorScheme(
    primary = Cobalt,
    onPrimary = Paper,
    primaryContainer = CobaltWash,
    onPrimaryContainer = Cobalt,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperSunk,
    onSurfaceVariant = InkSoft,
    outline = PaperLine,
    outlineVariant = PaperLine,
)

@Composable
fun MorphoTheme(content: @Composable () -> Unit) {
    // Light-first by design; we ignore system dark for a consistent brand surface.
    MaterialTheme(
        colorScheme = MorphoLight,
        typography = MorphoType,
        content = content
    )
}
