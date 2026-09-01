plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "cc.devbangs.morpho"
    compileSdk = 36

    defaultConfig {
        applicationId = "cc.devbangs.morpho"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.4"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../morpho-release.keystore")
            storePassword = "morpho2026"
            keyAlias = "morpho"
            keyPassword = "morpho2026"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.adamglin:phosphor-icon:1.0.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // On-device subject segmentation for Background Remover.
    // Unbundled: ~200KB in the APK, the model arrives via Play services.
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("dev.chrisbanes.haze:haze:1.6.10")
    implementation("dev.chrisbanes.haze:haze-materials:1.6.10")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.gms:play-services-ads:24.6.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("com.android.billingclient:billing:9.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
