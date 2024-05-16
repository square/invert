plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    kotlin("plugin.serialization")
    alias(libs.plugins.vanniktech.maven.publish)
}

java {
    withSourcesJar()
}

dependencies {
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(project(":invert-models"))
    implementation(project(":invert-gradle-plugin"))

    testImplementation(libs.kotlin.test)
}