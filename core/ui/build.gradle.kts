plugins {
    alias(libs.plugins.czytanie.android.library)
    alias(libs.plugins.czytanie.android.compose)
}

dependencies {
    api(projects.core.domain)
}
