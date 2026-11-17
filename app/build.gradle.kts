import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val nasaApiKey: String = (
    localProps.getProperty("NASA_API_KEY")
        ?: System.getenv("NASA_API_KEY")
        ?: "DEMO_KEY"
).trim()

android {
    namespace = "com.astroframe.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.astroframe.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.3.3"

        buildConfigField("String", "NASA_API_KEY", "\"$nasaApiKey\"")
        buildConfigField("String", "POSTHOG_API_KEY", "\"phc_xPxgwuPe2FD7dV4mJYSBeVbzbicvF3KMRxb2z6BEUZCZ\"")
        buildConfigField("String", "POSTHOG_HOST", "\"https://us.i.posthog.com\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.leanback:leanback:1.0.0")

    // WorkManager for background scheduled updates
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // OkHttp for reliable network calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // PostHog Analytics for live active user & install tracking
    implementation("com.posthog:posthog-android:3.+")
}
