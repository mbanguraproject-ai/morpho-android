# ---- Compose ----
-keepclassmembers class ** { @androidx.compose.runtime.Composable *; }

# ---- PDFBox-Android (tom_roush) — uses reflection + resources ----
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**
-keep class org.apache.** { *; }
-dontwarn org.apache.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.**

# ---- ML Kit text recognition ----
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# ---- ZXing ----
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ---- Phosphor icons (vector properties accessed lazily) ----
-keep class com.adamglin.** { *; }

# ---- Kotlin coroutines ----
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ---- Keep our data classes / models (used by UI reflection-free but safe) ----
-keep class cc.devbangs.morpho.data.** { *; }

# ---- General Android ----
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ---- Haze (frosted blur) ----
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# ---- AndroidX SplashScreen ----
-keep class androidx.core.splashscreen.** { *; }

# ---- Google Play Billing ----
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**
-keep class com.android.vending.billing.** { *; }

# ---- Google Mobile Ads (AdMob) ----
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.gms.internal.ads.** { *; }

# ---- UMP consent SDK ----
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**
