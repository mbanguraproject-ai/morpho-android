package cc.devbangs.morpho.ads

import android.content.Context
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Loads a native ad and renders it as a Morpho-styled "You might like" card,
 * with the required "Ad" label. Shows nothing for Plus users or if no ad loads.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    if (!AdState.showNativeCard() || !ConsentManager.canRequestAds) return
    val ctx = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val loader = AdLoader.Builder(ctx, AdState.TEST_NATIVE)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) { /* silent — show nothing */ }
            })
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy() }
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = modifier,
            factory = { c -> buildNativeView(c) },
            update = { view -> bindNativeAd(view as NativeAdView, ad) }
        )
    }
}

/** Programmatic Morpho-styled native ad layout (no XML needed). */
private fun buildNativeView(ctx: Context): NativeAdView {
    val density = ctx.resources.displayMetrics.density
    fun dp(v: Int) = (v * density).toInt()

    val adView = NativeAdView(ctx)
    val root = android.widget.LinearLayout(ctx).apply {
        orientation = android.widget.LinearLayout.HORIZONTAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        gravity = android.view.Gravity.CENTER_VERTICAL
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(0xFFF4F5F7.toInt())
            setStroke(dp(1), 0xFFE7E9EE.toInt())
        }
    }

    val icon = ImageView(ctx).apply {
        id = 1001
        layoutParams = android.widget.LinearLayout.LayoutParams(dp(48), dp(48)).apply {
            marginEnd = dp(12)
        }
    }
    val textCol = android.widget.LinearLayout(ctx).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        layoutParams = android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    // "Ad" label + "You might like" eyebrow row
    val labelRow = android.widget.LinearLayout(ctx).apply {
        orientation = android.widget.LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
    }
    val adBadge = TextView(ctx).apply {
        text = "Ad"
        setTextColor(0xFFFFFFFF.toInt())
        textSize = 9f
        setPadding(dp(6), dp(1), dp(6), dp(1))
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(4).toFloat(); setColor(0xFF9AA3B2.toInt())
        }
    }
    val eyebrow = TextView(ctx).apply {
        text = "  You might like"; setTextColor(0xFF9AA3B2.toInt()); textSize = 10f
    }
    labelRow.addView(adBadge); labelRow.addView(eyebrow)

    val headline = TextView(ctx).apply {
        id = 1002; setTextColor(0xFF0B0D12.toInt()); textSize = 15f
        maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
    }
    val body = TextView(ctx).apply {
        id = 1003; setTextColor(0xFF5A6472.toInt()); textSize = 12f
        maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
    }
    textCol.addView(labelRow); textCol.addView(headline); textCol.addView(body)

    val cta = Button(ctx).apply {
        id = 1004; textSize = 12f; isAllCaps = false
        setTextColor(0xFFFFFFFF.toInt())
        setPadding(dp(14), dp(6), dp(14), dp(6))
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(20).toFloat(); setColor(0xFF1A46E5.toInt())
        }
    }

    root.addView(icon); root.addView(textCol); root.addView(cta)
    adView.addView(root)
    adView.iconView = icon
    adView.headlineView = headline
    adView.bodyView = body
    adView.callToActionView = cta
    return adView
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as? TextView)?.text = ad.headline
    (view.bodyView as? TextView)?.text = ad.body ?: "Sponsored"
    (view.callToActionView as? Button)?.text = ad.callToAction ?: "Open"
    val iconView = view.iconView as? ImageView
    if (ad.icon != null) { iconView?.setImageDrawable(ad.icon?.drawable) }
    else { iconView?.visibility = android.view.View.GONE }
    view.setNativeAd(ad)
}
