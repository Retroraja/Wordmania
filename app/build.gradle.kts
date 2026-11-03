plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.retroraja.wordnoid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.retroraja.wordnoid"
        minSdk = 26
        targetSdk = 36
        // Play needs a bigger number for every upload: the CI run number (+100 so it is above all old test builds).
        versionCode = 100 + (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
    }

    signingConfigs {
        // A fixed test key kept in the repo, so each new APK installs over the old one and keeps progress.
        // It is only for testing. A private release key will be made before publishing on the Play Store.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // The private Play Store upload key. CI writes it from the UPLOAD_KEY secret (never kept in the repo).
        val upload = rootProject.file("upload.jks")
        if (upload.exists()) create("release") {
            storeFile = upload
            storePassword = System.getenv("UPLOAD_KEY_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
        }
    }

    buildTypes {
        // Ads: the test APK (debug) always shows Google's TEST ads, so tapping them is safe.
        // The Play Store bundle (release) uses the real AdMob ids.
        debug {
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("boolean", "REAL_ADS", "false")
        }
        release {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3867331045861180~6000389333"
            buildConfigField("boolean", "REAL_ADS", "true")
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    androidResources { noCompress += "txt" }
}

dependencies {
    implementation(project(":core"))
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Ads (test ids until the AdMob account exists) and the consent form.
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    // "Rate us" in-app review.
    implementation("com.google.android.play:review-ktx:2.0.2")
    // Friendly daily reminders.
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
