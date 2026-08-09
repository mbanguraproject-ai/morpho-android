package cc.devbangs.morpho.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Handles GDPR/CCPA consent via Google's User Messaging Platform (UMP).
 * Required before serving ads. Call gatherConsent() at app launch.
 */
object ConsentManager {
    private var consentInformation: ConsentInformation? = null
    var canRequestAds = false
        private set

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
                    canRequestAds = info.canRequestAds()
                    onReady()
                }
            },
            {
                // Failed to update — proceed without personalized ads.
                canRequestAds = info.canRequestAds()
                onReady()
            }
        )

        // If consent was already gathered in a prior session, we can request ads now.
        if (info.canRequestAds()) {
            canRequestAds = true
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
