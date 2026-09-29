plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.spiderclock.wallpaper"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spiderclock.wallpaper"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Sideload signing key so that assembleRelease produces an INSTALLABLE apk
    // (an unsigned release apk cannot be installed). Replace for Play Store use.
    signingConfigs {
        create("release") {
            storeFile = file("spiderclock-release.jks")
            storePassword = "spiderclock"
            keyAlias = "spiderclock"
            keyPassword = "spiderclock"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

// Intentionally no third-party dependencies: the wallpaper only uses the Android framework.
