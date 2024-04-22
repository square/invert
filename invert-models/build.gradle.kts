import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    js {
        browser()
    }
    jvm {
        withSourcesJar()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}

mavenPublishing {
    pomFromGradleProperties()
    signAllPublications()
    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
}
