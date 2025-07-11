plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) // Added KSP plugin
}

android {
    namespace = "com.d4viddf.medicationreminder.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.d4viddf.medicationreminder"
        minSdk = 30
        targetSdk = 36
        versionCode = 26
        versionName = "0.0.2_wear"

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
    // implementation(libs.androidx.compose.material) // Replaced by M3
    // implementation(libs.androidx.compose.foundation) // Replaced by Wear specific foundation
    implementation(libs.androidx.wear.compose.foundation) // M3 Foundation for Wear
    implementation(libs.androidx.tiles.tooling.preview) // General Wear OS tooling
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

    // Gson for JSON deserialization
    implementation("com.google.code.gson:gson:2.10.1")

    // ViewModel Compose for viewModel() composable
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    implementation(libs.androidx.media3.common.ktx) // Or use libs.androidx.lifecycle.viewmodel.compose


    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.tiles.tooling)

    implementation(libs.androidx.wear.protolayout.material)
    implementation(libs.androidx.wear.protolayout.material3)
    implementation("androidx.wear:protolayout-adapter:1.0.0") // Added adapter

    implementation(libs.androidx.tiles)
    implementation(libs.androidx.tiles.material)
    // Room Database for Wear
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
}