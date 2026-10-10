plugins {
    alias(libs.plugins.czytanie.android.application)
    alias(libs.plugins.czytanie.android.compose)
    alias(libs.plugins.czytanie.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pl.dev404.czytanie"

    defaultConfig {
        applicationId = "pl.dev404.czytanie"
        versionCode = providers.gradleProperty("czytanie.versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("czytanie.versionName").orNull ?: "0.1.0"
    }

    signingConfigs {
        // Release signing comes from the environment (CI secrets). Without it, release builds
        // fall back to the debug key so they can still be installed for testing.
        val keystorePath = providers.environmentVariable("CZYTANIE_KEYSTORE_PATH").orNull
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("CZYTANIE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("CZYTANIE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("CZYTANIE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.audio)
    implementation(projects.core.ui)
    implementation(projects.feature.levels)
    implementation(projects.feature.reading)
    implementation(projects.feature.parent)
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
