package cc.devbangs.morpho.ads

import android.app.Activity
import android.content.Context
import android.util.Log
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
        Log.d("MorphoAds", "preload check: adsEnabled=${AdState.adsEnabled()} consent=${ConsentManager.canRequestAds} adExists=${ad != null} loading=$loading")
        if (!AdState.adsEnabled() || !ConsentManager.canRequestAds || ad != null || loading) return
        Log.d("MorphoAds", "preload: requesting interstitial...")
        loading = true
        InterstitialAd.load(
            ctx, AdState.INTERSTITIAL_UNIT, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) { ad = loaded; loading = false; Log.d("MorphoAds", "Interstitial LOADED (ready)") }
                override fun onAdFailedToLoad(error: LoadAdError) { ad = null; loading = false; Log.e("MorphoAds", "Interstitial FAILED: code=${error.code} ${error.message}") }
            }
        )
    }

    /**
     * Show the interstitial if one is ready and policy allows.
     * Reloads a fresh one afterward. Safe to call anytime.
     */
    fun maybeShow(activity: Activity, onDone: () -> Unit = {}) {
        val current = ad
        Log.d("MorphoAds", "maybeShow: adReady=${current != null} adsEnabled=${AdState.adsEnabled()}")
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
