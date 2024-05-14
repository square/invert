import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.vanniktech.maven.publish)
}


kotlin {
    jvm {
        withSourcesJar()
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.compiler.embeddable)
                implementation(project(":invert-models"))
                implementation(project(":invert-gradle-plugin"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
