import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import pl.dev404.czytanie.buildlogic.configureKotlinAndroid
import pl.dev404.czytanie.buildlogic.configureQuality
import pl.dev404.czytanie.buildlogic.libs
import pl.dev404.czytanie.buildlogic.pluginId

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-library"))
        pluginManager.apply(libs.pluginId("kotlin-android"))
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            // Namespace derived from the module path, e.g. :feature:reading -> pl.dev404.czytanie.feature.reading
            namespace = "pl.dev404.czytanie" + path.replace(':', '.')
        }
        configureQuality()
    }
}
