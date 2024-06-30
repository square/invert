plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    kotlin("plugin.serialization")
//    alias(libs.plugins.vanniktech.maven.publish)
}

java {
    withSourcesJar()
}

dependencies {
    compileOnly(gradleApi())

    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(project(":invert-models"))
    implementation(project(":invert-gradle-plugin"))
    compileOnly("com.rickbusarow.statik:statik-kotlin-psi:0.1.0-SNAPSHOT")

    testImplementation("com.rickbusarow.statik:statik-kotlin-psi:0.1.0-SNAPSHOT")
    testImplementation(libs.kotlin.test)
}