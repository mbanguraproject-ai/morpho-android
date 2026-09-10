plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // KSP is pinned to the Kotlin version, not chosen freely: 2.0.21-1.0.28 is
    // the pairing for Kotlin 2.0.21. Bumping Kotlin means bumping this in the
    // same commit or the build stops.
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
