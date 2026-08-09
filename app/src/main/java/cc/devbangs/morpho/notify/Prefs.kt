package cc.devbangs.morpho.notify

import android.content.Context

/** Lightweight settings store (SharedPreferences) for user toggles. */
object Prefs {
    private const val FILE = "morpho_prefs"
    private const val KEY_NOTIFS = "notifications_enabled"
    private const val KEY_DARK = "dark_mode"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun notificationsEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_NOTIFS, false)   // off by default (user opts in)
    fun setNotifications(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_NOTIFS, on).apply()

    fun darkMode(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DARK, false)      // light by default
    fun setDarkMode(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_DARK, on).apply()
}
