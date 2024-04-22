import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.MavenPublishBaseExtension

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }

    configurations.all {
        val conf = this
        conf.resolutionStrategy.eachDependency {
            val isWasm = conf.name.contains("wasm", true)
            val isJs = conf.name.contains("js", true)
            val isComposeGroup = requested.module.group.startsWith("org.jetbrains.compose")
            val isComposeCompiler = requested.module.group.startsWith("org.jetbrains.compose.compiler")
            if (isComposeGroup && !isComposeCompiler && !isWasm && !isJs) {
                val composeVersion = project.property("compose.version") as String
                useVersion(composeVersion)
            }
            if (requested.module.name.startsWith("kotlin-stdlib")) {
                val kotlinVersion = project.property("kotlin.version") as String
                useVersion(kotlinVersion)
            }
        }
    }
}

plugins {
    kotlin("multiplatform") apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    kotlin("plugin.serialization") version "1.9.0" apply false
    id("org.jetbrains.compose") apply false
    id("com.autonomousapps.dependency-analysis") version "1.22.0"
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