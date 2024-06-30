import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
        maven {
            url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content")
        } // SNAPSHOT Versions for statik
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven {
            url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content")
        } // SNAPSHOT Versions for statik
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
                it.extensions.getByType(MavenPublishBaseExtension::class.java).apply {
                    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
                    signAllPublications()
                    pom {
                        url.set("https://www.github.com/square/invert")
                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        scm {
                            url.set("https://www.github.com/square/invert")
                            connection.set("scm:git:git://github.com/square/invert.git")
                            developerConnection.set("scm:git:ssh://git@github.com/square/invert.git")
                        }
                        developers {
                            developer {
                                name.set("Block, Inc.")
                                url.set("https://github.com/square")
                            }
                        }
                    }
                    if (plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
                        configure(
                            KotlinJvm(
                                javadocJar = JavadocJar.Dokka("dokkaHtml"),
                                sourcesJar = true,
                            )
                        )
                    }
                    if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                        configure(
                            KotlinMultiplatform(
                                javadocJar = JavadocJar.Dokka("dokkaHtml"),
                                sourcesJar = true,
                            )
                        )
                    }
//                    configure(
//                        GradlePlugin(
//                            javadocJar = JavadocJar.None(),
//                            sourcesJar = true,
//                        )
//                    )
                }
            }
        }
    }
