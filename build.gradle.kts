// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Applies the Android application plugin.
    // Project: https://developer.android.com/build
    alias(libs.plugins.android.application) apply false

    // Applies the Android library plugin.
    // Project: https://developer.android.com/build
    alias(libs.plugins.android.library) apply false

    // Applies the Kotlin Android plugin.
    // Project: https://kotlinlang.org/docs/android-overview.html
    alias(libs.plugins.kotlin.android) apply false

    // Applies the Kotlin Compose plugin.
    // Project: https://developer.android.com/jetpack/compose
    alias(libs.plugins.kotlin.compose) apply false

    // Applies the Kotlin Serialization plugin.
    // Project: https://github.com/Kotlin/kotlinx.serialization
    alias(libs.plugins.kotlin.serialization) apply false

    // Applies the Kotlin Parcelize plugin.
    // Project: https://developer.android.com/kotlin/parcelize
    alias(libs.plugins.kotlin.parcelize) apply false

    // Applies the Kotlin KAPT (Kotlin Annotation Processing Tool) plugin.
    // Project: https://kotlinlang.org/docs/kapt.html
    alias(libs.plugins.kotlin.kapt) apply false

    // Applies the Kotlin Symbol Processing (KSP) plugin.
    // Project: https://github.com/google/ksp
    alias(libs.plugins.ksp) apply false

    // Applies the Metro plugin for dependency injection.
    // Project: https://zacsweers.github.io/metro/
    alias(libs.plugins.metro) apply false

    // Applies the Kotlinter plugin for Kotlin code formatting and linting.
    // Project: https://github.com/jeremymailen/kotlinter-gradle
    alias(libs.plugins.kotlinter) apply false
}
