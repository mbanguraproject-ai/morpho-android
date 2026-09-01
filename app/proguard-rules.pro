# Morpho R8 configuration.
#
# The previous version kept the entire dependency graph with -keep class X { *; },
# which left obfuscation at 18% and drew a Play warning. Most of those keeps were
# unnecessary: Play Services, ML Kit, Billing and UMP all ship their own consumer
# rules inside their AARs, and a library that is only ever called directly from
# Kotlin does not need keeping at all - R8 keeps what is reachable and renames it.
#
# What remains below is limited to code that is genuinely reached by reflection,
# where renaming would break it at runtime.

# ---- Compose ----------------------------------------------------------------
# Left in place deliberately: it predates this cleanup and removing it is a
# separate change with its own testing.
-keepclassmembers class ** { @androidx.compose.runtime.Composable *; }

# ---- PDFBox-Android ---------------------------------------------------------
# Genuinely reflective: loads fonts and encoding tables by class and resource
# name, and resolves crypto providers at runtime. This one has to stay.
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**
-keep class org.apache.** { *; }
-dontwarn org.apache.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.**

# ---- Kotlin -----------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Enum values() and valueOf() can be reached reflectively by the platform.
-keepclassmembers enum cc.devbangs.morpho.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Warnings only, no keeps ------------------------------------------------
# These suppress build noise about optional dependencies. They do not prevent
# obfuscation, unlike a -keep.
-dontwarn com.google.mlkit.**
-dontwarn com.google.zxing.**
-dontwarn com.android.billingclient.**
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**
-dontwarn dev.chrisbanes.haze.**
-dontwarn org.json.**

# ---- Removed, and why -------------------------------------------------------
# com.adamglin.**            Phosphor icons are Kotlin objects referenced by name
#                            in source (PhosphorIcons.Bold.Crown). Direct calls,
#                            not reflection, and several hundred classes.
# com.google.android.gms.ads.**, com.google.android.gms.internal.ads.**
#                            AdMob ships its own consumer rules. Thousands of
#                            classes: the single biggest cause of the 18%.
# com.google.mlkit.**, ...mlkit_**   ML Kit ships its own consumer rules.
# com.android.billingclient.**       Billing ships its own consumer rules.
# com.google.android.ump.**          UMP ships its own consumer rules.
# com.google.zxing.**        Pure Java, called directly from our code.
# dev.chrisbanes.haze.**     Compose library, called directly.
# androidx.core.splashscreen.**      AndroidX ships its own rules.
# cc.devbangs.morpho.data.** Our own models. Nothing serialises them; the store
#                            writes strings we build by hand.
# cc.devbangs.morpho.billing.BillingManager
#                            Its fields are read from Kotlin, not reflectively.
