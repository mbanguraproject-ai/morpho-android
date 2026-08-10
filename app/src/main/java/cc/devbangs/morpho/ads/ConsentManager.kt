package cc.devbangs.morpho.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import androidx.compose.runtime.mutableStateOf
import android.util.Log

/**
 * Handles GDPR/CCPA consent via Google's User Messaging Platform (UMP).
 * Required before serving ads. Call gatherConsent() at app launch.
 */
object ConsentManager {
    private var consentInformation: ConsentInformation? = null
    // Observable so Compose recomposes (native card) when consent resolves.
    val canRequestAdsState = mutableStateOf(false)
    val canRequestAds: Boolean get() = canRequestAdsState.value

    /**
     * Request consent info and show the form if required.
     * onReady fires once consent is resolved (whether or not a form was shown).
     */
    fun gatherConsent(activity: Activity, onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info

        info.requestConsentInfoUpdate(
            activity, params,
            {
                // Consent info updated — load & show form if needed.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    canRequestAdsState.value = info.canRequestAds()
                    Log.d("MorphoAds", "Consent resolved (form path). canRequestAds=" + canRequestAdsState.value)
                    onReady()
                }
            },
            {
                // Failed to update — proceed without personalized ads.
                canRequestAdsState.value = info.canRequestAds()
                Log.d("MorphoAds", "Consent update FAILED. canRequestAds=" + canRequestAdsState.value)
                onReady()
            }
        )

        // If consent was already gathered in a prior session, we can request ads now.
        if (info.canRequestAds()) {
            canRequestAdsState.value = true
        }
    }

    /** Whether a privacy-options entry (to withdraw consent) should be shown in Settings. */
    fun privacyOptionsRequired(): Boolean =
        consentInformation?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Show the privacy options form (for the Settings "manage consent" entry). */
    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone() }
    }
}
