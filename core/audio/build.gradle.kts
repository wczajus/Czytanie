plugins {
    alias(libs.plugins.czytanie.android.library)
    alias(libs.plugins.czytanie.android.hilt)
}

dependencies {
    api(projects.core.domain)
}
