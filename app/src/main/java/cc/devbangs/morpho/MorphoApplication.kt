package cc.devbangs.morpho

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.google.android.gms.ads.MobileAds
import cc.devbangs.morpho.core.Motion
import cc.devbangs.morpho.ads.AdState
import cc.devbangs.morpho.ads.ConsentManager
import cc.devbangs.morpho.billing.BillingManager
import cc.devbangs.morpho.data.FileStore
import cc.devbangs.morpho.data.Stats
import cc.devbangs.morpho.workflow.WorkflowBus
import cc.devbangs.morpho.data.Workspace
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

        runCatching { Workspace.init(applicationContext) }
            .onFailure { Log.e("MorphoInit", "Workspace init failed", it) }

        runCatching { Motion.init(applicationContext) }
            .onFailure { Log.e("MorphoInit", "Motion init failed", it) }

        runCatching { Stats.init(applicationContext) }
            .onFailure { Log.e("MorphoInit", "Stats init failed", it) }

        // A Kotlin object initialises the first time anything touches it, and a
        // mutableStateOf created inside a composition snapshot cannot be read in
        // that same pass. PlanPill reading AdState.isPlus was the first touch of
        // AdState, so the state was born mid-composition and every launch threw
        // IllegalStateException: "Reading a state that was created after the
        // snapshot was taken".
        //
        // Touching each of these here forces class initialisation outside any
        // snapshot. FileStore, BillingManager and ConsentManager are the same
        // shape and would have failed the same way on whichever screen happened
        // to reach them first.
        runCatching {
            AdState.isPlus
            FileStore.loaded
            BillingManager.isReady
            ConsentManager.canRequestAdsState
            WorkflowBus.hasPending
        }.onFailure { Log.e("MorphoInit", "State warm-up failed", it) }

        // AdMob on a background thread, wrapped so SDK init can never crash the app
        Thread {
            runCatching { MobileAds.initialize(this) }
                .onFailure { Log.e("MorphoInit", "AdMob init failed", it) }
        }.start()
    }
}
