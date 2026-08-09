package cc.devbangs.morpho.ads

import androidx.compose.runtime.mutableStateOf

/**
 * Central ad policy for Morpho.
 * - Plus subscribers see NO ads anywhere.
 * - Interstitials fire only every Nth tool completion (gentle).
 * - Native "you might like" card shows at the bottom of the tools list.
 */
object AdState {
    // Test IDs — swap real ones at launch. These are Google's official test units.
    const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // Plus gating — set true when a Plus subscription is active (wired to billing later).
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
        if (!adsEnabled()) return false
        if (!toolWasUsed) return false   // only count real usage, not a glance
        toolWasUsed = false
        completionCount++
        if (completionCount % INTERSTITIAL_EVERY != 0) return false
        val now = System.currentTimeMillis()
        if (now - lastInterstitialAt < MIN_GAP_MS) return false
        lastInterstitialAt = now
        return true
    }

    /** Should the native "you might like" card render in the tools list? */
    fun showNativeCard(): Boolean = adsEnabled()
}
