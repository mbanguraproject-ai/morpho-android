package cc.devbangs.morpho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.devbangs.morpho.ui.MorphoApp
import cc.devbangs.morpho.ui.theme.MorphoTheme
import cc.devbangs.morpho.ui.theme.Paper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent system bars, dark icons (light bars). Applies on all API levels.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            MorphoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Paper),
                    color = Paper
                ) {
                    MorphoApp()
                }
            }
        }
    }
}
