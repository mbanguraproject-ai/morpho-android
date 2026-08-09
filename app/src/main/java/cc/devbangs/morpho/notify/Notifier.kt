package cc.devbangs.morpho.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Fires "your file is ready" notifications when a tool finishes.
 * Respects the user's Notifications setting + runtime permission.
 */
object Notifier {
    private const val CHANNEL_ID = "morpho_completion"
    private const val CHANNEL_NAME = "Task complete"
    private var nextId = 1000

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Tells you when a Morpho tool finishes." }
            val mgr = ctx.getSystemService(NotificationManager::class.java)
            mgr?.createNotificationChannel(channel)
        }
    }

    fun hasPermission(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Fire a completion notification if the user enabled them + granted permission. */
    fun notifyDone(ctx: Context, title: String, text: String) {
        if (!Prefs.notificationsEnabled(ctx)) return
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(cc.devbangs.morpho.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(nextId++, notif) }
    }
}
