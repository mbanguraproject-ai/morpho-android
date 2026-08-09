package cc.devbangs.morpho

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import cc.devbangs.morpho.ads.ConsentManager
import cc.devbangs.morpho.ads.InterstitialManager
import cc.devbangs.morpho.ui.theme.MorphoTheme
import cc.devbangs.morpho.ui.theme.Paper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
        // Ensure the system gesture bar keeps a contrast scrim so scrolling
        // content never collides with the pill/buttons (API 29+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = true
        }
        // Gather GDPR/CCPA consent first, then preload ads only if allowed
        ConsentManager.gatherConsent(this) {
            if (ConsentManager.canRequestAds) {
                InterstitialManager.preload(this)
            }
        }
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
