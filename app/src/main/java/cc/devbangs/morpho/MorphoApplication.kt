package cc.devbangs.morpho

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.google.android.gms.ads.MobileAds
import cc.devbangs.morpho.notify.Notifier

class MorphoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Global safety net: log any uncaught exception (Play Console captures these)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MorphoCrash", "Uncaught in ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }

        // Each init guarded so one failing subsystem can't take down the app
        runCatching { PDFBoxResourceLoader.init(applicationContext) }
            .onFailure { Log.e("MorphoInit", "PDFBox init failed", it) }

        runCatching { Notifier.ensureChannel(applicationContext) }
            .onFailure { Log.e("MorphoInit", "Notifier channel failed", it) }

        // AdMob on a background thread, wrapped so SDK init can never crash the app
        Thread {
            runCatching { MobileAds.initialize(this) }
                .onFailure { Log.e("MorphoInit", "AdMob init failed", it) }
        }.start()
    }
}
