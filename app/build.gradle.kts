import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlinter)
}

android {
    namespace = "ink.trmnl.android.buddy"
    compileSdk = 36

    defaultConfig {
        applicationId = "ink.trmnl.android.buddy"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // Read key or other properties from local.properties
        val localProperties =
            project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
                Properties().apply { load(it) }
            }
        val apiKey = localProperties?.getProperty("SERVICE_API_KEY") ?: "MISSING-KEY"
        buildConfigField("String", "SERVICE_API_KEY", "\"$apiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // See `keystore/README.md` for more information
    signingConfigs {
        create("config") {
            storeFile = file("../keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("config")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Use same signing config for release builds (for development/testing)
            signingConfig = signingConfigs.getByName("config")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Disable Instantiatable lint rule because we use a custom AppComponentFactory
        // (ComposeAppComponentFactory) for dependency injection. Activities are injected
        // via constructor parameters and instantiated by our DI framework (Metro) rather
        // than the Android system's default no-arg constructor mechanism.
        disable += "Instantiatable"
    }
}

kotlin {
    // See https://kotlinlang.org/docs/gradle-compiler-options.html
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // Modules
    implementation(project(":api"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.circuit.codegen.annotations)
    implementation(libs.circuit.foundation)
    implementation(libs.circuit.overlay)
    implementation(libs.circuitx.android)
    implementation(libs.circuitx.effects)
    implementation(libs.circuitx.gestureNav)
    implementation(libs.circuitx.overlays)
    ksp(libs.circuit.codegen)

    implementation(libs.javax.inject)

    implementation(libs.androidx.work)
    implementation(libs.androidx.datastore.preferences)

    // EitherNet for type-safe API results (needed to handle ApiResult from :api module)
    implementation(libs.eithernet)

    // Coil - Image loading for Compose
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Testing
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.assertk)
    testImplementation(libs.junit)
    testImplementation(libs.circuit.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

ksp {
    // Circuit-KSP for Metro
    arg("circuit.codegen.mode", "metro")
    
    // Metro 0.4.0 feature: Enable scoped inject class hints for better performance
    // This allows child graphs to depend on parent-scoped dependencies that are unused
    // See https://zacsweers.github.io/metro/dependency-graphs/
    arg("metro.enableScopedInjectClassHints", "true")
}


metro {
    // Enable Metro debug mode for better logging and debugging support
    // See https://zacsweers.github.io/metro/debugging/
  debug.set(true)
}