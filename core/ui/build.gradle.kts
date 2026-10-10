plugins {
    alias(libs.plugins.czytanie.android.library)
    alias(libs.plugins.czytanie.android.compose)
}

dependencies {
    api(projects.core.domain)

    // Compose UI tests of shared components run on the JVM with Robolectric (JUnit 4).
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// The Compose test activity is registered only in debug, so the JVM tests run on debug alone.
tasks.matching { it.name == "testReleaseUnitTest" }.configureEach { enabled = false }
