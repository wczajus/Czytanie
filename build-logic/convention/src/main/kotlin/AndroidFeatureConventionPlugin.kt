import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import pl.dev404.czytanie.buildlogic.library
import pl.dev404.czytanie.buildlogic.libs
import pl.dev404.czytanie.buildlogic.pluginId

/**
 * A feature module: Android library + Compose + Hilt + type-safe navigation.
 * Features depend on core modules only, never on each other.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("czytanie-android-library"))
        pluginManager.apply(libs.pluginId("czytanie-android-compose"))
        pluginManager.apply(libs.pluginId("czytanie-android-hilt"))
        pluginManager.apply(libs.pluginId("kotlin-serialization"))
        dependencies {
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:ui"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-navigation-compose"))
            add("implementation", libs.library("androidx-hilt-navigation-compose"))
            add("implementation", libs.library("kotlinx-serialization-json"))
        }
    }
}
