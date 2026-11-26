plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// The AdMob publisher number (16 digits). The ADMOB_PUB_ID secret can override it. The build checks it:
// written here it was once blanked out by a filter, and the Play build crashed on start (1.4.81 rejected).
val admobPub: String = Regex("\\d{16}").find(System.getenv("ADMOB_PUB_ID") ?: "")?.value ?: "2669470259590353" // AdMob → Payments → AdSense pub id (same as the AdMob publisher id)
val buildingRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
if (buildingRelease && !Regex("\\d{16}").matches(admobPub)) {
    throw GradleException("ADMOB_PUB_ID secret is missing or not 16 digits: the release build would crash on start.")
}

android {
    namespace = "com.retroraja.wordnoid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.retroraja.wordnoid"
        minSdk = 26
        targetSdk = 36
        // Play needs a bigger number for every upload: the CI run number (+100 so it is above all old test builds).
        versionCode = 182
        versionName = "1.4.82" // 1.4.82: fixes the start-up crash of 1.4.81
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
            buildConfigField("String", "ADMOB_PUB", "\"\"")
        }
        release {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-$admobPub~2192058149"
            buildConfigField("boolean", "REAL_ADS", "true")
            buildConfigField("String", "ADMOB_PUB", "\"$admobPub\"")
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
    // Ads (real ids in the Play bundle, test ids in the test APK) and the consent form.
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    // The ads/consent libraries pull in the old androidx.fragment 1.1.0 (Play Console warning): use a current one.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    // "Rate us" in-app review.
    implementation("com.google.android.play:review-ktx:2.0.2")
    // In-game "Update available" (Google Play in-app updates, Update 1.3).
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    // Firebase Analytics + Crashlytics (first live release).
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    // Friendly daily reminders.
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
