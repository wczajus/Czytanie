package pl.dev404.czytanie.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

private val javaVersion = JavaVersion.VERSION_17

/** Shared Android configuration for application and library modules. */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = libs.version("compileSdk").toInt()
        defaultConfig {
            minSdk = libs.version("minSdk").toInt()
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
        lint {
            abortOnError = true
            checkDependencies = false
            // CI uploads no artifacts, so lint findings go to the job log.
            textReport = true
            textOutput = file("stdout")
            // Dependency bumps are handled by Dependabot, not by failing the build.
            disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
        }
        packaging {
            resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
        }
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions { configureCommon() }
    }
    configureUnitTests()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions { configureCommon() }
    }
    configureUnitTests()
}

private fun KotlinJvmCompilerOptions.configureCommon() {
    jvmTarget.set(JvmTarget.JVM_17)
    allWarningsAsErrors.set(true)
    freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}

/** JUnit 5 + Kotest assertions + coroutines-test for every module's JVM unit tests. */
private fun Project.configureUnitTests() {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
    dependencies {
        add("testImplementation", platform(libs.library("junit-bom")))
        add("testImplementation", libs.library("junit-jupiter"))
        add("testImplementation", libs.library("junit-jupiter-params"))
        add("testImplementation", libs.library("kotest-assertions-core"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
        add("testRuntimeOnly", libs.library("junit-platform-launcher"))
    }
}
