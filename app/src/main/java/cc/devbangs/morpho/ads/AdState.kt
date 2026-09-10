package cc.devbangs.morpho.ads

import androidx.compose.runtime.mutableStateOf
import android.util.Log

/**
 * Central ad policy for Morpho.
 * - Plus subscribers see NO ads anywhere.
 * - Interstitials fire only every Nth tool completion (gentle).
 * - Native "you might like" card shows at the bottom of the tools list.
 */
object AdState {
    // Live units on the DEVBANGS publisher account - NOT test units. Google's
    // test interstitial is ca-app-pub-3940256099942544/...; these are real and
    // are what serves revenue. Do not "swap them for the real ones".
    const val INTERSTITIAL_UNIT = "ca-app-pub-9121922395304175/6203443667"
    const val NATIVE_UNIT = "ca-app-pub-9121922395304175/8812315198"
    const val BANNER_UNIT = "ca-app-pub-9121922395304175/8853953856"

    // Plus gating. BillingManager sets this from the active subscription, so
    // a paying subscriber sees no ads of any kind.
    val isPlus = mutableStateOf(false)

    // Interstitial cadence: fire on every 3rd completion.
    private const val INTERSTITIAL_EVERY = 3
    private var completionCount = 0

    // Was the current tool actually used (produced output)? Reset per tool open.
    private var toolWasUsed = false
    fun markUsed() { toolWasUsed = true }
    fun resetUsed() { toolWasUsed = false }

    // Frequency cap: never show two interstitials within this window.
    private const val MIN_GAP_MS = 90_000L  // 90s
    private var lastInterstitialAt = 0L

    /** Should ads show at all? False for Plus users. */
    fun adsEnabled(): Boolean = !isPlus.value

    /**
     * Call when a tool finishes producing output.
     * Returns true if an interstitial should be shown now.
     */
    fun onToolCompleted(): Boolean {
        if (!adsEnabled()) { Log.d("MorphoAds", "onToolCompleted: ads disabled (Plus)"); return false }
        if (!toolWasUsed) { Log.d("MorphoAds", "onToolCompleted: tool NOT used, skipping"); return false }
        toolWasUsed = false
        completionCount++
        Log.d("MorphoAds", "onToolCompleted: count=$completionCount (fires every $INTERSTITIAL_EVERY)")
        if (completionCount % INTERSTITIAL_EVERY != 0) { Log.d("MorphoAds", "not 3rd completion yet"); return false }
        val now = System.currentTimeMillis()
        if (now - lastInterstitialAt < MIN_GAP_MS) { Log.d("MorphoAds", "within 90s cap, skipping"); return false }
        lastInterstitialAt = now
        Log.d("MorphoAds", "INTERSTITIAL should show now")
        return true
    }

    /** Should the native "you might like" card render in the tools list? */
    fun showNativeCard(): Boolean = adsEnabled()
}
