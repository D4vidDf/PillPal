plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.d4viddf.medicationreminder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.d4viddf.medicationreminder"
        minSdk = 26
        targetSdk = 36
        versionCode = 24
        versionName = "0.5.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.graphics.shapes) // Added graphics-shapes
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)

    implementation(libs.coil.compose)


    // OkHttp
    implementation(libs.okhttp)
    implementation (libs.kotlinx.coroutines.core)

    // Snapper
    implementation(libs.snapper)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.material)
    implementation(libs.json)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.material)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material3.window.size.class1.android)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.foundation)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive)
    implementation(libs.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.google.guava)
    implementation(libs.concurrent.futures)
    implementation(libs.kotlinx.coroutines.guava)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)      // KSP for Hilt (covers hilt-android-compiler)
    implementation(libs.hilt.work) // Hilt WorkManager Integration (no separate KSP needed for this lib usually)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Hilt Navigation Compose
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation Compose
    implementation(libs.navigation.compose)


    // Accompanist Pager
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Unit Tests
    testImplementation(libs.junit)

    // Android Instrumented Tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging Tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android) // Or your current Hilt version

// Hilt Extension for WorkManager
    implementation(libs.hilt.work) // Or latest stable version

    implementation(libs.charty)

    // Wear OS
    implementation(libs.play.services.wearable)

    // Explicitly add Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines support for Play Services Tasks (for .await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // Or use libs.kotlinx.coroutines.play.services if defined
}
