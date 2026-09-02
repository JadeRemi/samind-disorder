plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Not part of the product: a stand-in "other app" for behavior tests.
android {
    namespace = "com.samind.testfeed"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.samind.testfeed"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
