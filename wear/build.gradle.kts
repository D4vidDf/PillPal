plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.d4viddf.medicationreminder.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.d4viddf.medicationreminder.wear"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview) // General Wear OS tooling
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Tiles
    implementation(libs.androidx.tiles)
    implementation(libs.androidx.tiles.material) // For Material Design in Tiles

    // Horologist for Compose previews and other utilities if needed (already present)
    implementation(libs.horologist.compose.tools)
    // Horologist for Tiles if specific components are used (already present, though androidx.wear.tiles.material is primary)
    implementation(libs.horologist.tiles)


    // Complications (already present, though not explicitly in this plan step)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.tiles.tooling.preview)


    // Coroutines support for Play Services Tasks (for .await())
    implementation(libs.kotlinx.coroutines.play.services) // Added for .await() with Play Services Tasks


    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.tiles.tooling)

    implementation("androidx.wear.protolayout:protolayout:1.3.0")
    implementation("androidx.wear.protolayout:protolayout-material:1.3.0")
}