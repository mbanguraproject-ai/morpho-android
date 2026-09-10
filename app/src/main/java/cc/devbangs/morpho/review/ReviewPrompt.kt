package cc.devbangs.morpho.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks for a Play rating once, after the app has actually done something.
 *
 * Three things about this API shape the whole design.
 *
 * It reports nothing. The flow completes without saying whether the user
 * reviewed, or whether the dialog was ever shown - Play quota-limits it and
 * decides for itself. So nothing here can be conditional on the outcome, and
 * there is no thank-you to show afterwards.
 *
 * It must not be tied to a button. Play's policy is that the app decides when
 * to ask; a "Rate us" button calling this API is a policy problem, which is
 * why this is reachable only from a completed save.
 *
 * And it must not interrupt. A save has just finished and the file is written,
 * so the user is between tasks rather than in one.
 *
 * The threshold is deliberately the first success. Beyond matching what a
 * utility earns - the user came to convert a file and the file is converted -
 * the interstitial fires on every third tool completion, so asking on the
 * first can never land on top of an ad. A later threshold would need the ad
 * path to know about this one.
 */
object ReviewPrompt {

    private const val FILE = "morpho_review"
    private const val KEY_ASKED = "asked"
    private const val KEY_WINS = "wins"

    /** Successful saves before asking. One: the first thing that worked. */
    private const val THRESHOLD = 1

    /**
     * Call from the main thread after a save is known to have succeeded.
     *
     * Silent when it decides not to ask, which is most of the time - it asks
     * at most once for the life of the install.
     */
    fun onSuccessfulSave(ctx: Context) {
        try {
            val prefs = ctx.applicationContext
                .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_ASKED, false)) return

            val wins = prefs.getInt(KEY_WINS, 0) + 1
            prefs.edit().putInt(KEY_WINS, wins).apply()
            if (wins < THRESHOLD) return

            val activity = ctx.findActivity() ?: return

            // Recorded before launching, not after. The API never confirms the
            // dialog appeared, so the only honest thing to remember is that we
            // took our turn - and asking twice because we waited for a
            // confirmation that never comes would be worse than not asking.
            prefs.edit().putBoolean(KEY_ASKED, true).apply()

            val manager = ReviewManagerFactory.create(ctx.applicationContext)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    runCatching { manager.launchReviewFlow(activity, task.result) }
                        .onFailure { Log.d("MorphoReview", "flow refused: " + it.message) }
                } else {
                    Log.d("MorphoReview", "no flow: " + task.exception?.message)
                }
            }
        } catch (e: Exception) {
            // Never let asking for a rating break a save that already worked.
            Log.d("MorphoReview", "skipped: " + e.message)
        }
    }
}

/** The hosting Activity, which the review flow needs and a Context is not. */
private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
