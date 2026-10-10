plugins {
    alias(libs.plugins.czytanie.android.library)
    alias(libs.plugins.czytanie.android.hilt)
    alias(libs.plugins.czytanie.android.room)
}

dependencies {
    api(projects.core.domain)
    implementation(libs.androidx.datastore.preferences)
}
