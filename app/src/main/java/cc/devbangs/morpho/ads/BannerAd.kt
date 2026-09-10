package cc.devbangs.morpho.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cc.devbangs.morpho.ui.theme.Paper
import cc.devbangs.morpho.ui.theme.PaperLine
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * How much vertical room the anchored banner is taking right now.
 *
 * Zero for Plus users and whenever nothing fills, so no space is reserved
 * unless an ad is genuinely on screen. Screen content reads this to pad
 * itself; without that the last row of every list would sit behind the ad,
 * which is both a layout bug and the setup for accidental clicks.
 */
object BannerSlot {
    var height by mutableStateOf(0.dp)
        internal set
}

/** Clear space between the ad and the navigation bar's tap targets. */
private val BANNER_GAP: Dp = 10.dp

/**
 * Anchored adaptive banner, sat above the bottom bar.
 *
 * Adaptive rather than the deprecated fixed size: the height is chosen for
 * the device and generally fills better.
 *
 * The gap below it is deliberate. An ad flush against navigation is what turns
 * ordinary mis-taps into invalid traffic, and invalid traffic is an account
 * problem rather than a revenue dip.
 *
 * Nothing is drawn until an ad actually loads, so a no-fill leaves the layout
 * exactly as it was rather than showing an empty band.
 */
@Composable
fun MorphoBanner(modifier: Modifier = Modifier) {
    val consentOk = ConsentManager.canRequestAdsState.value
    val show = AdState.adsEnabled() && consentOk
    if (!show) {
        DisposableEffect(Unit) {
            BannerSlot.height = 0.dp
            onDispose { BannerSlot.height = 0.dp }
        }
        return
    }

    val ctx = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp)
    }
    var loaded by remember(adSize) { mutableStateOf(false) }

    val adView = remember(adSize) {
        AdView(ctx).apply {
            adUnitId = AdState.BANNER_UNIT
            setAdSize(adSize)
        }
    }

    DisposableEffect(adView) {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("MorphoAds", "Banner LOADED h=${adSize.height}dp")
                loaded = true
                BannerSlot.height = adSize.height.dp + BANNER_GAP + 1.dp
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("MorphoAds", "Banner FAILED: code=${error.code} msg=${error.message}")
                loaded = false
                BannerSlot.height = 0.dp
            }
        }
        adView.loadAd(AdRequest.Builder().build())
        onDispose {
            BannerSlot.height = 0.dp
            adView.destroy()
        }
    }

    // A banner left running while the app is backgrounded still counts against
    // the impression, so it is paused with the lifecycle.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    if (loaded) {
        Column(modifier.fillMaxWidth().background(Paper)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(PaperLine))
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth().height(adSize.height.dp)
            )
            Spacer(Modifier.height(BANNER_GAP))
        }
    }
}
