import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
        maven { url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content") } // SNAPSHOT Versions for statik
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven { url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content") } // SNAPSHOT Versions for statik
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

if (hasProperty("buildScan")) {
    extensions.findByName("buildScan")?.withGroovyBuilder {
        setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
        setProperty("termsOfServiceAgree", "yes")
    }
}

subprojects
    .forEach {
        it.afterEvaluate {
            val hasPublishPlugin = it.plugins.hasPlugin("com.vanniktech.maven.publish.base")
            if (hasPublishPlugin) {
                val extension = it.extensions.getByType(MavenPublishBaseExtension::class.java)
                extension.apply {
                    // publishToMavenCentral(SonatypeHost.DEFAULT)
                    // or when publishing to https://s01.oss.sonatype.org
                    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
                    // or when publishing to https://central.sonatype.com/
                    signAllPublications()
                }
            }
        }
    }