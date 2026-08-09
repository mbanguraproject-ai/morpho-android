package cc.devbangs.morpho.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Loads and shows interstitial ads. Pre-loads one so it's ready instantly,
 * then reloads after each show. Respects AdState policy + Plus gating.
 */
object InterstitialManager {
    private var ad: InterstitialAd? = null
    private var loading = false

    /** Pre-load an interstitial so it's ready when needed. */
    fun preload(ctx: Context) {
        if (!AdState.adsEnabled() || !ConsentManager.canRequestAds || ad != null || loading) return
        loading = true
        InterstitialAd.load(
            ctx, AdState.TEST_INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) { ad = loaded; loading = false }
                override fun onAdFailedToLoad(error: LoadAdError) { ad = null; loading = false }
            }
        )
    }

    /**
     * Show the interstitial if one is ready and policy allows.
     * Reloads a fresh one afterward. Safe to call anytime.
     */
    fun maybeShow(activity: Activity, onDone: () -> Unit = {}) {
        val current = ad
        if (!AdState.adsEnabled() || current == null) { onDone(); return }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null; preload(activity); onDone()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                ad = null; preload(activity); onDone()
            }
        }
        current.show(activity)
    }
}
