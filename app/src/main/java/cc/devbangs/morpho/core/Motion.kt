package cc.devbangs.morpho.core

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Blueprint sections 19-23 and Appendix A - one source of truth for motion.
 *
 * Durations were previously hand-picked per screen: nav at 320, the Home
 * reveal at 420, onboarding tiles at 520, a crossfade at 260, and the bottom
 * bar on whatever Compose defaults to. Several sat outside every band the
 * appendix defines.
 *
 * Values are the midpoint of each band in Appendix A. Section 19 is explicit
 * that these are starting targets to validate on real devices and tune
 * globally rather than per screen - which is the point of them living here.
 */
object Motion {

    /** Tactile feedback on press and release. Appendix A: 80-120ms. */
    const val PRESS = 100

    /** Selection, toggles, small state changes. Appendix A: 120-160ms. */
    const val SMALL = 140

    /** Cards and controls. Appendix A: 180-220ms. */
    const val COMPONENT = 200

    /** Tab indicator and tab content. Appendix A: 180-240ms. */
    const val TAB = 220

    /** Menus and popovers. Appendix A: 180-240ms. */
    const val MENU = 220

    /** Bottom sheets. Appendix A: 240-320ms. */
    const val SHEET = 280

    /** Navigation between screens. Appendix A: 250-350ms. */
    const val PAGE = 320

    /** Large modal surfaces. Appendix A: 280-360ms. */
    const val MODAL = 320

    /** Completion feedback. Appendix A: 300-500ms. */
    const val SUCCESS = 400

    /** Section 20 and 22: an exit is generally slightly faster than its entrance. */
    const val EXIT = 220

    /** Per-item delay in a staggered reveal, and the item after which it stops growing. */
    const val STAGGER = 40
    const val STAGGER_CAP = 8

    /** Section 20: enter responds quickly then settles. */
    val Enter: Easing = EaseOutCubic

    /** Section 20: exits lead out rather than easing in. */
    val Exit: Easing = EaseInCubic

    /** Section 20: large movement eases in and out. */
    val Large: Easing = FastOutSlowInEasing

    private var scale = 1f

    /**
     * Read the system animator scale once at startup.
     *
     * Section 39 asks for reduced-motion support. Android already exposes the
     * user's choice here, including 0 when they turn animations off entirely,
     * so honouring it covers the requirement without inventing a separate
     * in-app setting the user would have to find.
     */
    fun init(ctx: Context) {
        scale = try {
            Settings.Global.getFloat(
                ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            )
        } catch (e: Exception) {
            1f
        }
    }

    /** A duration in ms, scaled by the user's system animation preference. */
    fun d(base: Int): Int = (base * scale).toInt().coerceAtLeast(0)

    /** A stagger delay for item [index], capped so long lists do not crawl. */
    fun stagger(index: Int): Int = d(STAGGER * index.coerceAtMost(STAGGER_CAP))
}
