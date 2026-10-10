package pl.dev404.czytanie.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** detekt + ktlint, both wired into `check`. */
internal fun Project.configureQuality() {
    pluginManager.apply(libs.pluginId("detekt"))
    pluginManager.apply(libs.pluginId("ktlint"))

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        parallel = true
    }
    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
        reports {
            html.required.set(false)
            xml.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
            txt.required.set(true)
        }
    }
    // detekt 1.23.x embeds its own Kotlin compiler; keep the Kotlin artifacts on its classpath
    // at the version it was built with instead of the project's Kotlin version.
    val detektKotlin = libs.version("detektKotlin")
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion(detektKotlin)
        }
    }

    extensions.configure<KtlintExtension> {
        version.set(libs.version("ktlint"))
        android.set(true)
        filter {
            exclude { it.file.path.contains("/build/") }
        }
    }
}
