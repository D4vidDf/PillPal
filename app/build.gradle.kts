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
        versionCode = 6
        versionName = "0.0.4"

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
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

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
}
