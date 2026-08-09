package cc.devbangs.morpho

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.google.android.gms.ads.MobileAds
import cc.devbangs.morpho.notify.Notifier

class MorphoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        Notifier.ensureChannel(applicationContext)
        // Initialize AdMob on a background thread (SDK recommendation)
        Thread { MobileAds.initialize(this) }.start()
    }
}
