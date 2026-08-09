package cc.devbangs.morpho

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MorphoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
