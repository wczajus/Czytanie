plugins {
    alias(libs.plugins.czytanie.android.feature)
    alias(libs.plugins.roborazzi)
}

dependencies {
    // Screenshot (Roborazzi) and Compose UI flow tests run on the JVM with Robolectric (JUnit 4).
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Reference screenshots are part of `check` (CLAUDE.md). Record them deliberately with
// `./gradlew recordRoborazziDebug` and explain every difference before committing new ones.
tasks.named("check") { dependsOn("verifyRoborazziDebug") }

// The Compose test activity is registered only in debug, so the JVM tests run on debug alone.
tasks.matching { it.name == "testReleaseUnitTest" }.configureEach { enabled = false }
