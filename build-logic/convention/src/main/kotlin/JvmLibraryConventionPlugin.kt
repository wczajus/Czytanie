import org.gradle.api.Plugin
import org.gradle.api.Project
import pl.dev404.czytanie.buildlogic.configureKotlinJvm
import pl.dev404.czytanie.buildlogic.configureQuality
import pl.dev404.czytanie.buildlogic.libs
import pl.dev404.czytanie.buildlogic.pluginId

/** Pure Kotlin/JVM module. Used by :core:domain, which must never see android.* or androidx.*. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-jvm"))
        configureKotlinJvm()
        configureQuality()
    }
}
