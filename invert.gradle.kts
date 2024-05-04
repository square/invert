/**
 * Invert Gradle init-script which applies the plugin to an existing Gradle project.
 *
 * Run it with the following:
 * ./gradlew --init-script invert.gradle.kts :invert
 */
settingsEvaluated {
    rootProject {
        buildscript {
            repositories {
                mavenCentral()
                gradlePluginPortal()
                google()
                mavenLocal()
                maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // SNAPSHOT Versions
            }

            dependencies {
                val invertVersion = "0.0.1-dev-SNAPSHOT"
                classpath("com.squareup.invert:invert-plugin:$invertVersion")
                classpath("com.squareup.invert:collectors-anvil-dagger:$invertVersion")
            }
        }
        allprojects {
            repositories {
                mavenCentral()
                google()
                gradlePluginPortal()
            }
        }

        afterEvaluate {
            plugins.apply("com.squareup.invert")
        }
    }
}
