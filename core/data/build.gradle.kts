plugins {
    alias(libs.plugins.czytanie.android.library)
    alias(libs.plugins.czytanie.android.hilt)
    alias(libs.plugins.czytanie.android.room)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.domain)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // Room tests run on the JVM with Robolectric, which needs JUnit 4 (through the vintage engine).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}
