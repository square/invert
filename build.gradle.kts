import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }

//    dependencies {
//        val invertVersion = "0.0.1-dev-SNAPSHOT"
//        classpath("com.squareup.invert:invert-plugin:$invertVersion")
//        classpath("com.squareup.invert:collectors-anvil-dagger-jvm:$invertVersion")
//    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
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
    .filter { it.plugins.hasPlugin("com.vanniktech.maven.publish.base") }
    .forEach {
        val extension = it.extensions.getByType(MavenPublishBaseExtension::class.java)
        extension.apply {
            // publishToMavenCentral(SonatypeHost.DEFAULT)
            // or when publishing to https://s01.oss.sonatype.org
            publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
            // or when publishing to https://central.sonatype.com/
            signAllPublications()
        }
    }